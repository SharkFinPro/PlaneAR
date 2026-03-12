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

            // Dark Mode toggle card
            val toggleCardX = width * 0.05f
            val toggleCardY = 260f
            val toggleCardW = width * 0.90f
            val toggleCardH = 130f
            val toggleCornerR = 20f

            fill(c.backgroundCard)
            rect(toggleCardX, toggleCardY, toggleCardW, toggleCardH, toggleCornerR)

            fill(c.textPrimary)
            textFont("roboto", 15)
            textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
            text("Dark Mode", toggleCardX + 30f, toggleCardY + toggleCardH / 2f)

            // Toggle pill
            val pillW     = 110f
            val pillH     = 60f
            val pillX     = toggleCardX + toggleCardW - 30f - pillW
            val pillY     = toggleCardY + (toggleCardH - pillH) / 2f
            val pillR     = pillH / 2f
            val thumbR    = pillH / 2f - 6f
            val thumbOnX  = pillX + pillW - pillR
            val thumbOffX = pillX + pillR

            if (AppSettings.darkMode) {
                fill(c.accent)
            } else {
                fill(c.trackBackground)
            }
            rect(pillX, pillY, pillW, pillH, pillR)

            fill(255, 255, 255)
            val thumbX = if (AppSettings.darkMode) thumbOnX else thumbOffX
            rect(thumbX - thumbR, pillY + 6f, thumbR * 2f, thumbR * 2f, thumbR)

            // Tap anywhere on the card to toggle
            gestures.singleTapUpPosition?.let { (tx, ty) ->
                if (tx >= toggleCardX && tx <= toggleCardX + toggleCardW &&
                    ty >= toggleCardY && ty <= toggleCardY + toggleCardH) {
                    AppSettings.darkMode = !AppSettings.darkMode
                }
            }

            // Radius slider card
            val newRadius = drawSlider(
                sceneInfo   = sceneInfo,
                cardX       = width * 0.05f,
                cardY       = 420f,
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
}