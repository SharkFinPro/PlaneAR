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
import edu.osu.t22.planear.adsb.AdsbModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Button

class MainActivity : GameActivity() {
    companion object {
        init {
            System.loadLibrary("GraphicsEngine")
        }

        private const val CAMERA_PERMISSION_CODE = 0
    }

    private var arSession: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main) // this links to activity_main.xml

        val myButton = findViewById<Button>(R.id.actionButton)
        myButton.setOnClickListener {
            // Do something
            Log.d("PlaneAR", "Button clicked!")
        }

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
                    } catch (e: Exception) {
                        Log.e("ADSB_TEST", "API call failed", e)
                    }

                    // repeat every 5 seconds
                    delay(5_000L)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            maybeCreateSession()
        }
    }
    @Suppress("MissingPermission")


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
            maybeCreateSession()
        }
    }

    private fun maybeCreateSession() {
        try {
            val installStatus = ArCoreApk.getInstance().requestInstall(this, true)
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                return
            }

            arSession = Session(this)
            Log.i("PlaneAR", "ARCore session created.")
        } catch (e: Exception) {
            Log.e("PlaneAR", "Failed to create a ARCore session", e)
        }
    }

    override fun onResume() {
        super.onResume()
        arSession?.resume()
        hideSystemUi()
    }

    override fun onPause() {
        super.onPause()
        arSession?.pause()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
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
}