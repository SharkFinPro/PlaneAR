#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <vulkan/vulkan.h>
#include <stdexcept>
#include <memory>
#include "Logger.h"
#include "GraphicsEngine.h"
#include "components/renderingManager/RenderingManager.h"

void android_main(struct android_app* pApp)
{
  std::unique_ptr<ge::GraphicsEngine> engine;

  int events;
  struct android_poll_source* source;

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
        LOGI("Window initialized, creating graphics engine...");
        engine = std::make_unique<ge::GraphicsEngine>(pApp);
      }

      if (pApp->window == nullptr && engine)
      {
        LOGI("Window lost, destroying graphics engine...");
        engine.reset();
      }

      if (pApp->destroyRequested != 0)
      {
        LOGI("App destroy requested, exiting...");
        return;
      }
    }

    if (engine)
    {
      const auto renderingManger = engine->getRenderingManager();
      static float x = 100;
      static float y = 100;
      static float w = 200;
      static float h = 100;

      renderingManger->renderRect(x, y, w, h, 0, 0, 1);

      renderingManger->renderRect(x, y * 3, w, h, 0, 1, 0);

      renderingManger->renderRect(x, y * 5, w, h, 1, 0, 0);

      engine->render();
    }
  }
}
