package edu.osu.t22.planear.scenes

import edu.osu.t22.planear.scenes.pages.ArPage
import edu.osu.t22.planear.scenes.pages.HomePage
import edu.osu.t22.planear.scenes.pages.SceneId
import edu.osu.t22.planear.scenes.pages.SettingsPage

interface Scene {
    fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher)
}

data class SceneInfo(
    val enginePtr: Long,
    val mouseX: Float,
    val mouseY: Float,
    val tapOccurred: Boolean,
    val screenWidth: Float,
    val screenHeight: Float,
    val isTouching: Boolean
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
        private external fun nativeCheckIfSceneExists(sceneId: Int) : Boolean

        // This will be called from C++ via JNI
        @JvmStatic
        fun renderScene(sceneId: Int, enginePtr: Long, mouseX: Float, mouseY: Float, tapOccurred: Boolean, screenWidth: Float, screenHeight: Float, isTouching: Boolean) {
            instance?.renderSceneInternal(sceneId, enginePtr, mouseX, mouseY, tapOccurred, screenWidth, screenHeight, isTouching)
        }

        private var instance: SceneSwitcher? = null

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
        registerScene(SceneId.Home.id, HomePage())
        registerScene(SceneId.AR.id, ArPage())
        registerScene(SceneId.Settings.id, SettingsPage())
    }

    private fun registerScene(sceneId: Int, scene: Scene) {
        if (scenes.containsKey(sceneId)) {
            return
        }

        scenes[sceneId] = scene
        nativeRegisterSceneCallback(sceneId)
    }

    fun setCurrentScene(sceneId: Int) {
        if (!nativeCheckIfSceneExists(sceneId)) {
            throw IllegalArgumentException("Scene $sceneId not registered")
        }
        nativeSetCurrentScene(sceneId)
    }

    private fun renderSceneInternal(sceneId: Int, enginePtr: Long, mouseX: Float, mouseY: Float, tapOccurred: Boolean, screenWidth: Float, screenHeight: Float, isTouching: Boolean) {
        val scene = scenes[sceneId] ?: return
        val sceneInfo = SceneInfo(enginePtr, mouseX, mouseY, tapOccurred, screenWidth, screenHeight, isTouching)
        scene.render(sceneInfo, this)
    }
}
