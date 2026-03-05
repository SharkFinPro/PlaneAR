package edu.osu.t22.planear.scenes.pages

import android.util.Log
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.Scene
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher

enum class SceneId(val id: Int) {
    Home(1),
    AR(2),
    FlightHistory(3),
    Settings(4)
}

val sceneIdMap = listOf(SceneId.Home, SceneId.AR, SceneId.FlightHistory, SceneId.Settings)
val navLabels = listOf("Home", "AR View", "History", "Settings")
val navEmojiLabels = listOf("🏠", "📷", "🕒", "⚙️")

interface Page : Scene {
    val sceneId: SceneId

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        drawNavButtons(sceneInfo, sceneSwitcher);
    }

    private fun drawNavButtons(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenWidth = sceneInfo.screenWidth;
        val screenHeight = sceneInfo.screenHeight;

        val buttonWidth = screenWidth / 4.0f
        val buttonHeight = 250.0f
        val buttonTop = screenHeight - buttonHeight

        val activeNavIndex = sceneId.ordinal;

        with (GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            // Navigation Background
            fill(255)
            rect(0f, buttonTop, screenWidth, buttonHeight)

            for (i in navLabels.indices) {
                val offsetX = i.toFloat() * buttonWidth

                // Active Button Background
                if (i == activeNavIndex) {
                    fill(76, 217, 100)
                    rect(offsetX, buttonTop, buttonWidth, buttonHeight)
                }

                // Button Text
                if (i == activeNavIndex) fill(255) else fill(100)

                val yOffset = 40.0f

                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                textFont("roboto", 42)
                text(
                    navLabels[i],
                    offsetX + buttonWidth / 2.0f,
                    screenHeight - buttonHeight / 2.0f + yOffset
                )

                textFont("emoji", 70)
                text(
                    navEmojiLabels[i],
                    offsetX + buttonWidth / 2.0f,
                    screenHeight - buttonHeight / 2.0f - yOffset
                )

                // Check for and handle button press
                if (sceneInfo.mouseX > offsetX &&
                    sceneInfo.mouseX < offsetX + buttonWidth &&
                    sceneInfo.mouseY > buttonTop &&
                    sceneInfo.mouseY < buttonTop + buttonHeight
                ) {
                    val targetId = sceneIdMap[i].id
                    sceneSwitcher.setCurrentScene(targetId)
                    Log.i("Page", "Button $i clicked! Switching to scene $targetId")
                }
            }

            // Small bar above buttons
            fill(100)
            rect(0, screenHeight - buttonHeight, screenWidth, 1)
        }
    }
}