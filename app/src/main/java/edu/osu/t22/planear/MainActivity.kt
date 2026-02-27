package edu.osu.t22.planear

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
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
import com.google.android.gms.location.*
import edu.osu.t22.planear.adsb.AdsbModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import edu.osu.t22.planear.geo.GeoUtils
import edu.osu.t22.planear.geo.GeoPoint
import android.view.Surface
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.CameraNotAvailableException
import edu.osu.t22.planear.scenes.Scene3
import edu.osu.t22.planear.scenes.SceneSwitcher
import android.hardware.HardwareBuffer
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log.d
import edu.osu.t22.planear.geo.Planeprojector


class MainActivity : GameActivity(), SensorEventListener {
    companion object {
        init {
            System.loadLibrary("GraphicsEngine")
            System.loadLibrary("planeARApp")
        }

        private const val CAMERA_PERMISSION_CODE = 0
        private const val LOCATION_PERMISSION_CODE = 1

        private const val H_FOV_DEG = 54.8 // this is for a samsung galaxy s20
        private const val V_FOV_DEG = 42.5 // https://www.camerafv5.com/devices/manufacturers/samsung/galaxy_s20_unknown_0/ find you device here and copy fov of camera


        @JvmStatic
        external fun nativeSetArReady(ready: Boolean)

        @JvmStatic
        external fun nativeSetAircraftDots(dots: FloatArray)
    }

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null

    private val rotationMatrix    = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    @Volatile private var deviceAzimuthDeg = 0.0
    @Volatile private var devicePitchDeg   = 0.0
    @Volatile private var deviceRollDeg    = 0.0


    private lateinit var sceneSwitcher: SceneSwitcher
    private var arSession: Session? = null
    private var arSessionManager: ARSessionManager? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null
    @Volatile private var lastKnownLocation: Location? = null

    @Suppress("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        // Initialize the scene manager
        sceneSwitcher = SceneSwitcher.initialize()

        // Register Kotlin scenes
        registerScenes()

        // get a new instance of the Retrofit API provider
        val api = AdsbModule.provideApi()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10000L
        ).apply {
            setMinUpdateIntervalMillis(2000L)
            setWaitForAccurateLocation(true)
        }.build()


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    try {

                        val loc = lastKnownLocation ?: getLastLocationOrNull()

                        if (loc == null) {
                            Log.w("ADSB", "No location available")
                        } else {
                            val lat = loc.latitude
                            val lon = loc.longitude

                            // get all nearby aircraft within 50nm
                            val nearby = async(Dispatchers.IO) {
                                api.getNearbyAircraft(lat, lon, 50)
                            }

                            // get closest aircraft (could also just be parsed from the nearby list
                            val closest = async(Dispatchers.IO) {
                                api.getClosestAircraft(lat, lon, 250)
                            }

                            // log results
                            val nearbyData = nearby.await()
                            d("ADSB_TEST", "Got ${nearbyData.total} aircraft")
                            val closestData = closest.await()

                            d(
                                "ADSB_TEST", "Closest aircraft: ${closestData.ac.firstOrNull() ?: "No aircraft found"}"
                            )
                            d(
                                "ADSB_TEST", "Timing data: (now: ${nearbyData.now}, cTime: ${nearbyData.cTime}, pTime: ${nearbyData.pTime})"
                            )


                            //--------------------TESTING GEO CALCULATIONS--------------------

                            // will be replaced with arcore geolocation
                            val userAltM = loc.altitude
                            val userPoint = GeoPoint(lat, lon, userAltM)
                            //val userHeadingDeg = 90.0 //facing east
                            val azimuth = deviceAzimuthDeg
                            val pitch = devicePitchDeg
                            val roll = deviceRollDeg

                            val screenW = window.decorView.width
                            val screenH = window.decorView.height

                            val projectableAircraft = nearbyData.ac.filter { it.isProjectable }

                            val acPoints = projectableAircraft.map { ac ->
                                GeoPoint(
                                    latDeg = ac.lat!!,
                                    lonDeg = ac.lon!!,
                                    altM = ac.altitudeMeters!!
                                )
                            }

                            val screenPoints = Planeprojector.projectAll(
                                user = userPoint,
                                aircraft = acPoints,
                                azimuthDeg = azimuth,
                                pitchDeg = pitch,
                                rollDeg = roll,
                                hFovDeg = H_FOV_DEG,
                                vFovDeg = V_FOV_DEG,
                                screenWidth = screenW,
                                screenHeight = screenH
                            )

                            val dots = Planeprojector.toNativeArray(screenPoints)
                            nativeSetAircraftDots(dots)

                            screenPoints.forEachIndexed { i, point ->
                                if (point.visible) {
                                    val ac = projectableAircraft[i]
                                    d("PROJECTION",
                                        "${ac.label} ->" + "x=${"%.0f".format(point.x)} " +
                                                "y=${"%.0f".format(point.y)} " +
                                                "dist=${"%.0f".format(point.distance)}m " +
                                                "type=${ac.t ?: "?"} " +
                                                "gs=${ac.gs ?: "?"}kts"
                                    )
                                }
                            }
                            d("PROJECTION", "${screenPoints.count { it.visible }} / ${acPoints.size} aircraft on screen")

                            val closestAc = closestData.ac.firstOrNull { it.isProjectable }
                            if (closestAc != null) {
                                val acPoint = GeoPoint(
                                    latDeg = closestAc.lat!!,
                                    lonDeg = closestAc.lon!!,
                                    altM = closestAc.altitudeMeters!!
                                )

                                val dir = GeoUtils.relativeDirection(
                                    user = userPoint,
                                    userHeadingDeg = azimuth,
                                    aircraft = acPoint
                                )
                                d("Closest",
                                    "label=${closestAc.label} " +
                                    "type=${closestAc.t ?: "unknown"} " +
                                    "alt=${"%.0f".format(closestAc.altitudeMeters!!)}m " +
                                    "gs=${closestAc.gs ?: "?"}kts " +
                                    "track=${closestAc.track ?: "?"}° " +
                                    "dist=${"%.0f".format(dir.distanceMeters)}m " +
                                    "elevation=${"%.1f".format(dir.elevationDeg)}°"
                                )

                                val enh = GeoUtils.enhVector(userPoint, acPoint)
                                d("CLOSEST_ENH",
                                    "east=${"%.1f".format(enh.east)}m " +
                                    "north=${"%.1f".format(enh.north)}m " +
                                    "height=${"%.1f".format(enh.height)}m"
                                )
                            } else {
                                Log.d("CLOSEST", "No projectable closest aircraft")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ADSB_TEST", "API call failed", e)
                    }
                    onArUpdateFrame() // something might need to change I have to call frame update and then the next call is delayed at least 5 seconds right after

                    // repeat every 5 seconds
                    delay(500L)//0.5 sec
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
            startLocationUpdates()
        }
    }


