package edu.osu.t22.planear

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.Surface
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
import edu.osu.t22.planear.ar.ArManager
import edu.osu.t22.planear.location.AppLocationManager
import edu.osu.t22.planear.scenes.SceneSwitcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import edu.osu.t22.planear.adsb.AircraftOverlayStore
import edu.osu.t22.planear.adsb.AircraftScreenPoint

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
        /*
                @JvmStatic
                external fun nativeSetAircraftDots(dots: FloatArray)

                @JvmStatic
                external fun nativeSetAircraftLabels(
                    xs: FloatArray,
                    ys: FloatArray,
                    labels: Array<String>
                )
         */
    }

    private lateinit var sceneSwitcher: SceneSwitcher
    private lateinit var appLocationManager: AppLocationManager
    private lateinit var adsbManager: AdsbManager
    private lateinit var arManager: ArManager

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
        arManager = ArManager(this)

        sceneSwitcher = SceneSwitcher.initialize()

        // Gesture Detector Setup
        frameGestureDetector = FrameGestureDetector(this)
        SceneSwitcher.gestureDetector = frameGestureDetector

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    try {
                        val loc = appLocationManager.lastKnownLocation

                        if (loc != null) {
                            val dm = resources.displayMetrics
                            val screenW = dm.widthPixels
                            val screenH = dm.heightPixels

                            val result = adsbManager.pollAndProject(
                                location = loc,
                                azimuthDeg = deviceAzimuthDeg,
                                pitchDeg = devicePitchDeg,
                                rollDeg = deviceRollDeg,
                                screenW = screenW,
                                screenH = screenH
                            )

                            if (result != null) {

                                AircraftOverlayStore.points =
                                    result.xs.indices.map { i ->
                                        AircraftScreenPoint(
                                            x = result.xs[i],
                                            y = result.ys[i],
                                            label = result.labels[i]
                                        )
                                    }
                            } else {
                                AircraftOverlayStore.points = emptyList()
                            }
                        } else {
                            AircraftOverlayStore.points = emptyList()
                            Log.w("ADSB_EXECUTION", "No location available yet")
                        }
                    } catch (e: Exception) {
                        AircraftOverlayStore.points = emptyList()
                        Log.e("ADSB_EXECUTION", "ADS-B polling failed", e)
                    }

                    delay(5_000L)
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
        }

        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        if (!arManager.hasSession()) {
            arManager.tryCreateSession { display?.rotation ?: Surface.ROTATION_0 }
        }

        arManager.resume()

        window.decorView.post {
            arManager.updateViewport(
                window.decorView.width,
                window.decorView.height
            )
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
        arManager.pause()
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            deviceAzimuthDeg = Math.toDegrees(orientationAngles[0].toDouble())
                .let { if (it < 0) it + 360.0 else it }
            devicePitchDeg   = Math.toDegrees(orientationAngles[1].toDouble())
            deviceRollDeg    = Math.toDegrees(orientationAngles[2].toDouble())
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
            arManager.tryCreateSession { display?.rotation ?: Surface.ROTATION_0 }
        }

        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
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

    fun onArUpdateFrame() {
        arManager.onUpdateFrame()
    }

    fun setCameraTexture(textureId: Int) {
        arManager.setCameraTexture(textureId)
    }
}
