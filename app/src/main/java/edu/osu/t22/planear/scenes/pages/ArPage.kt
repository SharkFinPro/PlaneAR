package edu.osu.t22.planear.scenes.pages

import android.hardware.HardwareBuffer
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.adsb.AircraftOverlayStore
import edu.osu.t22.planear.geo.Planeprojector
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.ImageMode
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.orientation.OrientationStore
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher
import kotlin.math.cos
import kotlin.math.sqrt

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

            for (indicator in AircraftOverlayStore.edgeIndicators) {
                pushMatrix()
                translate(indicator.x, indicator.y)
                rotate(indicator.angleDeg)

                val s = 24f
                val h = (sqrt(3.0) / 2.0 * s).toFloat()

                fill(255, 200, 0)  // make it visible for debugging
                triangle(
                    s / 2f, 0f,
                    -s / 2f, -h / 2f,
                    -s / 2f, h / 2f
                )

                popMatrix()
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

            textAlign(TextAlignH.LEFT, TextAlignV.TOP)
            textFont("roboto", 30)
            val cardinal = orientation.getCardinalDirection()
            text("Yaw: ${orientation.azimuthDeg.toInt()}° ($cardinal)", 50, 300)
            text("Pitch: ${orientation.pitchDeg.toInt()}°", 50, 400)
            text("Roll: ${orientation.rollDeg.toInt()}°", 50, 500)

            val proj2DPoints = mutableListOf<Pair<String, edu.osu.t22.planear.geo.ScreenPoint>>()
            AircraftOverlayStore.aircraftData.forEach { aircraft ->
                val sp = edu.osu.t22.planear.geo.Planeprojector.project(
                    user = edu.osu.t22.planear.geo.GeoPoint(phoneLat, phoneLon, phoneAlt),
                    aircraft = aircraft.position,
                    azimuthDeg = orientation.azimuthDeg,
                    pitchDeg = orientation.pitchDeg,
                    rollDeg = orientation.rollDeg,
                    hFovDeg = edu.osu.t22.planear.adsb.AdsbManager.H_FOV_DEG,
                    vFovDeg = edu.osu.t22.planear.adsb.AdsbManager.V_FOV_DEG,
                    screenWidth = width.toInt(),
                    screenHeight = height.toInt()
                )
                proj2DPoints.add(aircraft.label to sp)
            }

            var totalDiff = 0f
            var count = 0

            textFont("roboto", 20)
            textAlign(TextAlignH.LEFT, TextAlignV.TOP)

            for ((label, sp) in proj2DPoints) {
                if (!sp.visible) {
                    val edge = Planeprojector.getEdgeIndicator(sp, width.toInt(), height.toInt())
                    pushMatrix()
                    translate(edge.x, edge.y)
                    rotate(edge.angleDeg)
                    val s = 24f
                    val h = (sqrt(3.0) / 2.0 * s).toFloat()
                    fill(255, 200, 0)
                    triangle(s / 2f, 0f, -s / 2f, -h / 2f, -s / 2f, h / 2f)
                    popMatrix()
                }
            }

            AircraftOverlayStore.aircraftData.forEachIndexed { i, aircraft ->
                val storedPt = AircraftOverlayStore.points.firstOrNull { it.label == aircraft.label }
                    ?: return@forEachIndexed
                val projPt = proj2DPoints.getOrNull(i)?.second
                    ?: return@forEachIndexed

                if (projPt.visible) {
                    val diffX = storedPt.x - projPt.x
                    val diffY = storedPt.y - projPt.y
                    val diff = sqrt(diffX * diffX + diffY * diffY)
                    totalDiff += diff
                    count++

                    fill(255, 80, 80)
                    text(
                        "${aircraft.label}: store(${storedPt.x.toInt()},${storedPt.y.toInt()}) proj(${projPt.x.toInt()},${projPt.y.toInt()}) Δ=${diff.toInt()}px",
                        20, 580 + i * 32
                    )
                }
            }

            if (count > 0) {
                fill(255, 255, 0)
                text("Avg pixel diff: ${(totalDiff / count).toInt()}px over $count planes", 50, 550)

                var avgDx = 0f
                var avgDy = 0f

                AircraftOverlayStore.aircraftData.forEachIndexed { i, aircraft ->
                    val storedPt = AircraftOverlayStore.points.getOrNull(i) ?: return@forEachIndexed
                    val projPt = proj2DPoints.getOrNull(i)?.second ?: return@forEachIndexed
                    if (projPt.visible) {
                        avgDx += storedPt.x - projPt.x
                        avgDy += storedPt.y - projPt.y
                    }
                }

                avgDx /= count
                avgDy /= count

                val dirDesc = buildString {
                    if (avgDy < -10f) append("UP ") else if (avgDy > 10f) append("DOWN ")
                    if (avgDx < -10f) append("LEFT") else if (avgDx > 10f) append("RIGHT")
                    if (isEmpty()) append("CENTERED")
                }

                text("Store is avg ${avgDx.toInt()}px,${avgDy.toInt()}px ($dirDesc) from Planeprojector", 50, 590)
            }
        }

        postRender(sceneInfo, sceneSwitcher)
    }
}