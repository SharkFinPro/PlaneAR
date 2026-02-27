#include "AppScenes.h"
#include "ArBridge.h"
#include "SceneIds.h"
#include <Logger.h>
#include <source/components/assets/AssetManager.h>
#include <source/components/renderingManager/RenderingManager.h>
#include <source/components/renderingManager/renderer2D/Renderer2D.h>
#include <source/components/ui/Button.h>
#include <source/components/ui/Label.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <vector>
#include <memory>
#include <mutex>
#include <array>

// --- Global State for UI and Scenes ---
extern ArState gArState;
extern bool gArReady;

static int activeNavIndex = 0;
static std::vector<std::unique_ptr<ge::ui::Button>> navButtons;

// Mapping buttons to Scene IDs using the new Enum Class
static constexpr std::array<SceneId, 6> sceneIdMap = {
    SceneId::Home,
    SceneId::AR,
    SceneId::Kotlin,
    SceneId::Files,
    SceneId::Map,
    SceneId::Settings
};

// --- Private Helper Functions ---

void drawCommonUI(const SceneInfo& info, SceneSwitcher* switcher) {
  const auto r = info.engine->getRenderingManager()->getRenderer2D();
  
  uint32_t currentSceneId = switcher->getCurrentScene();
  for (size_t i = 0; i < sceneIdMap.size(); ++i) {
    if (static_cast<uint32_t>(sceneIdMap[i]) == currentSceneId) {
      activeNavIndex = static_cast<int>(i);
      break;
    }
  }

  // AR Status
  bool arReady = false;
  {
    std::lock_guard<std::mutex> lock(gArState.mtx);
    arReady = gArReady;
  }
  // ar dot top right
  if (arReady) r->fill(120, 255, 0, 220);
  else r->fill(255, 0, 0, 220);
  r->ellipse(1000, 20, 40, 40);

  // --- NEW: hardware buffer dot top left  ---
  long long hbCount = gHwBufferCount.load();
  if (hbCount > 0)
    r->fill(120, 255, 0, 220);     // green
  else
    r->fill(255, 0, 0, 220);     // red

    r->ellipse(50, 20, 40, 40);

  // Update and draw buttons
  for (int i = 0; i < navButtons.size(); ++i) {
    if (navButtons[i]->update(info.mouseX, info.mouseY, info.tapOccurred)) {
      SceneId targetId = sceneIdMap[i];
      switcher->setCurrentScene(static_cast<uint32_t>(targetId));
      LOGI("Button %d clicked! Switching to scene %u", i, static_cast<uint32_t>(targetId));
    }
    navButtons[i]->setActive(i == activeNavIndex);
    navButtons[i]->draw(r);
  }

  // Render cursor
  float cursorSize = 50.0f;
  r->fill(135, 22, 232);
  r->rect(info.mouseX - cursorSize / 2.0f, info.mouseY - cursorSize / 2.0f, cursorSize, cursorSize);
}

// --- Public Scene Definitions ---

namespace AppScenes {
    void initialize(const std::unique_ptr<ge::GraphicsEngine>& engine, struct android_app* pApp) {
        auto am = engine->getAssetManager();
        am->registerFont("roboto", "fonts/Roboto-VariableFont_wdth,wght.ttf");
        am->registerFont("emoji", "fonts/NotoEmoji-VariableFont_wght.ttf");
        am->registerImage("plane", "images/plane.jpg");

        if (navButtons.empty()) {
            float screenWidth = (float)ANativeWindow_getWidth(pApp->window);
            float screenHeight = (float)ANativeWindow_getHeight(pApp->window);
            float spacing = 80.0f;
            float navY = screenHeight - 150.0f;
            float dotSize = 80.0f;
            float totalNavWidth = (6 * dotSize) + (5 * spacing);
            float navStartX = (screenWidth - totalNavWidth) / 2.0f;

            const char* labels[] = {"Home", "AR", "Kot", "Files", "Map", "Set"};
            for (int i = 0; i < 6; ++i) {
                float dotX = navStartX + (i * (dotSize + spacing));
                navButtons.push_back(std::make_unique<ge::ui::Button>(labels[i], dotX, navY, dotSize, dotSize));
            }
        }
    }

    void homeScene(const SceneInfo& info, SceneSwitcher* switcher) {
        const auto r = info.engine->getRenderingManager()->getRenderer2D();
        r->fill(255, 255, 255);
        
        static ge::ui::Label title("PlaneAR", 100, 300, "roboto", 100);
        static ge::ui::Label subTitle("An AR Plane Tracking App", 100, 450, "roboto", 64);
        static ge::ui::Label emojis("✈🥳", 800, 200, "emoji", 150);

        title.draw(r);
        subTitle.draw(r);
        emojis.draw(r);

        r->image("plane", 350, 300, 600, 400);

        drawCommonUI(info, switcher);
    }

    void arScene(const SceneInfo& info, SceneSwitcher* switcher) {
        const auto r = info.engine->getRenderingManager()->getRenderer2D();
        static ge::ui::Label title("AR Scene", 100, 300, "roboto", 100);
        title.draw(r);
        drawCommonUI(info, switcher);
    }

    void filesScene(const SceneInfo& info, SceneSwitcher* switcher) {
        const auto r = info.engine->getRenderingManager()->getRenderer2D();
        static ge::ui::Label title("Files Scene", 100, 300, "roboto", 100);
        title.draw(r);
        drawCommonUI(info, switcher);
    }

    void mapScene(const SceneInfo& info, SceneSwitcher* switcher) {
        const auto r = info.engine->getRenderingManager()->getRenderer2D();
        static ge::ui::Label title("Map Scene", 100, 300, "roboto", 100);
        title.draw(r);
        drawCommonUI(info, switcher);
    }

    void settingsScene(const SceneInfo& info, SceneSwitcher* switcher) {
        const auto r = info.engine->getRenderingManager()->getRenderer2D();
        static ge::ui::Label title("Settings Scene", 100, 300, "roboto", 100);
        title.draw(r);
        drawCommonUI(info, switcher);
    }
} // namespace AppScenes
