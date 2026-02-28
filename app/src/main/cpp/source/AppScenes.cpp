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
#include <string>
#include <cmath>
#include <algorithm>

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
    SceneId::FlightHistory,
    SceneId::Map,
    SceneId::Settings
};

// --- Mock Flight Data ---

struct FlightEntry {
  std::string callsign;
  std::string takeoffTime;
  std::string landingTime;
  std::string planeType;
  int airspeed;
  std::string date;
};

static const std::vector<FlightEntry> g_flightData = {
  {"AAL1023", "06:15 AM", "09:30 AM", "Boeing 737-800",  452, "02/28/2026"},
  {"UAL455",  "07:00 AM", "10:12 AM", "Airbus A320",     430, "02/28/2026"},
  {"DAL892",  "08:30 AM", "12:45 PM", "Boeing 767-300",  470, "02/28/2026"},
  {"SWA317",  "09:00 AM", "11:20 AM", "Boeing 737 MAX",  440, "02/28/2026"},
  {"JBU528",  "10:15 AM", "01:40 PM", "Airbus A321",     460, "02/28/2026"},
  {"FDX901",  "05:00 AM", "08:15 AM", "Boeing 777F",     490, "02/27/2026"},
  {"UPS234",  "04:30 AM", "07:50 AM", "Boeing 747-8F",   500, "02/27/2026"},
  {"AAL2045", "11:00 AM", "02:30 PM", "Airbus A319",     420, "02/27/2026"},
  {"UAL718",  "12:30 PM", "04:00 PM", "Boeing 787-9",    480, "02/27/2026"},
  {"DAL310",  "01:15 PM", "03:45 PM", "Airbus A330",     475, "02/27/2026"},
  {"NKS602",  "02:00 PM", "05:10 PM", "Airbus A320neo",  435, "02/26/2026"},
  {"AAL789",  "03:30 PM", "06:50 PM", "Boeing 757-200",  455, "02/26/2026"},
  {"SWA142",  "04:00 PM", "06:15 PM", "Boeing 737-700",  425, "02/26/2026"},
  {"JBU915",  "05:30 PM", "08:45 PM", "Airbus A321neo",  465, "02/26/2026"},
  {"UAL333",  "06:00 PM", "09:20 PM", "Boeing 737-900",  445, "02/26/2026"},
  {"DAL567",  "07:15 PM", "10:30 PM", "Airbus A350",     485, "02/25/2026"},
  {"FDX412",  "08:00 PM", "11:15 PM", "MD-11F",          470, "02/25/2026"},
  {"AAL1500", "06:45 AM", "10:00 AM", "Boeing 777-200",  495, "02/25/2026"},
  {"SWA800",  "07:30 AM", "09:45 AM", "Boeing 737-800",  440, "02/25/2026"},
  {"NKS101",  "08:15 AM", "11:30 AM", "Airbus A321",     450, "02/25/2026"},
  {"UAL922",  "09:45 AM", "01:00 PM", "Boeing 787-10",   480, "02/24/2026"},
  {"DAL215",  "10:30 AM", "01:45 PM", "Airbus A220",     415, "02/24/2026"},
  {"JBU347",  "11:15 AM", "02:30 PM", "Airbus A320",     430, "02/24/2026"},
  {"AAL650",  "12:00 PM", "03:15 PM", "Boeing 737 MAX",  445, "02/24/2026"},
  {"SWA999",  "01:30 PM", "03:50 PM", "Boeing 737-800",  435, "02/24/2026"},
  {"FDX780",  "02:15 PM", "05:30 PM", "Boeing 767-300F", 470, "02/23/2026"},
  {"UAL111",  "03:00 PM", "06:20 PM", "Boeing 757-300",  460, "02/23/2026"},
  {"DAL444",  "04:45 PM", "08:00 PM", "Airbus A330neo",  478, "02/23/2026"},
  {"NKS275",  "05:30 PM", "08:45 PM", "Airbus A320neo",  432, "02/23/2026"},
  {"AAL900",  "06:15 PM", "09:30 PM", "Boeing 777-300",  498, "02/23/2026"}
};

