package edu.osu.t22.planear

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.androidgamesdk.GameActivity
import edu.osu.t22.planear.adsb.AdsbModule
import kotlinx.coroutines.launch

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
            try {
                val aircraft = api.getClosestAircraft(51.89508, 2.79437, 250)
                Log.d("ADSB_TEST", "Got $aircraft aircraft")
            } catch (e: Exception) {
                Log.e("ADSB_TEST", "API call failed", e)
            }
        }
    }
}