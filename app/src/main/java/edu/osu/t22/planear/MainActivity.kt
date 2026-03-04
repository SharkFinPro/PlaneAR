package edu.osu.t22.planear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.util.Log
import androidx.annotation.RequiresPermission
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.view.Surface
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.CameraNotAvailableException
import edu.osu.t22.planear.adsb.AdsbManager
import edu.osu.t22.planear.locationManager.LocationManager
import edu.osu.t22.planear.scenes.SceneSwitcher

class MainActivity : GameActivity() {
    companion object {
        init {
            System.loadLibrary("GraphicsEngine")
            System.loadLibrary("planeARApp")
        }

        private const val CAMERA_PERMISSION_CODE = 0
        private const val LOCATION_PERMISSION_CODE = 1

        @JvmStatic
        external fun nativeSetArReady(ready: Boolean)
    }

    private lateinit var sceneSwitcher: SceneSwitcher

    private var arSession: Session? = null
    private var arSessionManager: ARSessionManager? = null

    private lateinit var locationManager: LocationManager

    private lateinit var adsbManager: AdsbManager

    @Suppress("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager = LocationManager(this, lifecycleScope)

        adsbManager = AdsbManager(locationManager)

        // Initialize the scene manager
        sceneSwitcher = SceneSwitcher.initialize()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    try {
                        adsbManager.poll()
                    } catch (e: Exception) {
                        Log.e("ADSB_EXECUTION", "API call failed", e)
                    }

                    // repeat every 5 seconds
                    delay(5_000L)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        } else {
            locationManager.start()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()

        //check camera permissions
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        //check location permissions
        if (!hasLocationPermission()) {
            requestLocationPermission()
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

        locationManager.start()
        hideSystemUi()
    }

    override fun onPause() {
        super.onPause()
        locationManager.stop()
        try {
            arSession?.pause()
        } catch (e: Exception) {
            Log.e("PlaneAR", "Failed to pause AR session", e)
        }
    }

    // permission callbacks
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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

        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.start()
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

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_CODE
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

                val config = Config(arSession).apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    depthMode =
                        if (arSession!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                            Config.DepthMode.AUTOMATIC
                        else Config.DepthMode.DISABLED
                }
                arSession!!.configure(config)

                arSessionManager = ARSessionManager(
                    session = arSession!!,
                    displayRotation = { display?.rotation ?: Surface.ROTATION_0 }
                )

                nativeSetArReady(true)
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

        val frame: Frame
        try {
            frame = session.update()
        } catch (e: CameraNotAvailableException) {
            nativeOnTrackingStateChanged(TrackingState.STOPPED.ordinal)
            return
        }

        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) {
            // Inform native side that tracking is limited or lost
            nativeOnTrackingStateChanged(camera.trackingState.ordinal)
            return
        }


        // Camera pose -> 4x4 matrix
        val cameraMatrix = FloatArray(16)
        camera.pose.toMatrix(cameraMatrix, 0)

        // Send camera pose to native rendering engine
        nativeUpdateCameraPose(cameraMatrix)

        // Update any anchors we’re tracking (e.g., aircraft positions in AR space)
        val iterator = anchors.iterator()
        while (iterator.hasNext()) {
            val anchor = iterator.next()
            if (anchor.trackingState == TrackingState.STOPPED) {
                iterator.remove()
                continue
            }

            val anchorMatrix = FloatArray(16)
            anchor.pose.toMatrix(anchorMatrix, 0)

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