// --- Flight History Scene State ---
static int g_flightCurrentPage = 0;
static int g_flightSelectedIndex = -1; // -1 = no selection, widget hidden
static bool g_flightTapConsumed = false;
static constexpr int FLIGHTS_PER_PAGE = 14;

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
  if (arReady) r->fill(120, 255, 0, 220);
  else r->fill(255, 0, 0, 220);
  r->ellipse(1000, 20, 40, 40);

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
        am->registerFont("emoji", "fonts/NotoEmoji-VariableFont_wght.ttf", ge::CharsetMode::FULL);
        am->registerImage("plane", "images/plane.jpg");

        // Eagerly preload fonts at the sizes used by the app
        am->preloadFont("roboto", 34);
        am->preloadFont("roboto", 36);
        am->preloadFont("roboto", 38);
        am->preloadFont("roboto", 42);
        am->preloadFont("roboto", 52);
        am->preloadFont("roboto", 64);
        am->preloadFont("roboto", 100);
        am->preloadFont("emoji", 150);

        if (navButtons.empty()) {
            float screenWidth = (float)ANativeWindow_getWidth(pApp->window);
            float screenHeight = (float)ANativeWindow_getHeight(pApp->window);
            float spacing = 80.0f;
            float navY = screenHeight - 150.0f;
            float dotSize = 80.0f;
            float totalNavWidth = (6 * dotSize) + (5 * spacing);
            float navStartX = (screenWidth - totalNavWidth) / 2.0f;

            const char* labels[] = {"Home", "AR", "Kot", "Hist", "Map", "Set"};
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

    void flightHistoryScene(const SceneInfo& info, SceneSwitcher* switcher) {
        const auto r = info.engine->getRenderingManager()->getRenderer2D();
        float screenW = (float)ANativeWindow_getWidth(info.pApp->window);
        float screenH = (float)ANativeWindow_getHeight(info.pApp->window);

        g_flightTapConsumed = false;

        int totalFlights = static_cast<int>(g_flightData.size());
        int totalPages = ((totalFlights - 1) / FLIGHTS_PER_PAGE) + 1;
        g_flightCurrentPage = std::clamp(g_flightCurrentPage, 0, totalPages - 1);

        // ── Layout Constants ──
        float margin = screenW * 0.06f;
        float headerY = screenH * 0.06f;
        float titleY = headerY + 70.0f;
        float subtitleY = titleY + 45.0f;
        float listStartY = subtitleY + 50.0f;
        float listEndY = screenH - 200.0f;  // leave room for nav bar
        float rowHeight = (listEndY - listStartY) / FLIGHTS_PER_PAGE;

        // ── Background ──
        r->rectMode(ge::RectMode::CORNER);
        r->fill(245, 248, 250);
        r->rect(0, 0, screenW, screenH);

        // ── Header ──
        // "Back" button
        r->fill(76, 175, 80);
        r->textFont("roboto", 42);
        r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::BASELINE);
        r->text("Back", margin, titleY);

        // "Flight History" title
        r->fill(30, 30, 30);
        r->textFont("roboto", 64);
        r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::BASELINE);
        r->text("Flight History", screenW / 2.0f, titleY);

        // "Next" button
        r->fill(76, 175, 80);
        r->textFont("roboto", 42);
        r->textAlign(ge::TextAlignH::RIGHT, ge::TextAlignV::BASELINE);
        r->text("Next", screenW - margin, titleY);

        // Page indicator
        r->fill(120, 120, 120);
        r->textFont("roboto", 36);
        r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::BASELINE);
        r->text("Page " + std::to_string(g_flightCurrentPage + 1) + " / " + std::to_string(totalPages),
                screenW / 2.0f, subtitleY);

        // ── Handle Back/Next taps ──
        bool widgetShown = (g_flightSelectedIndex >= 0);
        if (info.tapOccurred && !widgetShown) {
            float btnTop = headerY;
            float btnBottom = headerY + 80.0f;
            if (info.mouseY >= btnTop && info.mouseY <= btnBottom) {
                if (info.mouseX < screenW * 0.25f && g_flightCurrentPage > 0) {
                    g_flightCurrentPage--;
                    g_flightTapConsumed = true;
                }
                if (info.mouseX > screenW * 0.75f && g_flightCurrentPage < totalPages - 1) {
                    g_flightCurrentPage++;
                    g_flightTapConsumed = true;
                }
            }
        }

        // ── Flight Rows ──
        int pageStart = g_flightCurrentPage * FLIGHTS_PER_PAGE;
        int pageEnd = std::min(pageStart + FLIGHTS_PER_PAGE, totalFlights);

        for (int i = pageStart; i < pageEnd; ++i) {
            int rowIdx = i - pageStart;
            float rowY = listStartY + rowIdx * rowHeight;
            float textY = rowY + rowHeight * 0.65f;
            float rightEdge = screenW - margin;
            float dotRadius = 16.0f;

            // Separator lines (top and bottom of each row)
            r->fill(200, 200, 200);
            r->rect(margin, rowY, screenW - 2.0f * margin, 2.0f);
            r->rect(margin, rowY + rowHeight - 2.0f, screenW - 2.0f * margin, 2.0f);

            // Callsign
            r->fill(50, 50, 50);
            r->textFont("roboto", 38);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::BASELINE);
            r->text(g_flightData[i].callsign, margin + 10.0f, textY);

            // Date
            r->fill(100, 100, 100);
            r->textFont("roboto", 34);
            r->textAlign(ge::TextAlignH::RIGHT, ge::TextAlignV::BASELINE);
            r->text(g_flightData[i].date, rightEdge - dotRadius * 3.0f, textY);

            // Green status dot
            r->fill(76, 175, 80);
            r->ellipseMode(ge::EllipseMode::CENTER);
            r->ellipse(rightEdge - dotRadius, rowY + rowHeight / 2.0f, dotRadius * 2.0f, dotRadius * 2.0f);

            // Row tap detection
            if (info.tapOccurred && !g_flightTapConsumed && !widgetShown) {
                if (info.mouseX >= margin && info.mouseX <= rightEdge &&
                    info.mouseY >= rowY && info.mouseY <= rowY + rowHeight) {
                    g_flightSelectedIndex = i;
                    g_flightTapConsumed = true;
                }
            }
        }


        // ── Detail Widget Overlay ──
        if (g_flightSelectedIndex >= 0 && g_flightSelectedIndex < totalFlights) {
            const auto& flight = g_flightData[g_flightSelectedIndex];

            // Semi-transparent backdrop
            r->fill(0, 0, 0, 80);
            r->rect(0, 0, screenW, screenH);

            // Widget dimensions
            float widgetW = screenW * 0.82f;
            float widgetH = 380.0f;
            float widgetX = (screenW - widgetW) / 2.0f;
            float widgetY = screenH * 0.30f;

            // Green background
            r->fill(76, 175, 80);
            r->rect(widgetX, widgetY, widgetW, widgetH);

            // Darker green header bar
            r->fill(56, 142, 60);
            r->rect(widgetX, widgetY, widgetW, 80.0f);

            // Callsign title
            r->fill(255, 255, 255);
            r->textFont("roboto", 52);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text(flight.callsign, screenW / 2.0f, widgetY + 40.0f);

            // Close "X" button
            float closeX = widgetX + widgetW - 55.0f;
            float closeY = widgetY + 15.0f;
            float closeSize = 50.0f;
            r->fill(255, 255, 255, 200);
            r->rect(closeX, closeY, closeSize, closeSize);
            r->fill(56, 142, 60);
            r->textFont("roboto", 42);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text("X", closeX + closeSize / 2.0f, closeY + closeSize / 2.0f);

            // Content rows
            float contentX = widgetX + 30.0f;
            float rowStart = widgetY + 110.0f;
            float rowGap = 70.0f;
            float halfW = (widgetW - 70.0f) / 2.0f;

            // Row 1: Takeoff / Landing
            r->fill(240, 248, 255);
            r->rect(contentX, rowStart, halfW, 55.0f);
            r->rect(contentX + halfW + 10.0f, rowStart, halfW, 55.0f);

            r->fill(30, 30, 30);
            r->textFont("roboto", 34);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text("Takeoff: " + flight.takeoffTime, contentX + halfW / 2.0f, rowStart + 27.0f);
            r->text("Landing: " + flight.landingTime, contentX + halfW * 1.5f + 10.0f, rowStart + 27.0f);

            // Row 2: Plane Type
            float row2Y = rowStart + rowGap;
            r->fill(240, 248, 255);
            r->rect(contentX, row2Y, widgetW - 60.0f, 55.0f);
            r->fill(30, 30, 30);
            r->textFont("roboto", 36);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::CENTER);
            r->text("Plane Type: " + flight.planeType, contentX + 15.0f, row2Y + 27.0f);

            // Row 3: Airspeed
            float row3Y = row2Y + rowGap;
            r->fill(240, 248, 255);
            r->rect(contentX, row3Y, widgetW - 60.0f, 55.0f);
            r->fill(30, 30, 30);
            r->textFont("roboto", 36);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::CENTER);
            r->text("Airspeed: " + std::to_string(flight.airspeed) + " kts", contentX + 15.0f, row3Y + 27.0f);

            // Handle taps on widget
            if (info.tapOccurred && !g_flightTapConsumed) {
                // Close button tap
                if (info.mouseX >= closeX && info.mouseX <= closeX + closeSize &&
                    info.mouseY >= closeY && info.mouseY <= closeY + closeSize) {
                    g_flightSelectedIndex = -1;
                    g_flightTapConsumed = true;
                }
                // Tap outside widget dismisses it
                else if (info.mouseX < widgetX || info.mouseX > widgetX + widgetW ||
                         info.mouseY < widgetY || info.mouseY > widgetY + widgetH) {
                    g_flightSelectedIndex = -1;
                    g_flightTapConsumed = true;
                }
            }
        }

        // ── Common Navbar ──
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
