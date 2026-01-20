#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <vulkan/vulkan.h>
#include <stdexcept>
#include <memory>
#include "Logger.h"
#include "GraphicsEngine.h"
#include "components/renderingManager/RenderingManager.h"
#include "ArBridge.h"
extern ArState gArState;
extern bool gArReady;

static void handleTouchInput(struct android_app* pApp, float* mouseX, float* mouseY);

void android_main(struct android_app* pApp)
{
  std::unique_ptr<ge::GraphicsEngine> engine;

  int events;
  struct android_poll_source* source;

  float mouseX = 0;
  float mouseY = 0;

    // remove after testing
    static float x = 100;
    static float y = 100;
    static float w = 200;
    static float h = 100;


    while (true)
  {
    int result = ALooper_pollOnce(0, nullptr, &events, (void**)&source);

    if (result >= 0)
    {
      if (source != nullptr)
      {
        source->process(pApp, source);
      }

      handleTouchInput(pApp, &mouseX, &mouseY);

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
    //this is what i want to send the graphics engine has not been tested yet this will be next PR
   /*if (engine)
   {
    auto rm = engine->getRenderingManager();

    {
        std::lock_guard<std::mutex> lock(gArState.mtx);

        if (gArState.hasCamera)
            rm->setCameraMatrix(gArState.cameraMatrix);

        rm->setAnchors(gArState.anchors);
    }

    engine->render();
   }
   */
      //this is just to check that arcore is sending data not to be merged just use to test PR
      //start
      if (engine)
      {
          const auto renderingManager = engine->getRenderingManager();

          float r = 0, g = 0, b = 1; // Default BLUE when session is not created

          if (gArReady) {
              r = 0.0f;
              g = 1.0f;
              b = 0.0f;
          }

          // Top rectangle (status box)
          renderingManager->renderRect(x, y, w, h, r, g, b);

          // Other demo rectangles
          renderingManager->renderRect(x, y * 3.0f, w * 3.0f, h, 0, 1, 0);
          renderingManager->renderRect(x, y * 5.0f, w * 2.0f, h, 1, 0, 0);

          // Mouse cursor box
          float cursorSize = 50.0f;
          renderingManager->renderRect(
                  mouseX - cursorSize / 2.0f,
                  mouseY - cursorSize / 2.0f,
                  cursorSize, cursorSize,
                  0.529f, 0.086f, 0.91f);

          engine->render();
      }
      //end remove after testing PR
  }
}

static void handleTouchInput(struct android_app* pApp, float* mouseX, float* mouseY)
{
  android_input_buffer* inputBuffer = android_app_swap_input_buffers(pApp);
  if (!inputBuffer) return;

  for (uint64_t i = 0; i < inputBuffer->motionEventsCount; i++)
  {
    GameActivityMotionEvent* motionEvent = &inputBuffer->motionEvents[i];
    int action = motionEvent->action;
    int actionMasked = action & AMOTION_EVENT_ACTION_MASK;

    if (actionMasked == AMOTION_EVENT_ACTION_DOWN)
    {
      float x = GameActivityPointerAxes_getX(&motionEvent->pointers[0]);
      float y = GameActivityPointerAxes_getY(&motionEvent->pointers[0]);

      *mouseX = x;
      *mouseY = y;
    }
    else if (actionMasked == AMOTION_EVENT_ACTION_MOVE)
    {
      float x = GameActivityPointerAxes_getX(&motionEvent->pointers[0]);
      float y = GameActivityPointerAxes_getY(&motionEvent->pointers[0]);

      *mouseX = x;
      *mouseY = y;
    }
  }

  android_app_clear_motion_events(inputBuffer);
}