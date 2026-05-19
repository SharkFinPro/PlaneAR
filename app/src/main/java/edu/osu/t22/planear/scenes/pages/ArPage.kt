package edu.osu.t22.planear.scenes.pages

import android.hardware.HardwareBuffer
import edu.osu.t22.planear.AppSettings
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
import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.achievements.ALL_ACHIEVEMENTS
import edu.osu.t22.planear.achievements.AchievementStore
import edu.osu.t22.planear.adsb.AdsbRepository
import kotlin.math.sin

class ArPage : Page {
    override val sceneId = SceneId.AR

    private var lastHb: HardwareBuffer? = null

    // Achievement popup state
    private var showingAchievementId: String? = null
    private var achievementAnimProgress: Float = 0.0f
    private var achievementClosing: Boolean = false

    private val initialDisplayRadius = 3000.0f

    private val layerStep = 250.0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val width = sceneInfo.screenWidth
        val height = sceneInfo.screenHeight - navHeight

        // Mark that we are on the AR page that enables achievement tracking
        AchievementStore.isOnArPage = true

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
                fill(90, 160, 255)
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
            val aircraftRepository = SceneSwitcher.adsbManager.getRepository()

            val sorted = aircraftRepository.getAircraft().sortedBy { p ->
                GeoUtils.distanceMeters(phoneGeo, p.getPosition())
            }

            val yaw = Math.toRadians((orientation.azimuthDeg - 90))
            val pitch = Math.toRadians(orientation.pitchDeg)

            val fx = cos(pitch) * cos(yaw)
            val fy = sin(pitch)
            val fz = cos(pitch) * sin(yaw)

            val flen = sqrt(fx*fx + fy*fy + fz*fz).toFloat()

            val cx = (fx / flen).toFloat()
            val cy = (fy / flen).toFloat()
            val cz = (fz / flen).toFloat()

            var bestIndex = -1
            var bestDot = -1f

            sorted.forEachIndexed { index, p ->
                val position = p.getPosition()
                val dLat = position.latDeg - phoneLat
                val dLon = position.lonDeg - phoneLon
                val dAlt = (position.altM - phoneAlt).toFloat()

                val rawX = (dLon * metersPerDegLon).toFloat()
                val rawY = dAlt
                val rawZ = -(dLat * metersPerDegLat).toFloat()

                val len = sqrt(rawX * rawX + rawY * rawY + rawZ * rawZ)
                if (len > 0.01f) {
                    val ax = rawX / len
                    val ay = rawY / len
                    val az = rawZ / len

                    val dot = ax * cx + ay * cy + az * cz

                    if (dot > bestDot) {
                        bestDot = dot
                        bestIndex = index
                    }
                }
            }

            if (bestIndex >= 0 && bestDot > 0.95f) {
                logFlightHistory(sorted[bestIndex])
            }

