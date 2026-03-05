package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher

class ArPage : Page {
    override val sceneId = SceneId.AR

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        super.render(sceneInfo, sceneSwitcher)

        val width = sceneInfo.screenWidth;
        val height = sceneInfo.screenHeight - navHeight;

        with (GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            fill(245);
            rect(0, 0, width, height);

            fill(0);
            textFont("roboto", 64);
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER);
            text("AR Scene", width / 2, 250);
        }
    }
}