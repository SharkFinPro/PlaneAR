#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <vulkan/vulkan.h>
#include <stdexcept>
#include <memory>
#include "Logger.h"
#include "GraphicsEngine.h"
#include "components/renderingManager/RenderingManager.h"
#include "components/renderingManager/renderer2D/Renderer2D.h"

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
  static float x = 100;
  static float y = 100;
  static float w = 200;
  static float h = 100;

  r->pushMatrix();
    r->translate(400, 1200);
    r->rotate(45.0f);
    r->scale(3.0f, 0.8f);
    r->rect(-50, -50, 100, 100);
  r->popMatrix();

  r->fill(0, 0, 255);
  r->rect(x, y, w, h);

  r->fill(0, 255, 0);
  r->rect(x, y * 3.0f, w * 3.0f, h);

  r->fill(255, 0, 0);
  r->rect(x, y * 5.0f, w * 2.0f, h);

  float cursorSize = 50.0f;
  r->fill(135, 22, 232);
  r->rect(mouseX - cursorSize / 2.0f, mouseY - cursorSize / 2.0f, cursorSize, cursorSize);

  r->fill(255, 255, 255);
  r->text("Hello, world!", 100, 800);

  engine->render();
}