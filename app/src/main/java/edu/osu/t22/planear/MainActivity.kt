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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.view.Surface
import edu.osu.t22.planear.adsb.AdsbManager
import edu.osu.t22.planear.ar.ArManager
import edu.osu.t22.planear.location.AppLocationManager
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

    private lateinit var appLocationManager: AppLocationManager

    private lateinit var adsbManager: AdsbManager

    private lateinit var arManager: ArManager

    @Suppress("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appLocationManager = AppLocationManager(this, lifecycleScope)

        adsbManager = AdsbManager(appLocationManager)

        arManager = ArManager(this)

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
            appLocationManager.start()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()

        /* Verify Permission Access */
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        /* Start AR Manager */
        if (!arManager.hasSession()) {
            arManager.tryCreateSession { display?.rotation ?: Surface.ROTATION_0 }
        }

        arManager.resume()

        window.decorView.post {
            arManager.updateViewport(window.decorView.width, window.decorView.height)
        }

        // Location Manager
        appLocationManager.start()

        // Hide System UI
        hideSystemUi()
    }

    override fun onPause() {
        super.onPause()
        appLocationManager.stop()
        arManager.pause()
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
            arManager.tryCreateSession { display?.rotation ?: Surface.ROTATION_0 }
        }

        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            appLocationManager.start()
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
        arManager.onUpdateFrame()
    }

    fun setCameraTexture(textureId: Int) {
        arManager.setCameraTexture(textureId)
    }
}