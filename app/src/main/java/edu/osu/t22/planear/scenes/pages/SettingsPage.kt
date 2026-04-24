package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher

class SettingsPage : Page {
    override val sceneId = SceneId.Settings

    private var radiusSliderDragging = false

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val width    = sceneInfo.screenWidth
        val height   = sceneInfo.screenHeight - navHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)
            fill(c.background)
            rect(0, 0, width, height)

            // Page title
            fill(c.textPrimary)
            textFont("roboto", 20)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Settings", width / 2f, 200f)

            AppSettings.darkMode = drawToggleCard(
                sceneInfo = sceneInfo,
                cardX     = width * 0.05f,
                cardY     = 260f,
                cardW     = width * 0.90f,
                title     = "Dark Mode",
                enabled   = AppSettings.darkMode
            )

            AppSettings.canEnableCamera = drawToggleCard(
                sceneInfo = sceneInfo,
                cardX     = width * 0.05f,
                cardY     = 420f,
                cardW     = width * 0.90f,
                title     = "Use Camera",
                enabled   = AppSettings.canEnableCamera
            )

            // Radius slider card
            val newRadius = drawSlider(
                sceneInfo   = sceneInfo,
                cardX       = width * 0.05f,
                cardY       = 580f,
                cardW       = width * 0.90f,
                title       = "Aircraft Search Radius",
                min         = 1,
                max         = 50,
                current     = AppSettings.searchRadiusNm,
                units       = "nm",
                dragging    = radiusSliderDragging,
                onDragStart = { radiusSliderDragging = true },
                onDragEnd   = { radiusSliderDragging = false }
            )
            AppSettings.searchRadiusNm = newRadius
        }

        postRender(sceneInfo, sceneSwitcher)
    }

    private fun drawSlider(
        sceneInfo: SceneInfo,
        cardX: Float,
        cardY: Float,
        cardW: Float,
        title: String,
        min: Int,
        max: Int,
        current: Int,
        units: String,
        dragging: Boolean,
        onDragStart: () -> Unit,
        onDragEnd: () -> Unit
    ): Int {
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            val cardH   = 220f
            val cornerR = 20f

            // Card background
            fill(c.backgroundCard)
            rect(cardX, cardY, cardW, cardH, cornerR)

            // Title label
            fill(c.textPrimary)
            textFont("roboto", 14)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text(title, cardX + 30f, cardY + 55f)

            // Current value label
            fill(c.accent)
            textFont("roboto", 14)
            textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
            text("$current $units", cardX + cardW - 30f, cardY + 55f)

            // Track geometry
            val trackPad   = 40f
            val trackLeft  = cardX + trackPad
            val trackRight = cardX + cardW - trackPad
            val trackWidth = trackRight - trackLeft
            val trackY     = cardY + 130f
            val trackH     = 8f
            val fraction   = (current - min).toFloat() / (max - min).toFloat()
            val thumbX     = trackLeft + fraction * trackWidth

            // Track background
            fill(c.trackBackground)
            rect(trackLeft, trackY - trackH / 2f, trackWidth, trackH, trackH / 2f)

            // Filled portion up to thumb
            fill(c.accent)
            rect(trackLeft, trackY - trackH / 2f, thumbX - trackLeft, trackH, trackH / 2f)

            // Thumb outer circle
            val thumbR = 28f
            fill(c.accent)
            rect(thumbX - thumbR, trackY - thumbR, thumbR * 2f, thumbR * 2f, thumbR)

            // Thumb inner circle
            val innerR = 14f
            fill(c.textOnAccent)
            rect(thumbX - innerR, trackY - innerR, innerR * 2f, innerR * 2f, innerR)

            // Min / max labels
            fill(c.textHint)
            textFont("roboto", 11)
            textAlign(TextAlignH.LEFT, TextAlignV.TOP)
            text("$min $units", trackLeft, trackY + 20f)
            textAlign(TextAlignH.RIGHT, TextAlignV.TOP)
            text("$max $units", trackRight, trackY + 20f)

            // Hit area is taller than the track to make it easier to grab
            val hitTop    = trackY - 44f
            val hitBottom = trackY + 44f

            gestures.touchDownPosition?.let { (tx, ty) ->
                if (ty in hitTop..hitBottom && tx in trackLeft..trackRight) onDragStart()
            }

            if (!gestures.isTouching) onDragEnd()

            if (dragging && gestures.isTouching) {
                val fingerX = gestures.scrollPosition?.first
                    ?: gestures.singleTapPosition?.first
                if (fingerX != null) {
                    val clamped = fingerX.coerceIn(trackLeft, trackRight)
                    val newFrac = (clamped - trackLeft) / trackWidth
                    return (min + newFrac * (max - min)).toInt().coerceIn(min, max)
                }
            }

            return current
        }
    }

    private fun drawToggleCard(
        sceneInfo: SceneInfo,
        cardX: Float,
        cardY: Float,
        cardW: Float,
        title: String,
        enabled: Boolean
    ): Boolean {
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            val cardH   = 130f
            val cornerR = 20f

            // Card background
            fill(c.backgroundCard)
            rect(cardX, cardY, cardW, cardH, cornerR)

            // Label
            fill(c.textPrimary)
            textFont("roboto", 15)
            textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
            text(title, cardX + 30f, cardY + cardH / 2f)

            // Pill
            val pillW  = 110f
            val pillH  = 60f
            val pillR  = pillH / 2f
            val thumbR = pillR - 6f
            val pillX  = cardX + cardW - 30f - pillW
            val pillY  = cardY + (cardH - pillH) / 2f

            fill(if (enabled) c.accent else c.trackBackground)
            rect(pillX, pillY, pillW, pillH, pillR)

            fill(255, 255, 255)
            val thumbX = if (enabled) pillX + pillW - pillR else pillX + pillR
            rect(thumbX - thumbR, pillY + 6f, thumbR * 2f, thumbR * 2f, thumbR)

            // Tap anywhere on the card to toggle
            val tapped = gestures.singleTapUpPosition?.let { (tx, ty) ->
                tx in cardX..(cardX + cardW) && ty in cardY..(cardY + cardH)
            } ?: false

            return if (tapped) !enabled else enabled
        }
    }
}