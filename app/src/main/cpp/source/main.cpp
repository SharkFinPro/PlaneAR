#include "SceneSwitcher.h"
#include "JNISceneBridge.h"
#include "SceneIds.h"
#include <source/GraphicsEngine.h>
#include <source/components/assets/AssetManager.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <memory>
#include "../GraphicsEngine/Logger.h"
#include <chrono>

void preloadAssets(const std::unique_ptr<ge::GraphicsEngine>& engine);

void android_main(struct android_app* pApp)
{
  std::unique_ptr<ge::GraphicsEngine> engine;
  SceneSwitcher switcher;
  bool engineReady = false;

  // Register the switcher with the JNI bridge
  JNISceneBridge::setSceneSwitcher(&switcher);

  int events;
  struct android_poll_source* source;

  float mouseX = 0;
  float mouseY = 0;

  using Clock = std::chrono::steady_clock;

  auto fpsStartTime = Clock::now();
  uint64_t frameCount = 0;

  while (true)
  {
    int result = ALooper_pollOnce(0, nullptr, &events, (void**)&source);

    if (result >= 0)
    {
      if (source != nullptr)
      {
        source->process(pApp, source);
      }

      if (pApp->window != nullptr && !engine)
      {
        engine = std::make_unique<ge::GraphicsEngine>(pApp);

        preloadAssets(engine);

        engineReady = true;
      }
      else if (pApp->window != nullptr && engine && !engineReady)
      {
        engine->resume(pApp->window);

        engineReady = true;
      }

      if (pApp->window == nullptr && engine && engineReady)
      {
        engine->suspend();

        engineReady = false;
      }
    }

    if (pApp->destroyRequested != 0)
    {
      return;
    }

    if (engine && engineReady)
    {
      SceneInfo info{engine, pApp};
      switcher.renderCurrentScene(info);

      ++frameCount;

      auto now = Clock::now();
      auto elapsedMs =
        std::chrono::duration_cast<std::chrono::milliseconds>(
          now - fpsStartTime).count();

      if (elapsedMs >= 5000)
      {
        double seconds = elapsedMs / 1000.0;
        double avgFps = static_cast<double>(frameCount) / seconds;

        LOGI("Average FPS: %.2f", avgFps);

        fpsStartTime = now;
        frameCount = 0;
      }
    }
  }
}

void preloadAssets(const std::unique_ptr<ge::GraphicsEngine>& engine)
{
  auto am = engine->getAssetManager();
  am->registerFont("roboto", "fonts/Roboto-VariableFont_wdth,wght.ttf");
  am->registerFont("emoji", "fonts/NotoEmoji-VariableFont_wght.ttf", ge::CharsetMode::FULL);
  am->registerImage("plane", "images/plane.jpg");

  am->preloadFont("roboto", 11);
  am->preloadFont("roboto", 12);
  am->preloadFont("roboto", 13);
  am->preloadFont("roboto", 14);
  am->preloadFont("roboto", 15);
  am->preloadFont("roboto", 16);
  am->preloadFont("roboto", 18);
  am->preloadFont("emoji", 20);
  am->preloadFont("emoji", 32);
}