    @Suppress("MissingPermission")
    private fun registerScenes() {
        // Register scenes with unique IDs
        sceneSwitcher.registerScene(3, Scene3())

        // Set the initial scene
        sceneSwitcher.setCurrentScene(3)
    }


    override fun onResume() {
        super.onResume()
        rotationVectorSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

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

        startLocationUpdates()
        hideSystemUi()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        stopLocationUpdates()
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

        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        deviceAzimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).let { if (it < 0) it + 360.0 else it }
        devicePitchDeg = Math.toDegrees(orientationAngles[1].toDouble())
        deviceRollDeg = Math.toDegrees(orientationAngles[2].toDouble())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun getLastLocationOrNull(): Location? {
        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            Log.w("PlaneAR", "Failed to get last location", e)
            null
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            Log.w("LOCATION", "Location permission not granted")
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    lastKnownLocation = loc
                    d("LOCATION", "Location update: ${loc.latitude}, ${loc.longitude}")
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback as LocationCallback, mainLooper)

        lifecycleScope.launch {
            val last = getLastLocationOrNull()
            if (last != null) {
                lastKnownLocation = last
                d("LOCATION", "Populated initial lastKnownLocation: ${last.latitude}, ${last.longitude}")
            }
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
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
                    textureUpdateMode = Config.TextureUpdateMode.EXPOSE_HARDWARE_BUFFER

                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    depthMode = if (arSession!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                        Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                }
                arSession!!.configure(config)

                Log.i("PlaneAR", "AR config applied: textureMode=${config.textureUpdateMode}")

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

        //arSessionManager?.setCameraTextureName(textureId)
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

    private var tick = 0
    private var nullHbCount = 0
    private var validHbCount = 0

    fun onUpdateFrame() {
        d("ARSession", "onUpdateFrame called")

        // Keep ARCore in sync with display rotation & surface size
        session.setDisplayGeometry(displayRotation(), viewportWidth, viewportHeight)


        val frame: Frame = try {
            session.update()
        } catch (e: CameraNotAvailableException) {
            nativeOnTrackingStateChanged(TrackingState.STOPPED.ordinal)
            return
        }

        val camera = frame.camera
        nativeOnTrackingStateChanged(camera.trackingState.ordinal)

        if (camera.trackingState != TrackingState.TRACKING) return

        val hb = frame.hardwareBuffer
        if (hb != null) {
            validHbCount++
            if (validHbCount % 60 == 0) Log.i("ARSession", "HB frames received: $validHbCount")
            try {
                nativeOnHardwareBuffer(hb, frame.timestamp)
            } finally {
                hb.close()
            }
        } else {
            nullHbCount++
            if (nullHbCount % 60 == 0) Log.w("ARSession", "HB was null $nullHbCount times")
            // acording to my logcat no frames where null on the emulator
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

    private external fun nativeOnHardwareBuffer(
        hardwareBuffer: HardwareBuffer,
        timestamp: Long
    )

    private external fun nativeGetHardwareBufferFrameCount(): Long
}