package edu.osu.t22.planear.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.HardwareBuffer
import android.hardware.camera2.*
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import edu.osu.t22.planear.AppSettings
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.math.abs

class CameraManager(context: Context) {

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private val cameraOpenLock = Semaphore(1)
    private val handlerThread = HandlerThread("CameraThread").also { it.start() }
    private val handler = Handler(handlerThread.looper)
    private val executor = Executors.newSingleThreadExecutor()

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    var isActive: Boolean = false

    @SuppressLint("MissingPermission")
    fun start(viewWidth: Int, viewHeight: Int) {
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: run { Log.e("CameraManager", "No back camera found"); return }

        val chars = cameraManager.getCameraCharacteristics(cameraId)
        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        val sensorOrientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        val isRotated = sensorOrientation == 90 || sensorOrientation == 270

        val portraitAspect = if (viewWidth < viewHeight)
            viewWidth.toFloat() / viewHeight.toFloat()
        else
            viewHeight.toFloat() / viewWidth.toFloat()

        val bestSize = map.getOutputSizes(ImageFormat.YUV_420_888)
            ?.minByOrNull { size ->
                val sizeAspect = if (isRotated)
                    size.height.toFloat() / size.width.toFloat()
                else
                    size.width.toFloat() / size.height.toFloat()
                abs(sizeAspect - portraitAspect)
            } ?: run { Log.e("CameraManager", "No supported sizes"); return }

        Log.i("CameraManager", "Sensor: ${sensorOrientation}° | Size: ${bestSize.width}x${bestSize.height}")

        imageReader = ImageReader.newInstance(
            bestSize.width, bestSize.height,
            ImageFormat.YUV_420_888,
            2,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
        )

        imageReader!!.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            // Close the previous image only when we have a new one
            AppSettings.hbImage?.close()
            AppSettings.hbImage = image
            AppSettings.hb = image.hardwareBuffer
        }, handler)

        cameraOpenLock.acquire()
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                cameraOpenLock.release()
                startCaptureSession()
            }
            override fun onDisconnected(camera: CameraDevice) {
                cameraOpenLock.release()
                camera.close()
            }
            override fun onError(camera: CameraDevice, error: Int) {
                cameraOpenLock.release()
                Log.e("CameraManager", "Camera error: $error")
                camera.close()
            }
        }, handler)

        isActive = true
    }

    private fun startCaptureSession() {
        val surface = imageReader!!.surface

        val request = cameraDevice!!
            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }.build()

        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                session.setRepeatingRequest(request, null, handler)
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("CameraManager", "Capture session config failed")
            }
        }

        cameraDevice!!.createCaptureSession(SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            listOf(OutputConfiguration(surface)),
            executor,
            stateCallback
        ))
    }

    fun stop() {
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()
        captureSession = null
        cameraDevice = null
        imageReader = null

        isActive = false
    }
}