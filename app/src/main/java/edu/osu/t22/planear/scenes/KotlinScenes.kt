package edu.osu.t22.planear.scenes

import edu.osu.t22.planear.graphicsEngine.*

class Scene3 : Scene {
    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val width = sceneInfo.screenWidth;

        with (GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D())
        {
            // Light blue background
            fill(240, 248, 255);
            rect(0f, 0f, sceneInfo.screenWidth, sceneInfo.screenHeight);

            // Title
            fill(0, 0, 0);
            textFont("roboto", 100);
            text("PlaneAR", 100f, 300f);

            textSize(64);
            text("An AR Plane Tracking App", 100f, 450f);

            fill(100, 100, 200);
            rect(sceneInfo.screenWidth / 4, sceneInfo.screenHeight - 200, sceneInfo.screenWidth / 2, 150f);

            fill(255, 255, 255);
            textSize(70);
            text("Go to C++", sceneInfo.screenWidth / 3, sceneInfo.screenHeight - 200);

            rectMode(RectMode.CENTER);
            fill(255, 0, 0);
            rect(width / 2, 100, width / 3, 30);
            fill(100, 200, 100);
            ellipse(width / 2, 100, 50, 50);

            renderTapEffect(this, sceneInfo);
        }

        if (sceneInfo.tapOccurred &&
            sceneInfo.mouseX > sceneInfo.screenWidth / 4 &&
            sceneInfo.mouseX < sceneInfo.screenWidth / 4 + sceneInfo.screenWidth / 2 &&
            sceneInfo.mouseY > sceneInfo.screenHeight - 200 &&
            sceneInfo.mouseY < sceneInfo.screenHeight - 200 + 150) {
            sceneSwitcher.setCurrentScene(1);
        }
    }

    private fun renderTapEffect(r: Renderer2D, sceneInfo: SceneInfo) {
        with (r) {
            fill(255, 200, 0, 150)
            ellipse(sceneInfo.mouseX - 15f, sceneInfo.mouseY - 15f, 30f, 30f)
        }
    }
}