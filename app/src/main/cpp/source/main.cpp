#include <source/GraphicsEngine.h>
#include <Logger.h>
#include <source/components/assets/AssetManager.h>
#include <source/components/renderingManager/RenderingManager.h>
#include <source/components/renderingManager/renderer2D/Renderer2D.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <vulkan/vulkan.h>
#include <stdexcept>
#include <memory>

struct NavButton {
  float x, y, size;
};

static int activeNavIndex = 1;

static bool handleTouchInput(struct android_app* pApp, float* mouseX, float* mouseY);

void doRendering(const std::unique_ptr<ge::GraphicsEngine>& engine, struct android_app* pApp, float mouseX, float mouseY, bool tapOccurred);

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

      bool tapOccurred = handleTouchInput(pApp, &mouseX, &mouseY);

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

      if (engine)
      {
        doRendering(engine, pApp, mouseX, mouseY, tapOccurred);
      }
    }
  }
}

static bool handleTouchInput(struct android_app* pApp, float* mouseX, float* mouseY)
{
  android_input_buffer* inputBuffer = android_app_swap_input_buffers(pApp);
  if (!inputBuffer) return false;

  bool tapDetected = false;
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
      tapDetected = true;
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
  return tapDetected;
}

void doRendering(const std::unique_ptr<ge::GraphicsEngine>& engine, struct android_app* pApp, float mouseX, float mouseY, bool tapOccurred)
{
  const auto r = engine->getRenderingManager()->getRenderer2D();
  
  r->fill(255, 255, 255);

  r->textFont("roboto", 100);
  r->text("PlaneAR", 100, 300);

  r->textSize(64);
  r->text("An AR Plane Tracking App", 100, 450);

  float screenWidth = (float)ANativeWindow_getWidth(pApp->window);
  float screenHeight = (float)ANativeWindow_getHeight(pApp->window);
  float spacing = 100.0f;
  float navY = screenHeight - 150.0f;
  float dotSize = 80.0f;
  float totalNavWidth = (5 * dotSize) + (4 * spacing);
  float navStartX = (screenWidth - totalNavWidth) / 2.0f;
  NavButton navButtons[5];

  for (int i = 0; i < 5; ++i) {
    float dotX = navStartX + (i * (dotSize + spacing));
    navButtons[i] = {dotX, navY, dotSize};

    if (i == activeNavIndex) {
      r->fill(102, 178, 102); // Active color
    } else {
      r->fill(204, 204, 204); // Inactive color
    }
    r->rect(dotX, navY, dotSize, dotSize);
  }

  if (tapOccurred) {
    for (int i = 0; i < 5; ++i) {
      const auto &button = navButtons[i];
      if (mouseX >= button.x && mouseX <= (button.x + button.size) &&
          mouseY >= button.y && mouseY <= (button.y + button.size)) {
        LOGI("Button %d clicked!", i);
        activeNavIndex = i;
        break;
      }
    }
  }

  float cursorSize = 50.0f;
  r->fill(135, 22, 232);
  r->rect(mouseX - cursorSize / 2.0f, mouseY - cursorSize / 2.0f, cursorSize, cursorSize);

  engine->render();
}
