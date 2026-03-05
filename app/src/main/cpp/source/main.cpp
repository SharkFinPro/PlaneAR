#include "ArBridge.h"
#include "SceneSwitcher.h"
#include "InputUtils.h"
#include "JNISceneBridge.h"
#include "SceneIds.h"
#include <source/GraphicsEngine.h>
#include <source/components/assets/AssetManager.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <memory>

void preloadAssets(const std::unique_ptr<ge::GraphicsEngine>& engine);

void android_main(struct android_app* pApp)
{
  std::unique_ptr<ge::GraphicsEngine> engine;
  SceneSwitcher switcher;

  // Register the switcher with the JNI bridge
  JNISceneBridge::setSceneSwitcher(&switcher);

  int events;
  struct android_poll_source* source;

  float mouseX = 0;
  float mouseY = 0;

  while (true)
  {
    int result = ALooper_pollOnce(0, nullptr, &events, (void**)&source);

    bool tapOccurred = false;
    if (result >= 0)
    {
      if (source != nullptr)
      {
        source->process(pApp, source);
      }

      // Input processing
      tapOccurred = InputUtils::handleTouchInput(pApp, &mouseX, &mouseY);

      if (pApp->window != nullptr && !engine)
      {
        engine = std::make_unique<ge::GraphicsEngine>(pApp);

        preloadAssets(engine);
      }

      if (pApp->window == nullptr && engine)
      {
        engine.reset();
      }

      if (pApp->destroyRequested != 0)
      {
        return;
      }
    }

    if (engine)
    {
      SceneInfo info{engine, pApp, mouseX, mouseY, tapOccurred, InputUtils::isTouching()};
      switcher.renderCurrentScene(info);
    }
  }
}

void preloadAssets(const std::unique_ptr<ge::GraphicsEngine>& engine)
{
  auto am = engine->getAssetManager();
  am->registerFont("roboto", "fonts/Roboto-VariableFont_wdth,wght.ttf");
  am->registerFont("emoji", "fonts/NotoEmoji-VariableFont_wght.ttf", ge::CharsetMode::FULL);
  am->registerImage("plane", "images/plane.jpg");

  am->preloadFont("roboto", 9);
  am->preloadFont("roboto", 10);
  am->preloadFont("roboto", 11);
  am->preloadFont("roboto", 12);
  am->preloadFont("roboto", 14);
  am->preloadFont("roboto", 15);
  am->preloadFont("roboto", 18);
  am->preloadFont("emoji", 20);
}