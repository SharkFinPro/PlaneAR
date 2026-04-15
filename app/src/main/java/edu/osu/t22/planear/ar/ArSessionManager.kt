package edu.osu.t22.planear.ar

import android.hardware.HardwareBuffer
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import edu.osu.t22.planear.AppSettings

class ARSessionManager(
    private val session: Session,
    private val displayRotation: () -> Int
) {

    private val anchors = mutableListOf<Anchor>()

    private var viewportWidth: Int = 1
    private var viewportHeight: Int = 1

    private var nullHbCount = 0
    private var validHbCount = 0

    fun updateViewport(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
    }

    fun setCameraTextureName(tex: Int) {
        session.setCameraTextureName(tex)
    }


    fun onUpdateFrame() {
        Log.i("updateframe", "onupdateframe")
        session.setDisplayGeometry(displayRotation(), viewportWidth, viewportHeight)

        val frame: Frame
        try {
            frame = session.update()
        } catch (e: CameraNotAvailableException) {
//            nativeOnTrackingStateChanged(TrackingState.STOPPED.ordinal)
            return
        }


        val camera = frame.camera

//        nativeOnTrackingStateChanged(camera.trackingState.ordinal)

        if (camera.trackingState != TrackingState.TRACKING)  return

        val hb: HardwareBuffer? = frame.hardwareBuffer
        if (hb != null) {
            validHbCount++
            if (validHbCount % 60 == 0) {
                Log.i("ARSessionManager", "HB frames received: $validHbCount")
            }

            Log.i("ARSessionManager", "HB Received")
            AppSettings.hb = hb;

        } else {
            Log.w("ARSessionManager", "HW buffer null")
            nullHbCount++
            if (nullHbCount % 60 == 0) {
                Log.w("ARSessionManager", "HardwareBuffer was null $nullHbCount times")
            }
        }

        // Camera pose -> 4x4 matrix
        val cameraMatrix = FloatArray(16)
        camera.pose.toMatrix(cameraMatrix, 0)

        // Update any anchors we’re tracking (e.g., aircraft positions in AR space)
        val iterator = anchors.iterator()
        while (iterator.hasNext()) {
            val anchor = iterator.next()
            if (anchor.trackingState == TrackingState.STOPPED) {
                iterator.remove()
                continue
            }

            val anchorMatrix = FloatArray(16)
            anchor.pose.toMatrix(anchorMatrix, 0)
        }
    }

    fun addAnchor(anchor: Anchor) {
        anchors.add(anchor)
        val matrix = FloatArray(16)
        anchor.pose.toMatrix(matrix, 0)
    }
}