            val reordered = if (bestIndex > 0) {
                val mutable = sorted.toMutableList()
                val best = mutable.removeAt(bestIndex)
                mutable.add(0, best)
                mutable
            } else {
                sorted
            }

            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)

            reordered.forEachIndexed { index, p ->
                // Raw direction vector from phone to aircraft (East / Up / North in meters)
                val position = p.getPosition()
                val dLat = position.latDeg - phoneLat
                val dLon = position.lonDeg - phoneLon
                val dAlt = (position.altM - phoneAlt).toFloat()

                val rawX = (dLon * metersPerDegLon).toFloat()   // East
                val rawY = dAlt                                 // Up
                val rawZ = -(dLat * metersPerDegLat).toFloat()  // North (camera is -Z forward)

                // Distance in the XZ plane + full 3-D magnitude
                val rawLen = sqrt((rawX * rawX + rawY * rawY + rawZ * rawZ).toDouble()).toFloat()

                // Avoid division by zero for aircraft exactly at phone position
                val displayRadius = initialDisplayRadius + index * layerStep

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

        // Check for new achievement notifications
        if (showingAchievementId == null) {
            val nextId = AchievementStore.popNotification()
            if (nextId != null) {
                showingAchievementId   = nextId
                achievementAnimProgress = 0.0f
                achievementClosing      = false
            }
        }

        // Draw achievement popup if active
        if (showingAchievementId != null) {
            drawAchievementPopup(sceneInfo)
        }

        postRender(sceneInfo, sceneSwitcher)
    }

    /**
     * Draw the achievement unlocked popup card, centered on screen.
     * Shows the achievement emoji, name, and a dismiss button.
     * Slides in from top with eased animation.
     */
    private fun drawAchievementPopup(sceneInfo: SceneInfo) {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight - navHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current
        val step     = 0.05f

        val achId = showingAchievementId ?: return
        val ach   = ALL_ACHIEVEMENTS.find { it.id == achId } ?: run {
            showingAchievementId = null
            return
        }

        // Animate
        if (achievementClosing) {
            achievementAnimProgress = (achievementAnimProgress - step).coerceAtLeast(0f)
            if (achievementAnimProgress == 0f) {
                showingAchievementId = null
                achievementClosing   = false
                return
            }
        } else {
            achievementAnimProgress = (achievementAnimProgress + step).coerceAtMost(1f)
        }

        val eased = 1f - (1f - achievementAnimProgress) * (1f - achievementAnimProgress)

        // Card dimensions
        val cardW = screenW * 0.75f
        val cardH = cardW * 1.2f
        val cardX = (screenW - cardW) / 2f
        val cardR = 28f

        // Slide down from top
        val targetY = (screenH - cardH) / 2f
        val startY  = -cardH - 50f
        val cardY   = startY + (targetY - startY) * eased

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)

            // Semi-transparent backdrop
            fill(c.overlay, (120 * eased).toInt())
            rect(0, 0, screenW, screenH)

            // Card background
            fill(c.backgroundCard)
            rect(cardX, cardY, cardW, cardH, cardR)

            // Accent top border
            fill(c.accent)
            rect(cardX + cardR, cardY, cardW - 2f * cardR, 4f)

            // "Achievement Unlocked!" label
            fill(c.accent)
            textFont("roboto", 12)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("🎉  ACHIEVEMENT UNLOCKED  🎉", screenW / 2f, cardY + 50f)

            // Large emoji
            textFont("emoji", 48)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            fill(c.textPrimary)
            text(ach.emoji, screenW / 2f, cardY + cardH * 0.35f)

            // Achievement name
            fill(c.textPrimary)
            textFont("roboto", 18)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text(ach.name, screenW / 2f, cardY + cardH * 0.55f)

            // Description
            fill(c.textSecondary)
            textFont("roboto", 12)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text(ach.description, screenW / 2f, cardY + cardH * 0.65f)

            // Requirement
            fill(c.textHint)
            textFont("roboto", 10)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text(ach.requirement, screenW / 2f, cardY + cardH * 0.73f)

            // Close button
            val btnW = cardW * 0.55f
            val btnH = 70f
            val btnX = (screenW - btnW) / 2f
            val btnY = cardY + cardH - btnH - 30f
            val btnR = btnH / 2f

            fill(c.accent)
            rect(btnX, btnY, btnW, btnH, btnR)
            fill(c.textOnAccent)
            textFont("roboto", 14)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Awesome!", screenW / 2f, btnY + btnH / 2f)

            // Handle close tap
            if (!achievementClosing && achievementAnimProgress >= 1f) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    // Tap on button
                    if (tx >= btnX && tx <= btnX + btnW && ty >= btnY && ty <= btnY + btnH) {
                        achievementClosing = true
                    }
                    // Tap outside card (backdrop dismiss)
                    if (tx < cardX || tx > cardX + cardW || ty < cardY || ty > cardY + cardH) {
                        achievementClosing = true
                    }
                }
            }
        }
    }
}