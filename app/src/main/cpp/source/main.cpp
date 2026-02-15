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
  SceneSwitcher switcher; // Using 'switcher' consistently

  // Register the switcher with the JNI bridge
  JNISceneBridge::setSceneSwitcher(&switcher);

  bool scenesLoaded = false; // Declaring the missing flag
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

        // Assets and UI are setup once here via AppScenes
        AppScenes::initialize(engine, pApp);

        if (!scenesLoaded) {
          switcher.loadScene(1, AppScenes::homeScene);
          switcher.loadScene(2, AppScenes::arScene);
          switcher.loadScene(3, AppScenes::filesScene);
          switcher.loadScene(4, AppScenes::mapScene);
          switcher.loadScene(5, AppScenes::settingsScene);
          switcher.setCurrentScene(1);
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
        // SceneSwitcher now handles createNewFrame and engine->render internally
        SceneInfo info{engine, pApp, mouseX, mouseY, tapOccurred};
        switcher.renderCurrentScene(info);
      }
    }
  }
}