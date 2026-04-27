package edu.osu.t22.planear.scenes.pages

import android.hardware.HardwareBuffer
import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.achievements.ALL_ACHIEVEMENTS
import edu.osu.t22.planear.achievements.AchievementStore
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

    // Achievement popup state
    private var showingAchievementId: String? = null
    private var achievementAnimProgress: Float = 0.0f
    private var achievementClosing: Boolean = false

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val width = sceneInfo.screenWidth
        val height = sceneInfo.screenHeight - navHeight

        // Mark that we are on the AR page — enables achievement tracking
        AchievementStore.isOnArPage = true

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