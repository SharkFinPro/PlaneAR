#include "ArBridge.h"
#include "SceneSwitcher.h"
#include "AppScenes.h"
#include "InputUtils.h"
#include "JNISceneBridge.h"
#include "SceneIds.h"
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
//          switcher.loadScene(static_cast<uint32_t>(SceneId::Home),     AppScenes::homeScene);
          switcher.loadScene(static_cast<uint32_t>(SceneId::FlightHistory), AppScenes::flightHistoryScene);
          switcher.loadScene(static_cast<uint32_t>(SceneId::Favorites), AppScenes::favoritesScene);

//          switcher.setCurrentScene(static_cast<uint32_t>(SceneId::Home));
          switcher.setCurrentScene(static_cast<uint32_t>(SceneId::FlightHistory));

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
        SceneInfo info{engine, pApp, mouseX, mouseY, tapOccurred, InputUtils::isTouching()};
        switcher.renderCurrentScene(info);
      }
    }
  }
}
