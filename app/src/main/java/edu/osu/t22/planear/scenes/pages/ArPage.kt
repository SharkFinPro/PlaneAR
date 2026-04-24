package edu.osu.t22.planear.scenes.pages

import android.hardware.HardwareBuffer
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.adsb.AircraftOverlayStore
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.ImageMode
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.orientation.OrientationStore
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher
import kotlin.math.cos

class ArPage : Page {
    override val sceneId = SceneId.AR

    private var lastHb: HardwareBuffer? = null

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val width = sceneInfo.screenWidth
        val height = sceneInfo.screenHeight - navHeight

        val hb = AppSettings.hb

        val orientation = OrientationStore.data

        val phoneLat: Double = orientation.x.toDouble()
        val phoneLon = orientation.z.toDouble()
        val phoneAlt = orientation.y.toDouble()

        if (!AppSettings.cameraIsEnabled && AppSettings.canEnableCamera && AppSettings.hasCameraPermissions) {
            AppSettings.cameraIsEnabled = true
        }

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            if (hb != null && hb != lastHb) {
                updateCameraBuffer(hb)
                lastHb = hb
            }

            if (AppSettings.cameraIsEnabled) {
                imageMode(ImageMode.CORNER)
                camera(0, 0, width, height);
            } else {
                rectMode(RectMode.CORNER)
                fill(145)
                rect(0, 0, width, height)
            }

            set3DView(
                0,
                phoneAlt,
                0,
                orientation.pitchDeg,
                orientation.azimuthDeg - 90,
                orientation.rollDeg,
                width,
                height
            )

            val metersPerDegLat = 111_320.0
            val metersPerDegLon = 111_320.0 * cos(Math.toRadians(phoneLat))

            textFont("roboto", 30)
            fill(42, 42, 42)

            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)

            for (p in AircraftOverlayStore.aircraftData) {
                val dLat = p.position.latDeg - phoneLat
                val dLon = p.position.lonDeg - phoneLon
                val dAlt = p.position.altM - phoneAlt

                var x = (dLon * metersPerDegLon).toFloat()   // East
                var y = dAlt.toFloat()                       // Up
                var z = -(dLat * metersPerDegLat).toFloat()   // North

                var scale = 0.25f
                x *= scale
                y *= scale
                z *= scale

                point(x, y, z)

                scale = 0.5f
                x *= scale
                y *= scale
                z *= scale

                text3D(p.label, x, y, z)
            }

            fill(0)
            textFont("roboto", 18)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("AR Scene", width / 2, 40)

            textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
            textFont("roboto", 14)

            // Display orientation info
            textAlign(TextAlignH.LEFT, TextAlignV.TOP)
            textFont("roboto", 30)
            val cardinal = orientation.getCardinalDirection()
            text("Yaw: ${orientation.azimuthDeg.toInt()}° ($cardinal)", 50, 300)
            text("Pitch: ${orientation.pitchDeg.toInt()}°", 50, 400)
            text("Roll: ${orientation.rollDeg.toInt()}°", 50, 500)
        }

        postRender(sceneInfo, sceneSwitcher)
    }
}