#include <source/GraphicsEngine.h>
#include <Logger.h>
#include <source/components/assets/AssetManager.h>
#include <source/components/renderingManager/RenderingManager.h>
#include <source/components/renderingManager/renderer2D/Renderer2D.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <vulkan/vulkan.h>
#include <stdexcept>
#include <memory>

static void handleTouchInput(struct android_app* pApp, float* mouseX, float* mouseY);

void doRendering(const std::unique_ptr<ge::GraphicsEngine>& engine, float mouseX, float mouseY);

void android_main(struct android_app* pApp)
{
  std::unique_ptr<ge::GraphicsEngine> engine;

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

      handleTouchInput(pApp, &mouseX, &mouseY);

      if (pApp->window != nullptr && !engine)
      {
        LOGI("Window initialized, creating graphics engine...");
        engine = std::make_unique<ge::GraphicsEngine>(pApp);

        engine->getAssetManager()->registerFont("roboto", "fonts/Roboto-VariableFont_wdth,wght.ttf");
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
      doRendering(engine, mouseX, mouseY);
    }
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

void doRendering(const std::unique_ptr<ge::GraphicsEngine>& engine, float mouseX, float mouseY)
{
  const auto r = engine->getRenderingManager()->getRenderer2D();

  r->fill(100, 100, 200);
  r->rect(100, 100, 100, 100);

  r->fill(255, 255, 255);

  r->textFont("roboto", 100);
  r->text("PlaneAR", 400, 300);

  r->textSize(64);
  r->text("An AR Plane Tracking App", 400, 450);

  engine->render();
}