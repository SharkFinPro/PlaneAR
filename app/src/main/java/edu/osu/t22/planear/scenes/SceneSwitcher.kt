package edu.osu.t22.planear.scenes

import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.FrameGestureDetector
import edu.osu.t22.planear.scenes.pages.ArPage
import edu.osu.t22.planear.scenes.pages.FavoritesPage
import edu.osu.t22.planear.scenes.pages.FlightHistoryPage

import edu.osu.t22.planear.scenes.pages.SceneId
import edu.osu.t22.planear.scenes.pages.SettingsPage

interface Scene {
    fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher)
}

data class SceneInfo(
    val enginePtr: Long,
    val screenWidth: Float,
    val screenHeight: Float,
    val gestures: FrameGestureDetector
)

class SceneSwitcher {
    private val scenes = mutableMapOf<Int, Scene>()

    companion object {
        @JvmStatic
        private external fun nativeInit(sceneSwitcher: SceneSwitcher)

        @JvmStatic
        private external fun nativeRegisterSceneCallback(sceneId: Int)

        @JvmStatic
        private external fun nativeSetCurrentScene(sceneId: Int)

        @JvmStatic
        private external fun nativeCheckIfSceneExists(sceneId: Int): Boolean

        // This will be called from C++ via JNI
        @JvmStatic
        fun renderScene(sceneId: Int, enginePtr: Long, screenWidth: Float, screenHeight: Float) {
            instance?.renderSceneInternal(sceneId, enginePtr, screenWidth, screenHeight)
        }

        private var instance: SceneSwitcher? = null

        // gestureDetector must be set by MainActivity before any frame renders
        var gestureDetector: FrameGestureDetector? = null

        fun initialize(): SceneSwitcher {
            if (instance == null) {
                instance = SceneSwitcher()
                nativeInit(instance!!)
            }
            instance!!.registerScenes()
            return instance!!
        }
    }

    private fun registerScenes() {
        registerScene(SceneId.AR.id, ArPage())
        registerScene(SceneId.FlightHistory.id, FlightHistoryPage())
        registerScene(SceneId.Settings.id, SettingsPage())
        registerScene(SceneId.Favorites.id, FavoritesPage())
        setCurrentScene(SceneId.AR.id)
    }

    private fun registerScene(sceneId: Int, scene: Scene) {
        if (scenes.containsKey(sceneId)) return
        scenes[sceneId] = scene
        nativeRegisterSceneCallback(sceneId)
    }

    fun setCurrentScene(sceneId: Int) {
        if (!nativeCheckIfSceneExists(sceneId)) {
            throw IllegalArgumentException("Scene $sceneId not registered")
        }
        nativeSetCurrentScene(sceneId)
    }

    private fun renderSceneInternal(sceneId: Int, enginePtr: Long, screenWidth: Float, screenHeight: Float) {
        AppSettings.cameraIsEnabled = false

        val scene = scenes[sceneId] ?: return
        val detector = gestureDetector ?: return
        val sceneInfo = SceneInfo(enginePtr, screenWidth, screenHeight, detector)
        scene.render(sceneInfo, this)
        detector.reset()
    }
}