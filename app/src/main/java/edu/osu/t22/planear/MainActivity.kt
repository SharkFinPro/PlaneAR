package edu.osu.t22.planear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.androidgamesdk.GameActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import edu.osu.t22.planear.adsb.AdsbModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import edu.osu.t22.planear.geo.GeoUtils
import edu.osu.t22.planear.geo.GeoPoint
import android.view.Surface


class MainActivity : GameActivity() {
    companion object {
        init {
            System.loadLibrary("GraphicsEngine")
        }

        private const val CAMERA_PERMISSION_CODE = 0

        @JvmStatic
        external fun nativeSetArReady(ready: Boolean)
    }

    private var arSession: Session? = null
    private var arSessionManager: ARSessionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get a new instance of the Retrofit API provider
        val api = AdsbModule.provideApi()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    try {
                        // get all nearby aircraft within 50nm
                        val nearby = async(Dispatchers.IO) {
                            api.getNearbyAircraft(44.565722, -123.278917, 50)
                        }

                        // get closest aircraft (could also just be parsed from the nearby list
                        val closest = async(Dispatchers.IO) {
                            api.getClosestAircraft(44.565722, -123.278917, 250)
                        }

                        // log results
                        val nearbyData = nearby.await()
                        Log.d("ADSB_TEST", "Got ${nearbyData.total} aircraft")
                        val closestData = closest.await()
                        Log.d("ADSB_TEST", "Closest aircraft: ${closestData.ac.firstOrNull() ?: "No aircraft found"}")
                        Log.d("ADSB_TEST", "Timing data: (now: ${nearbyData.now}, cTime: ${nearbyData.cTime}, pTime: ${nearbyData.pTime})")


                        //--------------------TESTING GEO CALCULATIONS--------------------
                        val closestAircraft = closestData.ac.firstOrNull()
                        if (closestAircraft != null) {

                            // will be replaced with arcore geolocation
                            val userLat = 44.565722
                            val userLon = -123.278917
                            val userAltM = 0.0
                            val userHeadingDeg = 90.0 //facing east

                            val acLat = closestAircraft.lat
                            val acLon =  closestAircraft.lon
                            val acAltFeet = closestAircraft.alt_baro?.toDoubleOrNull() ?: 0.0
                            val acAltM = acAltFeet * 0.3048

                            val userPoint = GeoPoint(userLat, userLon, userAltM)
                            val acPoint = GeoPoint(acLat, acLon, acAltM)

                            val dir = GeoUtils.relativeDirection(
                                user = userPoint,
                                userHeadingDeg = userHeadingDeg,
                                aircraft = acPoint
                            )

                            Log.d(
                                "ADSB_DIR",
                                "distance=${"%.0f".format(dir.distanceMeters)} m, " +
                                        "bearing=${"%.1f".format(dir.bearingToAircraft)}°" +
                                        "relative=${"%.1f".format(dir.relativeBearingDeg)}°, " +
                                        "elevation=${"%.1f".format(dir.elevationDeg)}°"
                            )

                            val enh = GeoUtils.enhVector(userPoint, acPoint)
                            Log.d(
                                "ADSB_ENH",
                                "east=${"%.1f".format(enh.east)}, " +
                                        "north=${"%.1f".format(enh.north)} m, " +
                                        "height=${"%.1f".format(enh.height)} m"
                            )
                        } else {
                            Log.d("ADSB_DIR", "No closest aircraft")
                        }



                    } catch (e: Exception) {
                        Log.e("ADSB_TEST", "API call failed", e)
                    }

