#include "AppScenes.h"
#include "ArBridge.h"
#include <Logger.h>
#include <source/components/assets/AssetManager.h>
#include <source/components/renderingManager/RenderingManager.h>
#include <source/components/renderingManager/renderer2D/Renderer2D.h>
#include <source/components/ui/Button.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <vector>
#include <memory>
#include <mutex>

// Global State for UI and Scenes
extern ArState gArState;
extern bool gArReady;

static int activeNavIndex = 0;
static std::vector<std::unique_ptr<ge::ui::Button>> navButtons;

void drawCommonUI(const SceneInfo& info, SceneSwitcher* switcher) {
  const auto r = info.engine->getRenderingManager()->getRenderer2D();

  bool arReady = false;
  {
    std::lock_guard<std::mutex> lock(gArState.mtx);
    arReady = gArReady;
  }
  if (arReady) r->fill(120, 255, 0, 220);
  else r->fill(255, 0, 0, 220);
  r->ellipse(1000, 20, 40, 40);

  for (int i = 0; i < navButtons.size(); ++i) {
    if (navButtons[i]->update(info.mouseX, info.mouseY, info.tapOccurred)) {
      activeNavIndex = i;
      switcher->setCurrentScene(i + 1);
      LOGI("Button %d clicked! Switching to scene %d", i, i + 1);
    }
    navButtons[i]->setActive(i == activeNavIndex);
    navButtons[i]->draw(*r);
  }

  float cursorSize = 50.0f;
  r->fill(135, 22, 232);
  r->rect(info.mouseX - cursorSize / 2.0f, info.mouseY - cursorSize / 2.0f, cursorSize, cursorSize);
}

namespace AppScenes {
  void initialize(const std::unique_ptr<ge::GraphicsEngine>& engine, struct android_app* pApp) {
    auto am = engine->getAssetManager();
    am->registerFont("roboto", "fonts/Roboto-VariableFont_wdth,wght.ttf");
    am->registerFont("emoji", "fonts/NotoEmoji-VariableFont_wght.ttf");
    am->registerImage("plane", "images/plane.jpg");

    if (navButtons.empty()) {
      float screenWidth = (float)ANativeWindow_getWidth(pApp->window);
      float screenHeight = (float)ANativeWindow_getHeight(pApp->window);
      float spacing = 100.0f;
      float navY = screenHeight - 150.0f;
      float dotSize = 80.0f;
      float totalNavWidth = (5 * dotSize) + (4 * spacing);
      float navStartX = (screenWidth - totalNavWidth) / 2.0f;

      const char* labels[] = {"Home", "AR", "Files", "Map", "Set"};
      for (int i = 0; i < 5; ++i) {
        float dotX = navStartX + (i * (dotSize + spacing));
        navButtons.push_back(std::make_unique<ge::ui::Button>(labels[i], dotX, navY, dotSize, dotSize));
      }
    }
  }

  void homeScene(const SceneInfo& info, SceneSwitcher* switcher) {
    const auto r = info.engine->getRenderingManager()->getRenderer2D();
    r->fill(255, 255, 255);

    r->textFont("emoji", 150);
    r->text("✈🥳", 800, 850);
    r->image("plane", 350, 900, 600, 400);
    r->textFont("roboto", 100);
    r->text("Home Scene", 100, 300);
    r->textSize(64);
    r->textFont("roboto", 100);
    r->text("An AR Plane Tracking App", 100, 450);
    drawCommonUI(info, switcher);
  }

  void arScene(const SceneInfo& info, SceneSwitcher* switcher) {
    const auto r = info.engine->getRenderingManager()->getRenderer2D();
    r->fill(255, 255, 255);
    r->textFont("roboto", 100);
    r->text("AR Scene", 100, 300);
    drawCommonUI(info, switcher);
  }

  void filesScene(const SceneInfo& info, SceneSwitcher* switcher) {
    const auto r = info.engine->getRenderingManager()->getRenderer2D();
    r->fill(255, 255, 255);
    r->textFont("roboto", 100);
    r->text("Files Scene", 100, 300);
    drawCommonUI(info, switcher);
  }

  void mapScene(const SceneInfo& info, SceneSwitcher* switcher) {
    const auto r = info.engine->getRenderingManager()->getRenderer2D();
    r->fill(255, 255, 255);
    r->textFont("roboto", 100);
    r->text("Map Scene", 100, 300);
    drawCommonUI(info, switcher);
  }

  void settingsScene(const SceneInfo& info, SceneSwitcher* switcher) {
    const auto r = info.engine->getRenderingManager()->getRenderer2D();
    r->fill(255, 255, 255);
    r->textFont("roboto", 100);
    r->text("Settings Scene", 100, 300);
    drawCommonUI(info, switcher);
  }
} // namespace AppScenes
