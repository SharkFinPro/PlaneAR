package edu.osu.t22.planear

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.GeomagneticField
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.MotionEvent
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.androidgamesdk.GameActivity
import edu.osu.t22.planear.adsb.AdsbManager
import edu.osu.t22.planear.achievements.AchievementStore
import edu.osu.t22.planear.location.AppLocationManager
import edu.osu.t22.planear.scenes.SceneSwitcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import edu.osu.t22.planear.camera.CameraManager
import edu.osu.t22.planear.orientation.OrientationData
import edu.osu.t22.planear.orientation.OrientationStore
import kotlin.math.atan2
import kotlin.math.sqrt

class MainActivity : GameActivity() {

    companion object {
        init {
            System.loadLibrary("GraphicsEngine")
            System.loadLibrary("planeARApp")
        }

        private const val CAMERA_PERMISSION_CODE = 0
        private const val LOCATION_PERMISSION_CODE = 1
    }

    private lateinit var sceneSwitcher: SceneSwitcher
    private lateinit var appLocationManager: AppLocationManager
    private lateinit var adsbManager: AdsbManager

    private lateinit var cameraManager: CameraManager

    private lateinit var frameGestureDetector: FrameGestureDetector

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private val rotationMatrix    = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    @Volatile private var deviceAzimuthDeg = 0.0
    @Volatile private var devicePitchDeg = 0.0
    @Volatile private var deviceRollDeg = 0.0

    @Suppress("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        appLocationManager = AppLocationManager(this, lifecycleScope)
        adsbManager = AdsbManager(appLocationManager)

        cameraManager = CameraManager(this)

        // Initialize achievement persistence
        AchievementStore.init(this)

        sceneSwitcher = SceneSwitcher.initialize()

        // Gesture Detector Setup
        frameGestureDetector = FrameGestureDetector(this)
        SceneSwitcher.gestureDetector = frameGestureDetector

        // Coroutine 1: Fetch aircraft data every 5 seconds
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    try {
                        adsbManager.fetchAircraftData(appLocationManager.lastKnownLocation)
                    } catch (e: Exception) {
                        Log.e("ADSB_EXECUTION", "ADS-B fetch failed", e)
                    }
                    delay(2_500L)
                }
            }
        }

        // Coroutine 2: Accumulate in-app tracking time (for marathon_spotter achievement)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(1_000L)
                    AchievementStore.addTrackingTime(1_000L)
                }
            }
        }

        // Coroutine 2: Project aircraft to screen at 60fps for smooth movement
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    try {
                        val loc = appLocationManager.lastKnownLocation
                        if (loc != null) {
                            val dm = resources.displayMetrics
                            adsbManager.projectToScreen(
                                location = loc,
                                azimuthDeg = deviceAzimuthDeg,
                                pitchDeg = devicePitchDeg,
                                rollDeg = deviceRollDeg,
                                screenW = dm.widthPixels,
                                screenH = dm.heightPixels
                            )
                        }

                        if (!AppSettings.cameraIsEnabled && cameraManager.isActive) {
                            cameraManager.stop()
                        }
                        else if (AppSettings.cameraIsEnabled && !cameraManager.isActive) {
                            window.decorView.post {
                                cameraManager.start(
                                    window.decorView.width,
                                    window.decorView.height
                                )
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("ADSB_EXECUTION", "ADS-B projection failed", e)
                    }
                    delay(16L) // ~60fps
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        } else {
            appLocationManager.start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        frameGestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()

        rotationVectorSensor?.also { sensor ->
            sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        } else {
            AppSettings.hasCameraPermissions = true
        }

        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        if (hasLocationPermission()) {
            appLocationManager.start()
        }
        hideSystemUi()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
        appLocationManager.stop()

        cameraManager.stop()
        AppSettings.cameraIsEnabled = false
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // The direction out the back of the phone (camera direction) in device coords is [0, 0, -1]
            // Transform to world coords: R * [0, 0, -1] = [-R[2], -R[5], -R[8]]
            // R is stored as: R[0] R[1] R[2]
            //                 R[3] R[4] R[5]
            //                 R[6] R[7] R[8]
            val backEast = -rotationMatrix[2].toDouble()   // X in world (East)
            val backNorth = -rotationMatrix[5].toDouble()  // Y in world (North)
            val backUp = -rotationMatrix[8].toDouble()     // Z in world (Up)

            // Calculate magnetic azimuth: angle in horizontal plane (East, North)
            // 0 = Magnetic North, 90 = Magnetic East, etc.
            // atan2(y, x) gives angle from positive x-axis. We want angle from North (Y axis)
            // so we use atan2(East, North) which gives angle clockwise from North
            val magneticAzimuthDeg = Math.toDegrees(atan2(backEast, backNorth))
                .let { if (it < 0) it + 360.0 else it }

            // Convert to true north using magnetic declination at current location
            val loc = appLocationManager.lastKnownLocation
            if (loc != null) {
                val geoField = GeomagneticField(
                    loc.latitude.toFloat(),
                    loc.longitude.toFloat(),
                    loc.altitude.toFloat(),
                    System.currentTimeMillis()
                )
                deviceAzimuthDeg = (magneticAzimuthDeg + geoField.declination).mod(360.0)
            } else {
                deviceAzimuthDeg = magneticAzimuthDeg
            }

            // Calculate pitch: angle above/below horizon
            // Positive = up, Negative = down
            val horizontalDist = sqrt(backEast * backEast + backNorth * backNorth)
            devicePitchDeg = Math.toDegrees(atan2(backUp, horizontalDist))

            // Roll is not directly used for camera direction projection, but kept for reference
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            deviceRollDeg = Math.toDegrees(orientationAngles[2].toDouble())

            // Update the orientation store for UI display
            if (loc != null)
            {
                OrientationStore.data = OrientationData(
                    azimuthDeg = deviceAzimuthDeg,
                    pitchDeg = devicePitchDeg,
                    rollDeg = deviceRollDeg,
                    x = loc.latitude.toFloat(),
                    y = loc.altitude.toFloat(),
                    z = loc.longitude.toFloat()
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
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
            AppSettings.hasCameraPermissions = true
        }

        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            @SuppressWarnings("MissingPermission") // suppress Lint errors for if the user has removed permissions
            if (hasLocationPermission()) {
                appLocationManager.start()
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_CODE
        )
    }

    private fun hideSystemUi() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }
}