                    // repeat every 5 seconds
                    delay(5_000L)
                }
            }
        }
    }
    @Suppress("MissingPermission")


    override fun onResume() {
        super.onResume()

        //check camera permissions
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        //create arcore session if not already created
        if (arSession == null) {
            tryCreateSession()
        }

        //resume ar session
        try {
            arSession?.resume()
        } catch (e: Exception) {
            Log.e("PlaneAR", "Failed to resume AR session", e)
        }

        window.decorView.post {
            arSessionManager?.updateViewport(
                window.decorView.width,
                window.decorView.height
            )
        }


        hideSystemUi()
    }

    override fun onPause() {
        super.onPause()
        try {
            arSession?.pause()
        } catch (e: Exception) {
            Log.e("PlaneAR", "Failed to pause AR session", e)
        }
    }

    // premission callbacks
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            tryCreateSession()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    //ARCORE 1.51 install / create session flow

    private fun tryCreateSession(): Boolean {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            if (!availability.isSupported) {
                Log.e("PlaneAR", "ARCore not supported")
                nativeSetArReady(false)
                return false
            }

            if (arSession == null) {
                arSession = Session(this)
                Log.i("PlaneAR", "ARCore session created")

                nativeSetArReady(true)             // <--  Notify native renderer for top green box

                val config = Config(arSession).apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    depthMode =
                        if (arSession!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                            Config.DepthMode.AUTOMATIC
                        else Config.DepthMode.DISABLED
                }
                arSession!!.configure(config)
            }

            return true

        } catch (e: Exception) {
            Log.e("PlaneAR", "Failed to create ARCore session", e)
            nativeSetArReady(false)               // failed → stay non-AR mode
            return false
        }
    }


    private fun hideSystemUi() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    fun onArUpdateFrame() {
        arSessionManager?.onUpdateFrame()
    }

    fun setCameraTexture(textureId: Int) {
        arSessionManager?.setCameraTextureName(textureId)
    }
}

class ARSessionManager(
    private val session: Session,
    private val displayRotation: () -> Int
) {

    private val anchors = mutableListOf<Anchor>()

    private var viewportWidth: Int = 1
    private var viewportHeight: Int = 1

    fun updateViewport(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
    }

    fun setCameraTextureName(tex: Int) {
        session.setCameraTextureName(tex)
    }


    fun onUpdateFrame() {
        // Keep ARCore in sync with display rotation & surface size
        session.setDisplayGeometry(displayRotation(), viewportWidth, viewportHeight)

        val frame = session.update()
        val camera = frame.camera

        if (camera.trackingState != TrackingState.TRACKING) {
            // Inform native side that tracking is limited or lost
            nativeOnTrackingStateChanged(camera.trackingState.ordinal)
            return
        }


        // Camera pose -> 4x4 matrix
        val cameraPose = camera.displayOrientedPose
        val cameraMatrix = FloatArray(16)
        cameraPose.toMatrix(cameraMatrix, 0)

        // Send camera pose to native rendering engine
        nativeUpdateCameraPose(cameraMatrix)

        // Update any anchors we’re tracking (e.g., aircraft positions in AR space)
        val iterator = anchors.iterator()
        while (iterator.hasNext()) {
            val anchor = iterator.next()

            if (anchor.trackingState == TrackingState.STOPPED) {
                anchor.detach()
                iterator.remove()
                continue
            }

            val anchorPose = anchor.pose
            val anchorMatrix = FloatArray(16)
            anchorPose.toMatrix(anchorMatrix, 0)

            nativeUpdateAnchorPose(
                anchor.hashCode(),               // simple ID for demo purposes
                anchorMatrix,
                anchor.trackingState.ordinal     // tracking state flag
            )
        }
    }

    fun addAnchor(anchor: Anchor) {
        anchors.add(anchor)
        val matrix = FloatArray(16)
        anchor.pose.toMatrix(matrix, 0)
        nativeOnNewAircraftAnchor(anchor.hashCode(), matrix)
    }

    private external fun nativeOnNewAircraftAnchor(
        anchorId: Int,
        poseMatrix: FloatArray
    )

    private external fun nativeUpdateCameraPose(poseMatrix: FloatArray)

    private external  fun nativeUpdateAnchorPose(
        anchorId: Int,
        poseMatrix: FloatArray,
        trackingState: Int
    )

    private external fun nativeOnTrackingStateChanged(
        trackingState: Int
    )
}