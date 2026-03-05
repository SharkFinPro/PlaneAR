package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.Renderer2D
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher

class SettingsPage : Page {
    override val sceneId = SceneId.Settings

    private var radiusSliderDragging = false

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        super.render(sceneInfo, sceneSwitcher)

        val width  = sceneInfo.screenWidth
        val height = sceneInfo.screenHeight - navHeight

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)
            fill(245)
            rect(0, 0, width, height)

            // Page title
            fill(30)
            textFont("roboto", 20)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Settings", width / 2f, 200f)

            // Aircraft search radius slider
            val newRadius = drawSlider(
                sceneInfo   = sceneInfo,
                cardX       = width * 0.05f,
                cardY       = 325f,
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

        postRender(sceneInfo, sceneSwitcher);
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
        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            val cardH = 220f
            val cornerR = 20f

            // Card background
            fill(255)
            rect(cardX, cardY, cardW, cardH, cornerR)

            // Title label
            fill(30)
            textFont("roboto", 14)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text(title, cardX + 30f, cardY + 55f)

            // Current value label
            fill(76, 175, 80)
            textFont("roboto", 14)
            textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
            text("$current $units", cardX + cardW - 30f, cardY + 55f)

            // Track geometry
            val trackPad = 40f
            val trackLeft = cardX + trackPad
            val trackRight = cardX + cardW - trackPad
            val trackWidth = trackRight - trackLeft
            val trackY = cardY + 130f
            val trackH = 8f

            val fraction = (current - min).toFloat() / (max - min).toFloat()
            val thumbX = trackLeft + fraction * trackWidth

            // Track background
            fill(220)
            rect(trackLeft, trackY - trackH / 2f, trackWidth, trackH, trackH / 2f)

            // Filled portion up to thumb
            fill(76, 175, 80)
            rect(trackLeft, trackY - trackH / 2f, thumbX - trackLeft, trackH, trackH / 2f)

            // Thumb outer circle
            val thumbR = 28f
            fill(76, 175, 80)
            rect(thumbX - thumbR, trackY - thumbR, thumbR * 2f, thumbR * 2f, thumbR)

            // Thumb inner circle
            val innerR = 14f
            fill(255)
            rect(thumbX - innerR, trackY - innerR, innerR * 2f, innerR * 2f, innerR)

            // Min / max labels
            fill(150)
            textFont("roboto", 10)
            textAlign(TextAlignH.LEFT, TextAlignV.TOP)
            text("$min $units", trackLeft, trackY + 20f)
            textAlign(TextAlignH.RIGHT, TextAlignV.TOP)
            text("$max $units", trackRight, trackY + 20f)

            // Hit area is taller than the track to make it easier to grab
            val hitTop = trackY - 44f
            val hitBottom = trackY + 44f

            if (sceneInfo.tapOccurred &&
                sceneInfo.mouseY in hitTop..hitBottom &&
                sceneInfo.mouseX in trackLeft..trackRight
            ) {
                onDragStart()
            }

            if (!sceneInfo.isTouching) {
                onDragEnd()
            }

            if (dragging && sceneInfo.isTouching) {
                val clamped = sceneInfo.mouseX.coerceIn(trackLeft, trackRight)
                val newFrac = (clamped - trackLeft) / trackWidth
                return (min + newFrac * (max - min)).toInt().coerceIn(min, max)
            }

            return current
        }
    }
}