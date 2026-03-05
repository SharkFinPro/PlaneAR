package edu.osu.t22.planear.scenes.pages

import android.util.Log
import edu.osu.t22.planear.graphicsEngine.EllipseMode
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.Scene
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher

enum class SceneId(val id: Int) {
    Home(1),
    AR(2),
    FlightHistory(3),
    Settings(4),
    Favorites(5)
}

val sceneIdMap = listOf(SceneId.Home, SceneId.AR, SceneId.FlightHistory, SceneId.Settings)
val navLabels = listOf("Home", "AR View", "History", "Settings")
val navEmojiLabels = listOf("🏠", "📷", "🕒", "⚙️")

interface Page : Scene {
    companion object {
        val flightFavorites: MutableList<Boolean> = MutableList(flightData.size) { false }

        // 0.0 = fully offscreen (sheet just opened), 1.0 = fully slid up.
        // Reset to 0f whenever a new flight is selected, then advances each frame.
        var sheetAnimProgress: Float = 0.0f
    }

    val sceneId: SceneId

    val navHeight: Float
        get() = 225.0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        drawNavButtons(sceneInfo, sceneSwitcher);
    }

    /**
     * Draws the flight detail popup overlay. Returns true if a dismiss tap was consumed,
     * so the caller can clear its selectedIndex / homeSelectedFlight state.
     */
    fun drawFlightDetailWidget(
        sceneInfo: SceneInfo,
        flight: FlightEntry,
        tapAlreadyConsumed: Boolean
    ): Boolean {
        val screenW = sceneInfo.screenWidth
        val screenH = sceneInfo.screenHeight - navHeight
        var dismissed = false

        // Advance animation each frame (step ≈ 0.06 → ~250 ms at 60 fps)
        sheetAnimProgress = (sheetAnimProgress + 0.05f).coerceAtMost(1.0f)

        // Ease out: fast start, settles smoothly at the top
        val eased = 1.0f - (1.0f - sheetAnimProgress) * (1.0f - sheetAnimProgress)

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)

            // Backdrop fades in with the sheet
            val backdropAlpha = (140 * eased).toInt()
            fill(0, 0, 0, backdropAlpha)
            rect(0, 0, screenW, screenH)

            // Bottom sheet — slides up from offscreen
            val sheetH      = screenH * 0.62f
            val sheetRestY  = screenH - sheetH
            val slideOffset = sheetH * (1.0f - eased)   // 0 when fully open
            val sheetY      = sheetRestY + slideOffset
            val sheetR      = 32.0f

            fill(255, 255, 255)
            rect(0, sheetY + sheetR, screenW, sheetH - sheetR)       // body (square bottom)
            rect(0, sheetY, screenW, sheetH * 0.4f, sheetR)          // top rounded portion

            // Drag handle pill
            fill(210, 215, 210)
            ellipseMode(EllipseMode.CENTER)
            ellipse(screenW / 2.0f, sheetY + 22.0f, 60.0f, 10.0f)

            // Callsign — big title
            fill(30, 30, 30)
            textFont("roboto", 26)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            val padX = screenW * 0.08f
            text(flight.callsign, padX, sheetY + 80.0f)

            // Thin accent line under callsign
            fill(76, 175, 80)
            rect(padX, sheetY + 92.0f, 60.0f, 4.0f, 2.0f)

            // Field rows — label left, value right, divider below
            val fieldStartY = sheetY + 140.0f
            val fieldGap    = 100.0f
            val labelSize   = 11
            val valueSize   = 17
            val rightEdge   = screenW - padX

            // Helper: draw one field row
            fun drawField(label: String, value: String, y: Float) {
                fill(150, 160, 150)
                textFont("roboto", labelSize)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(label, padX, y)

                fill(30, 30, 30)
                textFont("roboto", valueSize)
                textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
                text(value, rightEdge, y + 34.0f)

                // Divider
                fill(230, 235, 230)
                rect(padX, y + 52.0f, screenW - 2.0f * padX, 1.5f)
            }

            drawField("TAKEOFF",       flight.takeoffTime,         fieldStartY)
            drawField("LANDING",       flight.landingTime,         fieldStartY + fieldGap)
            drawField("AIRCRAFT TYPE", flight.planeType,           fieldStartY + fieldGap * 2)
            drawField("AIRSPEED",      "${flight.airspeed} kts",   fieldStartY + fieldGap * 3)

            // Close button — green pill at bottom
            val btnW   = screenW * 0.55f
            val btnH   = 72.0f
            val btnX   = (screenW - btnW) / 2.0f
            val btnY   = sheetY + sheetH - btnH - 28.0f
            fill(76, 175, 80)
            rect(btnX, btnY, btnW, btnH, btnH / 2.0f)
            fill(255, 255, 255)
            textFont("roboto", 15)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Close", screenW / 2.0f, btnY + btnH / 2.0f)

            // Dismiss: tap Close button or tap backdrop — only once fully open
            if (sceneInfo.tapOccurred && !tapAlreadyConsumed && sheetAnimProgress >= 1.0f) {
                val mx = sceneInfo.mouseX
                val my = sceneInfo.mouseY
                val tappedClose = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH
                val tappedBackdrop = my < sheetY
                if (tappedClose || tappedBackdrop) dismissed = true
            }
        }

        return dismissed
    }

    private fun drawNavButtons(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenWidth = sceneInfo.screenWidth;
        val screenHeight = sceneInfo.screenHeight;

        val buttonWidth = screenWidth / 4.0f
        val buttonTop = screenHeight - navHeight

        val activeNavIndex = sceneId.ordinal;

        with (GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            // Navigation Background
            fill(255)
            rect(0, buttonTop, screenWidth, navHeight)

            for (i in navLabels.indices) {
                val offsetX = i.toFloat() * buttonWidth

                // Active Button Background
                if (i == activeNavIndex) {
                    fill(76, 217, 100)
                    rect(offsetX, buttonTop, buttonWidth, navHeight)
                }

                // Button Text
                if (i == activeNavIndex) fill(255) else fill(100)

                val yOffset = 30.0f

                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                textFont("roboto", 12)
                text(
                    navLabels[i],
                    offsetX + buttonWidth / 2.0f,
                    screenHeight - navHeight / 2.0f + yOffset
                )

                textFont("emoji", 20)
                text(
                    navEmojiLabels[i],
                    offsetX + buttonWidth / 2.0f,
                    screenHeight - navHeight / 2.0f - yOffset
                )

                // Check for and handle button press
                if (sceneInfo.tapOccurred &&
                    sceneInfo.mouseX > offsetX &&
                    sceneInfo.mouseX < offsetX + buttonWidth &&
                    sceneInfo.mouseY > buttonTop &&
                    sceneInfo.mouseY < buttonTop + navHeight
                ) {
                    val targetId = sceneIdMap[i].id
                    sceneSwitcher.setCurrentScene(targetId)
                    Log.i("Page", "Button $i clicked! Switching to scene $targetId")
                }
            }

            // Small bar above buttons
            fill(100)
            rect(0, screenHeight - navHeight, screenWidth, 1)
        }
    }
}