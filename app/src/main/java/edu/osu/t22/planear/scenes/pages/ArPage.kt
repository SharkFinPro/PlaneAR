package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher

class ArPage : Page {
    override val sceneId = SceneId.AR

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        super.render(sceneInfo, sceneSwitcher)

        with (GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            fill(0, 0, 255);
            rect(300, 100, 100, 100);
        }
    }
}