package edu.osu.t22.planear.scenes.pages

import android.util.Log
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.adsb.AircraftOverlayStore
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher

class ArPage : Page {
    override val sceneId = SceneId.AR

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val width = sceneInfo.screenWidth
        val height = sceneInfo.screenHeight - navHeight
        val points = AircraftOverlayStore.points

        val hb = AppSettings.hb;

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            if (hb != null)
            {
                updateCameraBuffer(hb);
                Log.i("ARPage", "HB FOUND")
//                hb.close();
                AppSettings.hb = null;
            }

            fill(245)
            rect(0, 0, width, height)

            fill(0)
            textFont("roboto", 18)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("AR Scene", width / 2, 40)

            textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
            textFont("roboto", 14)

            for (point in points) {
                val x = point.x.toInt()
                val y = point.y.toInt()

                if (x >= 0 && x <= width && y >= 0 && y <= height) {
                    fill(0)
                    ellipse(x, y, 12, 12)

                    text(point.label, x + 14, y)
                }
            }
        }

        postRender(sceneInfo, sceneSwitcher)
    }
}