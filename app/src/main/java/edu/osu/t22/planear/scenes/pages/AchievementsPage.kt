package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher

/**
 * Placeholder page for the Achievements feature.
 * Will eventually show daily streaks, badges, and stats for plane viewing.
 */
class AchievementsPage : Page {
    override val sceneId = SceneId.Achievements

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val width  = sceneInfo.screenWidth
        val height = sceneInfo.screenHeight - navHeight
        val c      = AppColors.current

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)
            fill(c.background)
            rect(0, 0, width, height)

            // Page title
            fill(c.textPrimary)
            textFont("roboto", 20)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Achievements", width / 2f, 200f)

            // Placeholder trophy icon
            textFont("emoji", 48)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("🏆", width / 2f, height * 0.4f)

            // Coming soon message
            fill(c.textSecondary)
            textFont("roboto", 14)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Coming Soon", width / 2f, height * 0.55f)

            fill(c.textHint)
            textFont("roboto", 12)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Daily streaks, badges, and more!", width / 2f, height * 0.60f)
        }

        postRender(sceneInfo, sceneSwitcher)
    }
}
