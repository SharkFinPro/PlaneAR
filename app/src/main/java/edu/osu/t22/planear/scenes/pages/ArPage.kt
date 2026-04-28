package edu.osu.t22.planear.scenes.pages

import android.hardware.HardwareBuffer
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.adsb.AircraftOverlayStore
import edu.osu.t22.planear.geo.GeoPoint
import edu.osu.t22.planear.geo.GeoUtils
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

    private val displayRadius = 3000.0f

    private val layerStep = 250.0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val width = sceneInfo.screenWidth
        val height = sceneInfo.screenHeight - navHeight

        val hb = AppSettings.hb

        val orientation = OrientationStore.data

        val phoneLat: Double = orientation.x.toDouble()
        val phoneLon = orientation.z.toDouble()
        val phoneAlt = orientation.y.toDouble()
        val phoneGeo = GeoPoint(phoneLat, phoneLon, phoneAlt)

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
                camera(0, 0, width, height)
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

            // Sort aircraft nearest-first so closer planes draw on top
            val sorted = AircraftOverlayStore.aircraftData.sortedBy { p ->
                GeoUtils.distanceMeters(phoneGeo, p.position)
            }

            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)

            sorted.forEachIndexed { index, p ->
                // Raw direction vector from phone to aircraft (East / Up / North in meters)
                val dLat = p.position.latDeg - phoneLat
                val dLon = p.position.lonDeg - phoneLon
                val dAlt = (p.position.altM - phoneAlt).toFloat()

                val rawX = (dLon * metersPerDegLon).toFloat()   // East
                val rawY = dAlt                                 // Up
                val rawZ = -(dLat * metersPerDegLat).toFloat()  // North (camera is -Z forward)

                // Distance in the XZ plane + full 3-D magnitude
                val rawLen = sqrt((rawX * rawX + rawY * rawY + rawZ * rawZ).toDouble()).toFloat()

                // Avoid division by zero for aircraft exactly at phone position
                val displayRadius = displayRadius + index * layerStep

                val (nx, ny, nz) = if (rawLen > 0.01f) {
                    Triple(
                        rawX / rawLen * displayRadius,
                        rawY / rawLen * displayRadius,
                        rawZ / rawLen * displayRadius
                    )
                } else {
                    // Fallback: place directly in front of the camera
                    Triple(0f, 0f, -displayRadius)
                }

                // Distance in km for the label
                val distKm = (rawLen / 1000.0)
                val distStr = if (distKm < 1.0) "${"%.0f".format(rawLen)} m"
                else "${"%.1f".format(distKm)} km"

                val dotBackRadius  = displayRadius
                val dotFrontRadius = displayRadius - layerStep * 0.4f

                val (bx, by, bz) = Triple(nx / displayRadius * dotBackRadius,  ny / displayRadius * dotBackRadius,  nz / displayRadius * dotBackRadius)
                val (fx, fy, fz) = Triple(nx / displayRadius * dotFrontRadius, ny / displayRadius * dotFrontRadius, nz / displayRadius * dotFrontRadius)

                fill(0);
                point(bx, by, bz, 270)

                fill(245);
                point(fx, fy, fz, 250)

                val textRadius = displayRadius - layerStep * 0.7f
                val tx = nx / displayRadius * textRadius
                val ty = ny / displayRadius * textRadius
                val tz = nz / displayRadius * textRadius

                textFont("roboto", 30); fill(42, 42, 42)
                text3D(p.label,  tx, ty + 50, tz)
                text3D(distStr,  tx, ty - 50, tz)
            }

            // HUD overlays (always 2-D, drawn after 3-D content)
            fill(0)
            textFont("roboto", 18)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("AR Scene", width / 2, 40)

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