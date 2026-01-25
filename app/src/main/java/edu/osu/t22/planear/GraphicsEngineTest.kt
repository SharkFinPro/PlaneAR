package edu.osu.t22.planear

import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.androidgamesdk.GameActivity

class GraphicsEngineTest : GameActivity() {
    companion object {
        init {
            System.loadLibrary("graphicsEngineTest")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        hideSystemUi()
    }

    override fun onPause() {
        super.onPause()
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