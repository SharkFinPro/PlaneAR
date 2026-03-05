#include "AppScenes.h"
#include <source/components/assets/AssetManager.h>

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
        am->preloadFont("emoji", 20);
    }
} // namespace AppScenes
