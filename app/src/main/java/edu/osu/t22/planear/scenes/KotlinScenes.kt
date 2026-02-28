package edu.osu.t22.planear.scenes

import edu.osu.t22.planear.graphicsEngine.*

class Scene3 : Scene {
    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher, arCore: ARCoreProvider?) {
        val width = sceneInfo.screenWidth;
        val height = sceneInfo.screenHeight;

        with (GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D())
        {
            arCore?.updateCameraBuffer(this)

            rectMode(RectMode.CORNER);

            // Light blue background
            fill(240, 248, 255);
            rect(0, 0, width, height);

            // Title
            fill(0, 0, 0);
            textFont("roboto", 100);
            text("PlaneAR", 100, 300);

            textSize(64);
            text("An AR Plane Tracking App", 100, 450);

            fill(100, 100, 200);
            rectMode(RectMode.CENTER);
            rect(width / 2, height - 200, width / 2, 150);

            fill(255, 255, 255);
            textSize(70);
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER);
            text("Go to C++", width / 2, height - 200);

            renderTapEffect(this, sceneInfo);
        }

        if (sceneInfo.tapOccurred &&
            sceneInfo.mouseX > width / 4 &&
            sceneInfo.mouseX < width / 4 + width / 2 &&
            sceneInfo.mouseY > height - 200 &&
            sceneInfo.mouseY < height - 200 + 150) {
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