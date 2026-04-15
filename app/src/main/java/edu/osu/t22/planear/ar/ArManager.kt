package edu.osu.t22.planear.ar

import android.content.Context
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import edu.osu.t22.planear.MainActivity

class ArManager(private val context: Context) {

    private var arSession: Session? = null
    private var sessionManager: ARSessionManager? = null

    fun tryCreateSession(displayRotation: () -> Int): Boolean {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            if (!availability.isSupported) {
                Log.e("ArManager", "ARCore not supported")
                return false
            }

            if (arSession == null) {
                arSession = Session(context)
                Log.i("ArManager", "ARCore session created")

                val config = Config(arSession).apply {
                    textureUpdateMode = Config.TextureUpdateMode.EXPOSE_HARDWARE_BUFFER

                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    depthMode =
                        if (arSession!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                            Config.DepthMode.AUTOMATIC
                        else Config.DepthMode.DISABLED
                }
                arSession!!.configure(config)

                sessionManager = ARSessionManager(
                    session = arSession!!,
                    displayRotation = displayRotation
                )
            }

            return true

        } catch (e: Exception) {
            Log.e("ArManager", "Failed to create ARCore session", e)
            return false
        }
    }

    fun resume() {
        try {
            arSession?.resume()
        } catch (e: Exception) {
            Log.e("ArManager", "Failed to resume AR session", e)
        }
    }

    fun pause() {
        try {
            arSession?.pause()
        } catch (e: Exception) {
            Log.e("ArManager", "Failed to pause AR session", e)
        }
    }

    fun updateViewport(width: Int, height: Int) {
        sessionManager?.updateViewport(width, height)
    }

    fun onUpdateFrame() {
        sessionManager?.onUpdateFrame()
    }

    fun hasSession(): Boolean = arSession != null
}