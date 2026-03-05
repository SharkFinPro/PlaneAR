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

// Returned by drawFlightDetailWidget each frame so callers know what to do.
enum class SheetResult {
    ANIMATING,   // still opening or closing - keep calling, don't clear selection yet
    OPEN,        // fully open and idle
    DISMISSED    // close animation finished - caller should clear its selected index
}

interface Page : Scene {
    companion object {
        val flightFavorites: MutableList<Boolean> = MutableList(flightData.size) { false }

        // Drives both the open and close animations.
        // 0.0 = fully offscreen, 1.0 = fully open.
        // Counts up while opening, counts down while closing.
        var sheetAnimProgress: Float = 0.0f
        var sheetClosing: Boolean = false
    }

    val sceneId: SceneId

    val navHeight: Float
        get() = 225.0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        // Nav is drawn in postRender() so it always appears on top of page content and overlays.
        // Each page must call postRender() as the last line of its render().
    }

    fun postRender(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        drawNavButtons(sceneInfo, sceneSwitcher)
    }

    // Draws the flight detail bottom sheet overlay.
    // Returns SheetResult so the caller knows whether to keep rendering, stay idle, or clear its selection.
    // Call Page.sheetAnimProgress = 0f and Page.sheetClosing = false before showing for the first time.
    fun drawFlightDetailWidget(
        sceneInfo: SceneInfo,
        flight: FlightEntry,
        tapAlreadyConsumed: Boolean
    ): SheetResult {
        val screenW = sceneInfo.screenWidth
        val screenH = sceneInfo.screenHeight
        val navTop  = screenH - navHeight
        val step    = 0.06f

        // Advance progress in whichever direction we're animating
        if (sheetClosing) {
            sheetAnimProgress = (sheetAnimProgress - step).coerceAtLeast(0.0f)
            if (sheetAnimProgress == 0.0f) return SheetResult.DISMISSED
        } else {
            sheetAnimProgress = (sheetAnimProgress + step).coerceAtMost(1.0f)
        }

        // Ease out: fast start, settles smoothly
        val eased = 1.0f - (1.0f - sheetAnimProgress) * (1.0f - sheetAnimProgress)

        // Fixed sheet dimensions - always drawn as if fully open
        val sheetH = navTop * 0.62f
        val sheetY = navTop - sheetH   // resting position (fully open, y=0 origin)

        // Slide offset: 0 when open, sheetH when fully hidden below navTop
        val slideOffset = sheetH * (1.0f - eased)

        val padX      = screenW * 0.08f
        val rightEdge = screenW - padX
        val sheetR    = 32.0f

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)

            // Backdrop - no transform needed
            val backdropAlpha = (140 * eased).toInt()
            fill(0, 0, 0, backdropAlpha)
            rect(0, 0, screenW, navTop)

            // Translate down by slideOffset so the sheet slides up from navTop
            pushMatrix()
            translate(0, slideOffset)

            // Sheet background
            fill(255, 255, 255)
            rect(0, sheetY + sheetR, screenW, sheetH - sheetR)
            rect(0, sheetY, screenW, sheetH * 0.4f, sheetR)

            // Drag handle pill
            fill(210, 215, 210)
            ellipseMode(EllipseMode.CENTER)
            ellipse(screenW / 2.0f, sheetY + 22.0f, 60.0f, 10.0f)

            // Callsign
            fill(30, 30, 30)
            textFont("roboto", 26)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text(flight.callsign, padX, sheetY + 100.0f)

            // Accent line under callsign
            fill(76, 175, 80)
            rect(padX, sheetY + 112.0f, 60.0f, 4.0f, 2.0f)

            // Field rows
            val fieldStartY = sheetY + 200.0f
            val fieldGap    = 100.0f

            fun drawField(label: String, value: String, y: Float) {
                fill(150, 160, 150)
                textFont("roboto", 11)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(label, padX, y)
                fill(30, 30, 30)
                textFont("roboto", 17)
                textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
                text(value, rightEdge, y + 34.0f)
                fill(230, 235, 230)
                rect(padX, y + 52.0f, screenW - 2.0f * padX, 1.5f)
            }

            drawField("TAKEOFF",       flight.takeoffTime,       fieldStartY)
            drawField("LANDING",       flight.landingTime,       fieldStartY + fieldGap)
            drawField("AIRCRAFT TYPE", flight.planeType,         fieldStartY + fieldGap * 2)
            drawField("AIRSPEED",      "${flight.airspeed} kts", fieldStartY + fieldGap * 3)

            // Close button
            val btnW = screenW * 0.55f
            val btnH = 72.0f
            val btnX = (screenW - btnW) / 2.0f
            val btnY = navTop - btnH - 28.0f
            fill(76, 175, 80)
            rect(btnX, btnY, btnW, btnH, btnH / 2.0f)
            fill(255, 255, 255)
            textFont("roboto", 15)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Close", screenW / 2.0f, btnY + btnH / 2.0f)

            popMatrix()

            // Tap checks use unadjusted coords - offset by slideOffset to match translated positions
            if (sceneInfo.tapOccurred && !tapAlreadyConsumed && !sheetClosing && sheetAnimProgress >= 1.0f) {
                val mx = sceneInfo.mouseX
                val my = sceneInfo.mouseY + slideOffset
                val tappedClose    = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH
                val tappedBackdrop = my < sheetY
                if (tappedClose || tappedBackdrop) sheetClosing = true
            }
        }

        return if (sheetAnimProgress >= 1.0f && !sheetClosing) SheetResult.OPEN else SheetResult.ANIMATING
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