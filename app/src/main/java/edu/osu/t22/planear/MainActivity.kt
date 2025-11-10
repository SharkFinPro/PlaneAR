package edu.osu.t22.planear

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.androidgamesdk.GameActivity
import edu.osu.t22.planear.adsb.AdsbModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : GameActivity() {
    companion object {
        init {
            System.loadLibrary("GraphicsEngine")
        }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val api = AdsbModule.provideApi()
        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    try {
                        val nearby = async(Dispatchers.IO) {
                            api.getNearbyAircraft(44.565722, -123.278917, 50)
                        }
                        val closest = async(Dispatchers.IO) {
                            api.getClosestAircraft(44.565722, -123.278917, 250)
                        }
                        Log.d("ADSB_TEST", "Got ${nearby.await().ac.size} aircraft")
                        Log.d("ADSB_TEST", "Closest aircraft: ${closest.await().ac.firstOrNull() ?: "No aircraft found"}")
                    } catch (e: Exception) {
                        Log.e("ADSB_TEST", "API call failed", e)
                    }
                    delay(5_000L)
                }
            }
        }
    }
}