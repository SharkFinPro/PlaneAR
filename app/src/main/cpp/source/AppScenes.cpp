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

// Mapping buttons to Scene IDs using the new Enum Class
static constexpr std::array<SceneId, 4> sceneIdMap = {
    SceneId::Home,
    SceneId::AR,
    SceneId::FlightHistory,
    SceneId::Settings
};

constexpr std::array<std::string, 4> navLabels { "Home", "AR View", "History", "Settings" };
constexpr std::array<std::string, 4> navEmojiLabels { "🏠", "📷", "🕒", "⚙️" };

//Mock Flight Data
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

// --- Favorites State ---
static std::vector<bool> g_flightFavorites(30, false);
static int g_favCurrentPage = 0;
static int g_favSelectedIndex = -1;
static bool g_favTapConsumed = false;

// --- Private Helper Functions ---
void drawNavButtons(const SceneInfo& info, SceneSwitcher* switcher) {
  const auto r = info.engine->getRenderingManager()->getRenderer2D();

  auto screenWidth = static_cast<float>(ANativeWindow_getWidth(info.pApp->window));
  auto screenHeight = static_cast<float>(ANativeWindow_getHeight(info.pApp->window));

  auto buttonWidth = screenWidth / 4.0f;
  float buttonHeight = 250.0f;

  float buttonTop = screenHeight - buttonHeight;

  // Navigation Background
  r->fill(255);
  r->rect(0, buttonTop, screenWidth, buttonHeight);

  for (int i = 0; i < navLabels.size(); ++i) {
    float offsetX = static_cast<float>(i) * buttonWidth;

    // Active Button Background
    if (i == activeNavIndex) {
      r->fill(76, 217, 100);
      r->rect(offsetX, buttonTop, buttonWidth, buttonHeight);
    }

    // Button Text
    if (i == activeNavIndex) {
      r->fill(255);
    } else {
      r->fill(100);
    }

    float yOffset = 40.0f;

    r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
    r->textFont("roboto", 12);
    r->text(navLabels[i], offsetX + buttonWidth / 2.0f, screenHeight - buttonHeight / 2.0f + yOffset);

    r->textFont("emoji", 20);
    r->text(navEmojiLabels[i], offsetX + buttonWidth / 2.0f, screenHeight - buttonHeight / 2.0f - yOffset);

    // Check for and handle button press
    if (info.mouseX > offsetX &&
        info.mouseX < offsetX + buttonWidth &&
        info.mouseY > buttonTop &&
        info.mouseY < buttonTop + buttonHeight)
    {
      const auto targetId = static_cast<uint32_t>(sceneIdMap[i]);
      switcher->setCurrentScene(targetId);
      LOGI("Button %d clicked! Switching to scene %u", i, targetId);
    }
  }

  // Small bar above buttons
  r->fill(100);
  r->rect(0, screenHeight - buttonHeight, screenWidth, 1);
}

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
  drawNavButtons(info, switcher);

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

        am->preloadFont("roboto", 9);
        am->preloadFont("roboto", 10);
        am->preloadFont("roboto", 11);
        am->preloadFont("roboto", 12);
        am->preloadFont("roboto", 14);
        am->preloadFont("roboto", 15);
        am->preloadFont("roboto", 18);
        am->preloadFont("emoji", 70);
        am->preloadFont("emoji", 150);
    }

    void homeScene(const SceneInfo& info, SceneSwitcher* switcher) {
        const auto r = info.engine->getRenderingManager()->getRenderer2D();
        float screenW = (float)ANativeWindow_getWidth(info.pApp->window);
        float screenH = (float)ANativeWindow_getHeight(info.pApp->window);

        r->rectMode(ge::RectMode::CORNER);
        r->imageMode(ge::ImageMode::CORNER);

        // ── Background ──
        r->fill(245, 245, 245);
        r->rect(0, 0, screenW, screenH);

        // ── Layout constants ──
        float margin = screenW * 0.05f;
        float cardTop = screenH * 0.05f;
        float cardW = screenW - 2.0f * margin;
        float cardH = screenH * 0.38f;
        float cornerR = 30.0f;

        r->fill(76, 175, 80);
        r->rect(margin + cornerR, cardTop, cardW - 2.0f * cornerR, cardH);
        r->rect(margin, cardTop + cornerR, cardW, cardH - 2.0f * cornerR);

        r->ellipseMode(ge::EllipseMode::CENTER);
        r->ellipse(margin + cornerR, cardTop + cornerR, cornerR * 2.0f, cornerR * 2.0f);
        r->ellipse(margin + cardW - cornerR, cardTop + cornerR, cornerR * 2.0f, cornerR * 2.0f);
        r->ellipse(margin + cornerR, cardTop + cardH - cornerR, cornerR * 2.0f, cornerR * 2.0f);
        r->ellipse(margin + cardW - cornerR, cardTop + cardH - cornerR, cornerR * 2.0f, cornerR * 2.0f);

        // Flights in air card
        r->fill(255, 255, 255);
        float btnW = cardW * 0.75f;
        float btnH = 80.0f;
        float btnX = margin + (cardW - btnW) / 2.0f;
        float btnY = cardTop + cardH * 0.10f;
        float btnR = btnH / 2.0f;

        r->fill(255, 255, 255);
        r->rect(btnX + btnR, btnY, btnW - 2.0f * btnR, btnH);
        r->rect(btnX, btnY, btnW, btnH); // full fill (covered by ellipses at ends)

        r->ellipseMode(ge::EllipseMode::CENTER);
        r->ellipse(btnX + btnR, btnY + btnR, btnH, btnH);
        r->ellipse(btnX + btnW - btnR, btnY + btnR, btnH, btnH);
        r->fill(255, 255, 255);
        r->rect(btnX + btnR, btnY, btnW - 2.0f * btnR, btnH);

        r->fill(76, 175, 80);
        r->textFont("roboto", 14);
        r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
        r->text("Flights in Air", screenW / 2.0f, btnY + btnH / 2.0f);

        float imgPad = cardW * 0.08f;
        float imgTop = btnY + btnH + cardH * 0.06f;
        float imgW = cardW - 2.0f * imgPad;
        float imgH = cardTop + cardH - imgTop - cardH * 0.06f;
        float imgCornerR = 20.0f;
        r->fill(255, 255, 255);
        r->rect(margin + imgPad + imgCornerR, imgTop, imgW - 2.0f * imgCornerR, imgH);
        r->rect(margin + imgPad, imgTop + imgCornerR, imgW, imgH - 2.0f * imgCornerR);
        r->ellipseMode(ge::EllipseMode::CENTER);
        r->ellipse(margin + imgPad + imgCornerR, imgTop + imgCornerR, imgCornerR * 2.0f, imgCornerR * 2.0f);
        r->ellipse(margin + imgPad + imgW - imgCornerR, imgTop + imgCornerR, imgCornerR * 2.0f, imgCornerR * 2.0f);
        r->ellipse(margin + imgPad + imgCornerR, imgTop + imgH - imgCornerR, imgCornerR * 2.0f, imgCornerR * 2.0f);
        r->ellipse(margin + imgPad + imgW - imgCornerR, imgTop + imgH - imgCornerR, imgCornerR * 2.0f, imgCornerR * 2.0f);

        r->image("plane", margin + imgPad, imgTop, imgW, imgH);

        // Recently viewed
        float rvSectionY = cardTop + cardH + 80.0f;
        r->fill(30, 30, 30);
        r->textFont("roboto", 15);
        r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::BASELINE);
        r->text("Recently Viewed", margin, rvSectionY);

        // Carousel state
        static float scrollOffset = 0.0f;
        static float prevMouseX = 0.0f;
        static bool dragging = false;
        static float dragStartX = 0.0f;
        static float dragStartOffset = 0.0f;
        static float totalDragDist = 0.0f;
        static int homeSelectedFlight = -1;
        static bool homeTapConsumed = false;

        homeTapConsumed = false;

        // Carousel layout
        constexpr int RV_COUNT = 10;
        float rvCardGap = 20.0f;
        float rvCardW = (cardW - 2.0f * rvCardGap) / 3.0f;
        float rvTop = rvSectionY + 25.0f;
        float rvImgH = rvCardW * 0.75f;
        float rvCornerR = 12.0f;
        float totalContentW = RV_COUNT * (rvCardW + rvCardGap) - rvCardGap;
        float maxScroll = std::max(0.0f, totalContentW - cardW);

        // Carousel touch zone
        float rvZoneTop = rvTop - 10.0f;
        float rvZoneBottom = rvTop + rvImgH + 80.0f;

        // Drag handling
        bool inZone = info.mouseY >= rvZoneTop && info.mouseY <= rvZoneBottom &&
                      info.mouseX >= margin && info.mouseX <= margin + cardW;

        if (info.tapOccurred && inZone && homeSelectedFlight < 0) {
            dragging = true;
            dragStartX = info.mouseX;
            dragStartOffset = scrollOffset;
            totalDragDist = 0.0f;
            prevMouseX = info.mouseX;
        }

        if (dragging && info.isTouching) {
            float delta = info.mouseX - prevMouseX;
            scrollOffset -= delta;
            totalDragDist += std::abs(delta);
            prevMouseX = info.mouseX;
        }

        if (dragging && !info.isTouching) {
            dragging = false;
        }

        // Clamp scroll
        scrollOffset = std::clamp(scrollOffset, 0.0f, maxScroll);

        // Draw carousel cards
        for (int i = 0; i < RV_COUNT && i < (int)g_flightData.size(); ++i) {
            float rawX = margin + i * (rvCardW + rvCardGap) - scrollOffset;

            // Cull cards outside visible area
            if (rawX + rvCardW < margin - 20.0f || rawX > margin + cardW + 20.0f)
                continue;

            r->fill(230, 235, 240);
            r->rectMode(ge::RectMode::CORNER);
            r->rect(rawX + rvCornerR, rvTop, rvCardW - 2.0f * rvCornerR, rvImgH);
            r->rect(rawX, rvTop + rvCornerR, rvCardW, rvImgH - 2.0f * rvCornerR);
            r->ellipseMode(ge::EllipseMode::CENTER);
            r->fill(230, 235, 240);
            r->ellipse(rawX + rvCornerR, rvTop + rvCornerR, rvCornerR * 2.0f, rvCornerR * 2.0f);
            r->ellipse(rawX + rvCardW - rvCornerR, rvTop + rvCornerR, rvCornerR * 2.0f, rvCornerR * 2.0f);
            r->ellipse(rawX + rvCornerR, rvTop + rvImgH - rvCornerR, rvCornerR * 2.0f, rvCornerR * 2.0f);
            r->ellipse(rawX + rvCardW - rvCornerR, rvTop + rvImgH - rvCornerR, rvCornerR * 2.0f, rvCornerR * 2.0f);

            r->image("plane", rawX, rvTop, rvCardW, rvImgH);

            float textY = rvTop + rvImgH + 30.0f;
            r->fill(50, 50, 50);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::BASELINE);
            r->text(g_flightData[i].callsign, rawX, textY);

            r->fill(120, 120, 120);
            r->textFont("roboto", 9);
            r->text(g_flightData[i].date, rawX, textY + 36.0f);

            // Tap detection on card (only if not dragging)
            if (info.tapOccurred && !homeTapConsumed && homeSelectedFlight < 0 &&
                totalDragDist < 15.0f) {
            }
        }

        // Detect card taps when finger lifts
        if (!info.isTouching && !dragging && totalDragDist < 15.0f && homeSelectedFlight < 0) {
            float tapX = info.mouseX;
            float tapY = info.mouseY;
            if (tapY >= rvTop && tapY <= rvTop + rvImgH + 70.0f) {
                for (int i = 0; i < RV_COUNT && i < (int)g_flightData.size(); ++i) {
                    float rawX = margin + i * (rvCardW + rvCardGap) - scrollOffset;
                    if (tapX >= rawX && tapX <= rawX + rvCardW) {
                        homeSelectedFlight = i;
                        homeTapConsumed = true;
                        totalDragDist = 9999.0f; // Prevent re-triggering
                        break;
                    }
                }
            }
        }

        // ── Favorites Section ──
        float favSectionY = rvTop + rvImgH + 160.0f;
        r->fill(30, 30, 30);
        r->textFont("roboto", 15);
        r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::BASELINE);
        r->text("Favorites", margin, favSectionY);

        // Build favorites list
        std::vector<int> homeFavIndices;
        for (int i = 0; i < (int)g_flightData.size(); ++i) {
            if (i < (int)g_flightFavorites.size() && g_flightFavorites[i]) {
                homeFavIndices.push_back(i);
            }
        }
        int favCount = static_cast<int>(homeFavIndices.size());

        // Favorites carousel state
        static float favScrollOffset = 0.0f;
        static float favPrevMouseX = 0.0f;
        static bool favDragging = false;
        static float favTotalDragDist = 0.0f;

        float favTop = favSectionY + 25.0f;
        float favTotalContentW = favCount > 0 ? favCount * (rvCardW + rvCardGap) - rvCardGap : 0.0f;
        float favMaxScroll = std::max(0.0f, favTotalContentW - cardW);

        // Favorites touch zone
        float favZoneTop = favTop - 10.0f;
        float favZoneBottom = favTop + rvImgH + 80.0f;

        bool inFavZone = info.mouseY >= favZoneTop && info.mouseY <= favZoneBottom &&
                         info.mouseX >= margin && info.mouseX <= margin + cardW;

        if (info.tapOccurred && inFavZone && homeSelectedFlight < 0) {
            favDragging = true;
            favTotalDragDist = 0.0f;
            favPrevMouseX = info.mouseX;
        }

        if (favDragging && info.isTouching) {
            float delta = info.mouseX - favPrevMouseX;
            favScrollOffset -= delta;
            favTotalDragDist += std::abs(delta);
            favPrevMouseX = info.mouseX;
        }

        if (favDragging && !info.isTouching) {
            favDragging = false;
        }

        favScrollOffset = std::clamp(favScrollOffset, 0.0f, favMaxScroll);

        // Draw favorites cards
        for (int fi = 0; fi < favCount; ++fi) {
            int idx = homeFavIndices[fi];
            float rawX = margin + fi * (rvCardW + rvCardGap) - favScrollOffset;

            if (rawX + rvCardW < margin - 20.0f || rawX > margin + cardW + 20.0f)
                continue;

            r->fill(230, 235, 240);
            r->rectMode(ge::RectMode::CORNER);
            r->rect(rawX + rvCornerR, favTop, rvCardW - 2.0f * rvCornerR, rvImgH);
            r->rect(rawX, favTop + rvCornerR, rvCardW, rvImgH - 2.0f * rvCornerR);
            r->ellipseMode(ge::EllipseMode::CENTER);
            r->fill(230, 235, 240);
            r->ellipse(rawX + rvCornerR, favTop + rvCornerR, rvCornerR * 2.0f, rvCornerR * 2.0f);
            r->ellipse(rawX + rvCardW - rvCornerR, favTop + rvCornerR, rvCornerR * 2.0f, rvCornerR * 2.0f);
            r->ellipse(rawX + rvCornerR, favTop + rvImgH - rvCornerR, rvCornerR * 2.0f, rvCornerR * 2.0f);
            r->ellipse(rawX + rvCardW - rvCornerR, favTop + rvImgH - rvCornerR, rvCornerR * 2.0f, rvCornerR * 2.0f);

            r->image("plane", rawX, favTop, rvCardW, rvImgH);

            float ftextY = favTop + rvImgH + 30.0f;
            r->fill(50, 50, 50);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::BASELINE);
            r->text(g_flightData[idx].callsign, rawX, ftextY);

            r->fill(120, 120, 120);
            r->textFont("roboto", 9);
            r->text(g_flightData[idx].date, rawX, ftextY + 36.0f);
        }

        if (!info.isTouching && !favDragging && favTotalDragDist < 15.0f && homeSelectedFlight < 0) {
            float tapX = info.mouseX;
            float tapY = info.mouseY;
            if (tapY >= favTop && tapY <= favTop + rvImgH + 70.0f) {
                for (int fi = 0; fi < favCount; ++fi) {
                    int idx = homeFavIndices[fi];
                    float rawX = margin + fi * (rvCardW + rvCardGap) - favScrollOffset;
                    if (tapX >= rawX && tapX <= rawX + rvCardW) {
                        homeSelectedFlight = idx;
                        homeTapConsumed = true;
                        favTotalDragDist = 9999.0f;
                        break;
                    }
                }
            }
        }

        // Widget overlay
        if (homeSelectedFlight >= 0 && homeSelectedFlight < (int)g_flightData.size()) {
            const auto& flight = g_flightData[homeSelectedFlight];

            r->fill(0, 0, 0, 80);
            r->rect(0, 0, screenW, screenH);

            float widgetW = screenW * 0.82f;
            float widgetH = 380.0f;
            float widgetX = (screenW - widgetW) / 2.0f;
            float widgetY = screenH * 0.30f;

            r->fill(76, 175, 80);
            r->rect(widgetX, widgetY, widgetW, widgetH);
            r->fill(56, 142, 60);
            r->rect(widgetX, widgetY, widgetW, 80.0f);

            r->fill(255, 255, 255);
            r->textFont("roboto", 15);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text(flight.callsign, screenW / 2.0f, widgetY + 40.0f);

            float closeX = widgetX + widgetW - 55.0f;
            float closeY = widgetY + 15.0f;
            float closeSize = 50.0f;
            r->fill(255, 255, 255, 200);
            r->rect(closeX, closeY, closeSize, closeSize);
            r->fill(56, 142, 60);
            r->textFont("roboto", 12);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text("X", closeX + closeSize / 2.0f, closeY + closeSize / 2.0f);

            float contentX = widgetX + 30.0f;
            float rowStart = widgetY + 110.0f;
            float rowGap = 70.0f;
            float halfW = (widgetW - 70.0f) / 2.0f;

            r->fill(240, 248, 255);
            r->rect(contentX, rowStart, halfW, 55.0f);
            r->rect(contentX + halfW + 10.0f, rowStart, halfW, 55.0f);
            r->fill(30, 30, 30);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text("Takeoff: " + flight.takeoffTime, contentX + halfW / 2.0f, rowStart + 27.0f);
            r->text("Landing: " + flight.landingTime, contentX + halfW * 1.5f + 10.0f, rowStart + 27.0f);

            float row2Y = rowStart + rowGap;
            r->fill(240, 248, 255);
            r->rect(contentX, row2Y, widgetW - 60.0f, 55.0f);
            r->fill(30, 30, 30);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::CENTER);
            r->text("Plane Type: " + flight.planeType, contentX + 15.0f, row2Y + 27.0f);

            float row3Y = row2Y + rowGap;
            r->fill(240, 248, 255);
            r->rect(contentX, row3Y, widgetW - 60.0f, 55.0f);
            r->fill(30, 30, 30);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::CENTER);
            r->text("Airspeed: " + std::to_string(flight.airspeed) + " kts", contentX + 15.0f, row3Y + 27.0f);

            if (info.tapOccurred && !homeTapConsumed) {
                if (info.mouseX >= closeX && info.mouseX <= closeX + closeSize &&
                    info.mouseY >= closeY && info.mouseY <= closeY + closeSize) {
                    homeSelectedFlight = -1;
                    homeTapConsumed = true;
                }
                else if (info.mouseX < widgetX || info.mouseX > widgetX + widgetW ||
                         info.mouseY < widgetY || info.mouseY > widgetY + widgetH) {
                    homeSelectedFlight = -1;
                    homeTapConsumed = true;
                }
            }
        }

        if (info.tapOccurred && homeSelectedFlight < 0) {
            if (info.mouseX >= btnX && info.mouseX <= btnX + btnW &&
                info.mouseY >= btnY && info.mouseY <= btnY + btnH) {
                switcher->setCurrentScene(static_cast<uint32_t>(SceneId::AR));
            }
        }

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

        float margin = screenW * 0.06f;
        float headerY = screenH * 0.06f;
        float titleY = headerY + 70.0f;
        float subtitleY = titleY + 45.0f;
        float favLinkY = subtitleY + 45.0f;
        float listStartY = favLinkY + 50.0f;
        float listEndY = screenH - 280.0f;
        float rowHeight = (listEndY - listStartY) / FLIGHTS_PER_PAGE;

        r->rectMode(ge::RectMode::CORNER);
        r->fill(245, 248, 250);
        r->rect(0, 0, screenW, screenH);

        r->fill(76, 175, 80);
        r->textFont("roboto", 12);
        r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::BASELINE);
        r->text("Back", margin, titleY);

        r->fill(30, 30, 30);
        r->textFont("roboto", 18);
        r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::BASELINE);
        r->text("Flight History", screenW / 2.0f, titleY);

        r->fill(76, 175, 80);
        r->textFont("roboto", 12);
        r->textAlign(ge::TextAlignH::RIGHT, ge::TextAlignV::BASELINE);
        r->text("Next", screenW - margin, titleY);

        r->fill(120, 120, 120);
        r->textFont("roboto", 10);
        r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::BASELINE);
        r->text("Page " + std::to_string(g_flightCurrentPage + 1) + " / " + std::to_string(totalPages),
                screenW / 2.0f, subtitleY);

        // "Favorites" link
        r->fill(76, 175, 80);
        r->textFont("roboto", 11);
        r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::BASELINE);
        r->text("Favorites", screenW / 2.0f, favLinkY);

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

            // Tap on "Favorites" link
            if (info.mouseY >= favLinkY - 35.0f && info.mouseY <= favLinkY + 10.0f &&
                info.mouseX >= screenW * 0.3f && info.mouseX <= screenW * 0.7f) {
                switcher->setCurrentScene(static_cast<uint32_t>(SceneId::Favorites));
                g_flightTapConsumed = true;
            }
        }

        int pageStart = g_flightCurrentPage * FLIGHTS_PER_PAGE;
        int pageEnd = std::min(pageStart + FLIGHTS_PER_PAGE, totalFlights);

        for (int i = pageStart; i < pageEnd; ++i) {
            int rowIdx = i - pageStart;
            float rowY = listStartY + rowIdx * rowHeight;
            float textY = rowY + rowHeight * 0.65f;
            float rightEdge = screenW - margin;
            float dotRadius = 16.0f;
            float starX = margin + 240.0f;
            float starHalf = 18.0f;

            r->fill(200, 200, 200);
            r->rect(margin, rowY, screenW - 2.0f * margin, 2.0f);
            r->rect(margin, rowY + rowHeight - 2.0f, screenW - 2.0f * margin, 2.0f);

            r->fill(50, 50, 50);
            r->textFont("roboto", 11);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::BASELINE);
            r->text(g_flightData[i].callsign, margin + 10.0f, textY);

            // Favorite indicator (square)
            if (i < (int)g_flightFavorites.size() && g_flightFavorites[i]) {
                r->fill(128, 0, 128); // purple
            } else {
                r->fill(180, 180, 180); // grey
            }
            r->rectMode(ge::RectMode::CORNER);
            r->rect(starX - starHalf, rowY + rowHeight / 2.0f - starHalf, starHalf * 2.0f, starHalf * 2.0f);

            r->fill(100, 100, 100);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::RIGHT, ge::TextAlignV::BASELINE);
            r->text(g_flightData[i].date, rightEdge - dotRadius * 3.0f, textY);

            r->fill(76, 175, 80);
            r->ellipseMode(ge::EllipseMode::CENTER);
            r->ellipse(rightEdge - dotRadius, rowY + rowHeight / 2.0f, dotRadius * 2.0f, dotRadius * 2.0f);

            // Tap on star to toggle favorite
            if (info.tapOccurred && !g_flightTapConsumed && !widgetShown) {
                if (info.mouseX >= starX - 30.0f && info.mouseX <= starX + 30.0f &&
                    info.mouseY >= rowY && info.mouseY <= rowY + rowHeight) {
                    if (i < (int)g_flightFavorites.size()) {
                        g_flightFavorites[i] = !g_flightFavorites[i];
                    }
                    g_flightTapConsumed = true;
                }
            }

            // Tap on row (excluding star) to open detail widget
            if (info.tapOccurred && !g_flightTapConsumed && !widgetShown) {
                if (info.mouseX >= margin && info.mouseX <= rightEdge &&
                    info.mouseY >= rowY && info.mouseY <= rowY + rowHeight) {
                    g_flightSelectedIndex = i;
                    g_flightTapConsumed = true;
                }
            }
        }


        if (g_flightSelectedIndex >= 0 && g_flightSelectedIndex < totalFlights) {
            const auto& flight = g_flightData[g_flightSelectedIndex];

            r->fill(0, 0, 0, 80);
            r->rect(0, 0, screenW, screenH);

            float widgetW = screenW * 0.82f;
            float widgetH = 380.0f;
            float widgetX = (screenW - widgetW) / 2.0f;
            float widgetY = screenH * 0.30f;

            r->fill(76, 175, 80);
            r->rect(widgetX, widgetY, widgetW, widgetH);

            r->fill(56, 142, 60);
            r->rect(widgetX, widgetY, widgetW, 80.0f);

            r->fill(255, 255, 255);
            r->textFont("roboto", 15);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text(flight.callsign, screenW / 2.0f, widgetY + 40.0f);

            float closeX = widgetX + widgetW - 55.0f;
            float closeY = widgetY + 15.0f;
            float closeSize = 50.0f;
            r->fill(255, 255, 255, 200);
            r->rect(closeX, closeY, closeSize, closeSize);
            r->fill(56, 142, 60);
            r->textFont("roboto", 12);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text("X", closeX + closeSize / 2.0f, closeY + closeSize / 2.0f);

            float contentX = widgetX + 30.0f;
            float rowStart = widgetY + 110.0f;
            float rowGap = 70.0f;
            float halfW = (widgetW - 70.0f) / 2.0f;

            r->fill(240, 248, 255);
            r->rect(contentX, rowStart, halfW, 55.0f);
            r->rect(contentX + halfW + 10.0f, rowStart, halfW, 55.0f);

            r->fill(30, 30, 30);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text("Takeoff: " + flight.takeoffTime, contentX + halfW / 2.0f, rowStart + 27.0f);
            r->text("Landing: " + flight.landingTime, contentX + halfW * 1.5f + 10.0f, rowStart + 27.0f);

            float row2Y = rowStart + rowGap;
            r->fill(240, 248, 255);
            r->rect(contentX, row2Y, widgetW - 60.0f, 55.0f);
            r->fill(30, 30, 30);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::CENTER);
            r->text("Plane Type: " + flight.planeType, contentX + 15.0f, row2Y + 27.0f);

            float row3Y = row2Y + rowGap;
            r->fill(240, 248, 255);
            r->rect(contentX, row3Y, widgetW - 60.0f, 55.0f);
            r->fill(30, 30, 30);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::CENTER);
            r->text("Airspeed: " + std::to_string(flight.airspeed) + " kts", contentX + 15.0f, row3Y + 27.0f);

            if (info.tapOccurred && !g_flightTapConsumed) {
                if (info.mouseX >= closeX && info.mouseX <= closeX + closeSize &&
                    info.mouseY >= closeY && info.mouseY <= closeY + closeSize) {
                    g_flightSelectedIndex = -1;
                    g_flightTapConsumed = true;
                }
                else if (info.mouseX < widgetX || info.mouseX > widgetX + widgetW ||
                         info.mouseY < widgetY || info.mouseY > widgetY + widgetH) {
                    g_flightSelectedIndex = -1;
                    g_flightTapConsumed = true;
                }
            }
        }

        drawCommonUI(info, switcher);
    }

    void favoritesScene(const SceneInfo& info, SceneSwitcher* switcher) {
        const auto r = info.engine->getRenderingManager()->getRenderer2D();
        float screenW = (float)ANativeWindow_getWidth(info.pApp->window);
        float screenH = (float)ANativeWindow_getHeight(info.pApp->window);

        g_favTapConsumed = false;

        // Build list of favorite flight indices
        std::vector<int> favIndices;
        for (int i = 0; i < (int)g_flightData.size(); ++i) {
            if (i < (int)g_flightFavorites.size() && g_flightFavorites[i]) {
                favIndices.push_back(i);
            }
        }

        int totalFavs = static_cast<int>(favIndices.size());
        int totalPages = totalFavs > 0 ? ((totalFavs - 1) / FLIGHTS_PER_PAGE) + 1 : 1;
        g_favCurrentPage = std::clamp(g_favCurrentPage, 0, std::max(0, totalPages - 1));

        float margin = screenW * 0.06f;
        float headerY = screenH * 0.06f;
        float titleY = headerY + 70.0f;
        float subtitleY = titleY + 45.0f;
        float histLinkY = subtitleY + 45.0f;
        float listStartY = histLinkY + 50.0f;
        float listEndY = screenH - 280.0f;
        float rowHeight = (listEndY - listStartY) / FLIGHTS_PER_PAGE;

        r->rectMode(ge::RectMode::CORNER);
        r->fill(245, 248, 250);
        r->rect(0, 0, screenW, screenH);

        // Header: Back / Title / Next
        r->fill(76, 175, 80);
        r->textFont("roboto", 12);
        r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::BASELINE);
        r->text("Back", margin, titleY);

        r->fill(30, 30, 30);
        r->textFont("roboto", 18);
        r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::BASELINE);
        r->text("Favorites", screenW / 2.0f, titleY);

        r->fill(76, 175, 80);
        r->textFont("roboto", 12);
        r->textAlign(ge::TextAlignH::RIGHT, ge::TextAlignV::BASELINE);
        r->text("Next", screenW - margin, titleY);

        // Page indicator
        r->fill(120, 120, 120);
        r->textFont("roboto", 10);
        r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::BASELINE);
        r->text("Page " + std::to_string(g_favCurrentPage + 1) + " / " + std::to_string(totalPages),
                screenW / 2.0f, subtitleY);

        // "Flight History" link
        r->fill(76, 175, 80);
        r->textFont("roboto", 11);
        r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::BASELINE);
        r->text("Flight History", screenW / 2.0f, histLinkY);

        bool widgetShown = (g_favSelectedIndex >= 0);
        if (info.tapOccurred && !widgetShown) {
            float btnTop = headerY;
            float btnBottom = headerY + 80.0f;
            if (info.mouseY >= btnTop && info.mouseY <= btnBottom) {
                if (info.mouseX < screenW * 0.25f && g_favCurrentPage > 0) {
                    g_favCurrentPage--;
                    g_favTapConsumed = true;
                }
                if (info.mouseX > screenW * 0.75f && g_favCurrentPage < totalPages - 1) {
                    g_favCurrentPage++;
                    g_favTapConsumed = true;
                }
            }

            // Tap on "Flight History" link
            if (info.mouseY >= histLinkY - 35.0f && info.mouseY <= histLinkY + 10.0f &&
                info.mouseX >= screenW * 0.2f && info.mouseX <= screenW * 0.8f) {
                switcher->setCurrentScene(static_cast<uint32_t>(SceneId::FlightHistory));
                g_favTapConsumed = true;
            }
        }

        // Draw rows
        int pageStart = g_favCurrentPage * FLIGHTS_PER_PAGE;
        int pageEnd = std::min(pageStart + FLIGHTS_PER_PAGE, totalFavs);

        for (int fi = pageStart; fi < pageEnd; ++fi) {
            int i = favIndices[fi]; // actual flight index
            int rowIdx = fi - pageStart;
            float rowY = listStartY + rowIdx * rowHeight;
            float textY = rowY + rowHeight * 0.65f;
            float rightEdge = screenW - margin;
            float dotRadius = 16.0f;
            float starX = margin + 240.0f;
            float starHalf = 18.0f;

            r->fill(200, 200, 200);
            r->rect(margin, rowY, screenW - 2.0f * margin, 2.0f);
            r->rect(margin, rowY + rowHeight - 2.0f, screenW - 2.0f * margin, 2.0f);

            r->fill(50, 50, 50);
            r->textFont("roboto", 11);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::BASELINE);
            r->text(g_flightData[i].callsign, margin + 10.0f, textY);

            // Purple square (all favorites are purple)
            r->fill(128, 0, 128);
            r->rectMode(ge::RectMode::CORNER);
            r->rect(starX - starHalf, rowY + rowHeight / 2.0f - starHalf, starHalf * 2.0f, starHalf * 2.0f);

            r->fill(100, 100, 100);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::RIGHT, ge::TextAlignV::BASELINE);
            r->text(g_flightData[i].date, rightEdge - dotRadius * 3.0f, textY);

            r->fill(76, 175, 80);
            r->ellipseMode(ge::EllipseMode::CENTER);
            r->ellipse(rightEdge - dotRadius, rowY + rowHeight / 2.0f, dotRadius * 2.0f, dotRadius * 2.0f);

            // Tap on star to un-favorite
            if (info.tapOccurred && !g_favTapConsumed && !widgetShown) {
                if (info.mouseX >= starX - 30.0f && info.mouseX <= starX + 30.0f &&
                    info.mouseY >= rowY && info.mouseY <= rowY + rowHeight) {
                    g_flightFavorites[i] = false;
                    g_favTapConsumed = true;
                }
            }

            // Tap on row to show detail widget
            if (info.tapOccurred && !g_favTapConsumed && !widgetShown) {
                if (info.mouseX >= margin && info.mouseX <= rightEdge &&
                    info.mouseY >= rowY && info.mouseY <= rowY + rowHeight) {
                    g_favSelectedIndex = i;
                    g_favTapConsumed = true;
                }
            }
        }

        // Detail widget overlay
        if (g_favSelectedIndex >= 0 && g_favSelectedIndex < (int)g_flightData.size()) {
            const auto& flight = g_flightData[g_favSelectedIndex];

            r->fill(0, 0, 0, 80);
            r->rect(0, 0, screenW, screenH);

            float widgetW = screenW * 0.82f;
            float widgetH = 380.0f;
            float widgetX = (screenW - widgetW) / 2.0f;
            float widgetY = screenH * 0.30f;

            r->fill(76, 175, 80);
            r->rect(widgetX, widgetY, widgetW, widgetH);

            r->fill(56, 142, 60);
            r->rect(widgetX, widgetY, widgetW, 80.0f);

            r->fill(255, 255, 255);
            r->textFont("roboto", 15);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text(flight.callsign, screenW / 2.0f, widgetY + 40.0f);

            float closeX = widgetX + widgetW - 55.0f;
            float closeY = widgetY + 15.0f;
            float closeSize = 50.0f;
            r->fill(255, 255, 255, 200);
            r->rect(closeX, closeY, closeSize, closeSize);
            r->fill(56, 142, 60);
            r->textFont("roboto", 12);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text("X", closeX + closeSize / 2.0f, closeY + closeSize / 2.0f);

            float contentX = widgetX + 30.0f;
            float rowStart = widgetY + 110.0f;
            float rowGap = 70.0f;
            float halfW = (widgetW - 70.0f) / 2.0f;

            r->fill(240, 248, 255);
            r->rect(contentX, rowStart, halfW, 55.0f);
            r->rect(contentX + halfW + 10.0f, rowStart, halfW, 55.0f);

            r->fill(30, 30, 30);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::CENTER, ge::TextAlignV::CENTER);
            r->text("Takeoff: " + flight.takeoffTime, contentX + halfW / 2.0f, rowStart + 27.0f);
            r->text("Landing: " + flight.landingTime, contentX + halfW * 1.5f + 10.0f, rowStart + 27.0f);

            float row2Y = rowStart + rowGap;
            r->fill(240, 248, 255);
            r->rect(contentX, row2Y, widgetW - 60.0f, 55.0f);
            r->fill(30, 30, 30);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::CENTER);
            r->text("Plane Type: " + flight.planeType, contentX + 15.0f, row2Y + 27.0f);

            float row3Y = row2Y + rowGap;
            r->fill(240, 248, 255);
            r->rect(contentX, row3Y, widgetW - 60.0f, 55.0f);
            r->fill(30, 30, 30);
            r->textFont("roboto", 10);
            r->textAlign(ge::TextAlignH::LEFT, ge::TextAlignV::CENTER);
            r->text("Airspeed: " + std::to_string(flight.airspeed) + " kts", contentX + 15.0f, row3Y + 27.0f);

            if (info.tapOccurred && !g_favTapConsumed) {
                if (info.mouseX >= closeX && info.mouseX <= closeX + closeSize &&
                    info.mouseY >= closeY && info.mouseY <= closeY + closeSize) {
                    g_favSelectedIndex = -1;
                    g_favTapConsumed = true;
                }
                else if (info.mouseX < widgetX || info.mouseX > widgetX + widgetW ||
                         info.mouseY < widgetY || info.mouseY > widgetY + widgetH) {
                    g_favSelectedIndex = -1;
                    g_favTapConsumed = true;
                }
            }
        }

        drawCommonUI(info, switcher);
    }

} // namespace AppScenes
