package edu.osu.t22.planear

import android.util.Log

class GraphicsEngineWrapper {
    private var nativeHandle: Long = 0
    private var isDestroyed = false

    init {
        nativeHandle = nativeCreate()
        if (nativeHandle == 0L) {
            throw RuntimeException("Failed to create native GraphicsEngine")
        }
        Log.d(TAG, "GraphicsEngine created with handle: $nativeHandle")
    }

    fun destroy() {
        if (!isDestroyed && nativeHandle != 0L) {
            Log.d(TAG, "Destroying GraphicsEngine with handle: $nativeHandle")
            nativeDestroy(nativeHandle)
            nativeHandle = 0
            isDestroyed = true
        }
    }

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)

    companion object {
        private const val TAG = "GraphicsEngineWrapper"
    }
}