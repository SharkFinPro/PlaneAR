package edu.osu.t22.planear.scenes

import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.Renderer2D

class Scene3 : Scene {
    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val engine = GraphicsEngineWrapper(sceneInfo.enginePtr)
        val r = engine.getRenderer2D()

        // Light blue background
        r.fill(240, 248, 255)
        r.rect(0f, 0f, sceneInfo.screenWidth, sceneInfo.screenHeight)

        // Title
        r.fill(0, 0, 0)
        r.textFont("roboto", 100)
        r.text("PlaneAR", 100f, 300f)

        r.textSize(64)
        r.text("An AR Plane Tracking App", 100f, 450f)

        r.fill(100, 100, 200);
        r.rect(sceneInfo.screenWidth / 4, sceneInfo.screenHeight - 200, sceneInfo.screenWidth / 2, 150f);

        r.fill(255, 255, 255);
        r.textSize(70);
        r.text("Go to C++", sceneInfo.screenWidth / 3, sceneInfo.screenHeight - 200);

        if (sceneInfo.tapOccurred &&
            sceneInfo.mouseX > sceneInfo.screenWidth / 4 &&
            sceneInfo.mouseX < sceneInfo.screenWidth / 4 + sceneInfo.screenWidth / 2 &&
            sceneInfo.mouseY > sceneInfo.screenHeight - 200 &&
            sceneInfo.mouseY < sceneInfo.screenHeight - 200 + 150) {
            sceneSwitcher.setCurrentScene(1);
        }

        renderTapEffect(r, sceneInfo)
    }

    private fun renderTapEffect(r: Renderer2D, sceneInfo: SceneInfo) {
        r.fill(255, 200, 0, 150)
        r.ellipse(sceneInfo.mouseX - 15f, sceneInfo.mouseY - 15f, 30f, 30f)
    }
}