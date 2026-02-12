#include "ArBridge.h"
#include "SceneSwitcher.h"
#include <source/GraphicsEngine.h>
#include <Logger.h>
#include <source/components/assets/AssetManager.h>
#include <source/components/renderingManager/RenderingManager.h>
#include <source/components/renderingManager/renderer2D/Renderer2D.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <vulkan/vulkan.h>
#include <stdexcept>
#include <memory>

extern ArState gArState;
extern bool gArReady;

struct NavButton {
  float x, y, size;
};

static int activeNavIndex = 0;

static bool handleTouchInput(struct android_app* pApp, float* mouseX, float* mouseY);

void displayNavButtons(const SceneInfo& sceneInfo,
                       SceneSwitcher* sceneSwitcher);

void displayCursor(const SceneInfo& sceneInfo);

void scene1(const SceneInfo& sceneInfo,
            SceneSwitcher* sceneSwitcher);

void scene2(const SceneInfo& sceneInfo,
            SceneSwitcher* sceneSwitcher);

void android_main(struct android_app* pApp)
{
  SceneSwitcher sceneSwitcher;
  sceneSwitcher.loadScene(1, scene1);
  sceneSwitcher.loadScene(2, scene2);

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
        SceneInfo sceneInfo {
          .engine = engine,
          .pApp = pApp,
          .mouseX = mouseX,
          .mouseY = mouseY,
          .tapOccurred = tapOccurred
        };

        sceneSwitcher.renderCurrentScene(sceneInfo);
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

void displayNavButtons(const SceneInfo& sceneInfo,
                       SceneSwitcher* sceneSwitcher)
{
  const auto r = sceneInfo.engine->getRenderingManager()->getRenderer2D();

  float screenWidth = (float)ANativeWindow_getWidth(sceneInfo.pApp->window);
  float screenHeight = (float)ANativeWindow_getHeight(sceneInfo.pApp->window);
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
      r->fill(102, 178, 102);
    } else {
      r->fill(204, 204, 204);
    }
    r->rect(dotX, navY, dotSize, dotSize);
  }

  if (sceneInfo.tapOccurred) {
    for (int i = 0; i < 5; ++i) {
      const auto &button = navButtons[i];
      if (sceneInfo.mouseX >= button.x && sceneInfo.mouseX <= (button.x + button.size) &&
          sceneInfo.mouseY >= button.y && sceneInfo.mouseY <= (button.y + button.size)) {
        LOGI("Button %d clicked!", i);
        activeNavIndex = i;

        if (i == 0)
        {
          sceneSwitcher->setCurrentScene(1);
        }

        if (i == 1)
        {
          sceneSwitcher->setCurrentScene(2);
        }
        break;
      }
    }
  }
}

void displayCursor(const SceneInfo& sceneInfo)
{
  const auto r = sceneInfo.engine->getRenderingManager()->getRenderer2D();

  float cursorSize = 50.0f;
  r->fill(135, 22, 232);
  r->rect(sceneInfo.mouseX - cursorSize / 2.0f, sceneInfo.mouseY - cursorSize / 2.0f, cursorSize, cursorSize);
}

void scene1(const SceneInfo& sceneInfo,
            SceneSwitcher* sceneSwitcher)
{
  const auto r = sceneInfo.engine->getRenderingManager()->getRenderer2D();

  r->fill(255, 255, 255);

  r->textFont("roboto", 100);
  r->text("PlaneAR", 100, 300);

  r->textSize(64);
  r->text("An AR Plane Tracking App", 100, 450);

  bool arReady = false;
  {
    std::lock_guard<std::mutex> lock(gArState.mtx);

    arReady = gArReady;
  }

  if (arReady)
  {
    r->fill(120, 255, 0, 220);
  }
  else
  {
    r->fill(255, 0, 0, 220);
  }

  r->ellipse(1000, 20, 40, 40);

  displayNavButtons(sceneInfo, sceneSwitcher);

  displayCursor(sceneInfo);
}

void scene2(const SceneInfo& sceneInfo,
            SceneSwitcher* sceneSwitcher)
{
  const auto r = sceneInfo.engine->getRenderingManager()->getRenderer2D();

  r->fill(255, 255, 255);

  r->textFont("roboto", 100);
  r->text("PlaneAR", 100, 300);

  r->textSize(64);
  r->text("Scene 2", 100, 450);

  displayNavButtons(sceneInfo, sceneSwitcher);

  displayCursor(sceneInfo);
}