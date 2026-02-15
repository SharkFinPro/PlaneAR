#include "ArBridge.h"
#include "SceneSwitcher.h"
#include "AppScenes.h"
#include "InputUtils.h"
#include "JNISceneBridge.h"
#include <source/GraphicsEngine.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <memory>

void android_main(struct android_app* pApp)
{
  std::unique_ptr<ge::GraphicsEngine> engine;
  SceneSwitcher switcher;

  // Register the switcher with the JNI bridge
  JNISceneBridge::setSceneSwitcher(&switcher);

  bool scenesLoaded = false;
  int events;
  struct android_poll_source* source;

  float mouseX = 0;
  float mouseY = 0;

  while (true)
  {
    int result = ALooper_pollOnce(0, nullptr, &events, (void**)&source);

    if (result >= 0)
    {
      if (source != nullptr)
      {
        source->process(pApp, source);
      }

      // Input processing
      bool tapOccurred = InputUtils::handleTouchInput(pApp, &mouseX, &mouseY);

      if (pApp->window != nullptr && !engine)
      {
        engine = std::make_unique<ge::GraphicsEngine>(pApp);

        // Assets and UI setup
        AppScenes::initialize(engine, pApp);

        if (!scenesLoaded) {
          // IDs 1, 2, 4, 5, 6 (Leaving 3 for Kotlin Scene3)
          switcher.loadScene(1, AppScenes::homeScene);
          switcher.loadScene(2, AppScenes::arScene);
          switcher.loadScene(4, AppScenes::filesScene);
          switcher.loadScene(5, AppScenes::mapScene);
          switcher.loadScene(6, AppScenes::settingsScene);
          
          scenesLoaded = true;
        }
      }

      if (pApp->window == nullptr && engine)
      {
        engine.reset();
      }

      if (pApp->destroyRequested != 0)
      {
        return;
      }

      if (engine)
      {
        SceneInfo info{engine, pApp, mouseX, mouseY, tapOccurred};
        switcher.renderCurrentScene(info);
      }
    }
  }
}
