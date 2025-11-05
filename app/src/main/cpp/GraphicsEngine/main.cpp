#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <vulkan/vulkan.h>
#include <stdexcept>

#include "Logger.h"
#include "GraphicsEngine.h"

void android_main(struct android_app *pApp)
{
  ge::GraphicsEngine engine;

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

      if (pApp->destroyRequested != 0)
      {
        LOGI("App destroy requested, exiting...");
        return;
      }
    }
  }
}
