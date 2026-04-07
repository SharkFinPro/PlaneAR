package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.adsb.AircraftOverlayStore
import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher

class ArPage : Page {
    override val sceneId = SceneId.AR

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenW = sceneInfo.screenWidth
        val screenH = sceneInfo.screenHeight - navHeight
        val c       = AppColors.current

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            rectMode(RectMode.CORNER)
            fill(c.background)
            rect(0, 0, screenW, screenH)

            fill(c.textPrimary)
            textFont("roboto", 18)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("AR Scene", screenW / 2, 250)
        }

        postRender(sceneInfo, sceneSwitcher)
    }
}