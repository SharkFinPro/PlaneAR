package edu.osu.t22.planear

class Scene3 : Scene {
    override fun render(sceneInfo: SceneInfo) {
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

        renderTapEffect(r, sceneInfo)
    }

    private fun renderTapEffect(r: Renderer2D, sceneInfo: SceneInfo) {
        r.fill(255, 200, 0, 150)
        r.ellipse(sceneInfo.mouseX - 15f, sceneInfo.mouseY - 15f, 30f, 30f)
    }
}