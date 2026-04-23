package edu.osu.t22.planear.scenes.pages

import android.util.Log
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.adsb.AircraftOverlayStore
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.ImageMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.orientation.OrientationStore
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher
import kotlin.math.cos
import kotlin.math.sqrt

class ArPage : Page {
    override val sceneId = SceneId.AR

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val width = sceneInfo.screenWidth
        val height = sceneInfo.screenHeight - navHeight
        val points = AircraftOverlayStore.points

        val hb = AppSettings.hb;

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            if (hb != null)
            {
                updateCameraBuffer(hb);
                hb.close();
                AppSettings.hb = null;
            }

            fill(245)
            rect(0, 0, width, height)

            imageMode(ImageMode.CORNER)
            camera(0, 0, width, height);

            fill(0)
            textFont("roboto", 18)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("AR Scene", width / 2, 40)

            // Display orientation info
            textAlign(TextAlignH.LEFT, TextAlignV.TOP)
            textFont("roboto", 30)
            val orientation = OrientationStore.data
            val cardinal = orientation.getCardinalDirection()
            text("Yaw: ${orientation.azimuthDeg.toInt()}° ($cardinal)", 50, 300)
            text("Pitch: ${orientation.pitchDeg.toInt()}°", 50, 400)
            text("Roll: ${orientation.rollDeg.toInt()}°", 50, 500)

            textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
            textFont("roboto", 14)

            for (point in points) {
                val x = point.x.toInt()
                val y = point.y.toInt()

                if (x >= 0 && x <= width && y >= 0 && y <= height) {
                    fill(0)
                    ellipse(x, y, 12, 12)

                    text(point.label, x + 14, y)
                }
            }

            val lat0: Double = orientation.x.toDouble()  // phone lat
            val lon0 = orientation.z.toDouble()  // phone lon
            val alt0 = orientation.y.toDouble()  // phone alt

            set3DView(
                0,
                alt0,
                0,
                orientation.pitchDeg,
                orientation.azimuthDeg - 90,
                orientation.rollDeg,
                width,
                height
            )

            val metersPerDegLat = 111_320.0
            val metersPerDegLon = 111_320.0 * cos(Math.toRadians(lat0))

            textSize(30)
            fill(42, 245, 42)

            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)

            for (p in AircraftOverlayStore.aircraftData) {
                val dLat = p.position.latDeg - lat0
                val dLon = p.position.lonDeg - lon0
                val dAlt = p.position.altM - alt0

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
        }

        postRender(sceneInfo, sceneSwitcher)
    }
}