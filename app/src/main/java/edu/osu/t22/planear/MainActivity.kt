package edu.osu.t22.planear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.androidgamesdk.GameActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session

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

    private fun maybeCreateSession() {
        try {
            val installStatus = ArCoreApk.getInstance().requestInstall(this, true)
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {

                return
            }

            arSession = Session(this)
            // nativeOnArSessionCreated(arSession!!.nativeHandle)
        } catch (e: Exception) {
            e.printStackTrace()
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

    private external fun nativeOnArSessionCreated(sessionHandle: Long)
}