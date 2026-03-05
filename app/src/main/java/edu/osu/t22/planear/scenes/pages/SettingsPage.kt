package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher

class SettingsPage : Page {
    override val sceneId = SceneId.Settings

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        super.render(sceneInfo, sceneSwitcher)

        with (GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            fill(255, 0, 0);
            rect(300, 100, 100, 100);
        }
    }
}