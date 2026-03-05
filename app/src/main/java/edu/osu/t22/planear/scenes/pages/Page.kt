package edu.osu.t22.planear.scenes.pages

import android.util.Log
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

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            // Dimmed backdrop
            rectMode(RectMode.CORNER)
            fill(0, 0, 0, 80)
            rect(0, 0, screenW, screenH)

            val widgetW = screenW * 0.82f
            val widgetH = 380.0f
            val widgetX = (screenW - widgetW) / 2.0f
            val widgetY = screenH * 0.30f

            // Widget body
            fill(76, 175, 80)
            rect(widgetX, widgetY, widgetW, widgetH)

            // Dark green header bar
            fill(56, 142, 60)
            rect(widgetX, widgetY, widgetW, 80.0f)

            // Callsign
            fill(255, 255, 255)
            textFont("roboto", 15)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text(flight.callsign, screenW / 2.0f, widgetY + 40.0f)

            // Close button
            val closeX    = widgetX + widgetW - 55.0f
            val closeY    = widgetY + 15.0f
            val closeSize = 50.0f
            fill(255, 255, 255, 200)
            rect(closeX, closeY, closeSize, closeSize)
            fill(56, 142, 60)
            textFont("roboto", 12)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("X", closeX + closeSize / 2.0f, closeY + closeSize / 2.0f)

            // Content rows
            val contentX = widgetX + 30.0f
            val rowStart = widgetY + 110.0f
            val rowGap   = 70.0f
            val halfW    = (widgetW - 70.0f) / 2.0f

            // Row 1: Takeoff / Landing
            fill(240, 248, 255)
            rect(contentX, rowStart, halfW, 55.0f)
            rect(contentX + halfW + 10.0f, rowStart, halfW, 55.0f)
            fill(30, 30, 30)
            textFont("roboto", 10)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Takeoff: ${flight.takeoffTime}", contentX + halfW / 2.0f, rowStart + 27.0f)
            text("Landing: ${flight.landingTime}", contentX + halfW * 1.5f + 10.0f, rowStart + 27.0f)

            // Row 2: Plane type
            val row2Y = rowStart + rowGap
            fill(240, 248, 255)
            rect(contentX, row2Y, widgetW - 60.0f, 55.0f)
            fill(30, 30, 30)
            textFont("roboto", 10)
            textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
            text("Plane Type: ${flight.planeType}", contentX + 15.0f, row2Y + 27.0f)

            // Row 3: Airspeed
            val row3Y = row2Y + rowGap
            fill(240, 248, 255)
            rect(contentX, row3Y, widgetW - 60.0f, 55.0f)
            fill(30, 30, 30)
            textFont("roboto", 10)
            textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
            text("Airspeed: ${flight.airspeed} kts", contentX + 15.0f, row3Y + 27.0f)

            // Dismiss: tap X or tap outside the widget
            if (sceneInfo.tapOccurred && !tapAlreadyConsumed) {
                val mx = sceneInfo.mouseX
                val my = sceneInfo.mouseY
                if ((mx >= closeX && mx <= closeX + closeSize && my >= closeY && my <= closeY + closeSize) ||
                    (mx < widgetX || mx > widgetX + widgetW || my < widgetY || my > widgetY + widgetH)) {
                    dismissed = true
                }
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