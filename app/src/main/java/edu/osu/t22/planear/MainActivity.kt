package edu.osu.t22.planear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.GeomagneticField
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.MotionEvent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
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

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
            val cameraGranted = hasCameraPermission()
            val locationGranted = hasLocationPermission()

            if (cameraGranted) {
                Log.i("PERM", "Camera permission granted")
                AppSettings.hasCameraPermissions = true
            } else {
                Log.w("PERM", "Camera permission denied")
                handleDeniedPermission(Manifest.permission.CAMERA)
            }

            if (locationGranted) {
                Log.i("PERM", "Location permission granted")
            } else {
                Log.w("PERM", "Location permission denied")
                handleDeniedPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

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

        checkAndRequestPermissions()
        appLocationManager.start()
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

        if (hasCameraPermission()) {
            AppSettings.hasCameraPermissions = true
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

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestPermissions() {
        val needCamera = !hasCameraPermission()
        val needLocation = !hasLocationPermission()

        if (!needCamera && !needLocation) {
            return
        }

        val toRequest = mutableListOf<String>()
        if (needCamera) toRequest.add(Manifest.permission.CAMERA)
        if (needLocation) toRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)

        val showRationaleForAny = toRequest.any { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }
        if (showRationaleForAny) {
            AlertDialog.Builder(this)
                .setTitle("Permissions required")
                .setMessage("This app needs Camera (for AR) and Location (for nearby aircraft) permissions to function properly.")
                .setPositiveButton("Continue") { _, _ -> permissionLauncher.launch(toRequest.toTypedArray()) }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    private fun handleDeniedPermission(permission: String) {
        val shouldExplain = ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        if (!shouldExplain) {
            val msg = when (permission) {
                Manifest.permission.CAMERA -> "Camera access is required for AR features. Enable it in App Settings."
                Manifest.permission.ACCESS_FINE_LOCATION -> "Location access is required to show nearby aircraft. Enable it in App Settings."
                else -> "Required permission was denied. Enable it in App Settings."
            }

            AlertDialog.Builder(this)
                .setTitle("Permission denied")
                .setMessage(msg)
                .setPositiveButton("Open Settings") { _, _ -> openAppSettings() }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            val pretty = when (permission) {
                Manifest.permission.CAMERA -> "Camera"
                Manifest.permission.ACCESS_FINE_LOCATION -> "Location"
                else -> "Permission"
            }
            AlertDialog.Builder(this)
                .setTitle("$pretty permission needed")
                .setMessage("$pretty permission is required for core functionality. Would you like to grant it?")
                .setPositiveButton("Grant") { _, _ -> permissionLauncher.launch(arrayOf(permission)) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
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
