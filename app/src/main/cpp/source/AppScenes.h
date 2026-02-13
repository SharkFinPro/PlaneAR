#ifndef PLANEAR_APPSCENES_H
#define PLANEAR_APPSCENES_H

#include "SceneSwitcher.h"

namespace AppScenes {
    void homeScene(const SceneInfo& info, SceneSwitcher* switcher);
    void arScene(const SceneInfo& info, SceneSwitcher* switcher);
    void filesScene(const SceneInfo& info, SceneSwitcher* switcher);
    void mapScene(const SceneInfo& info, SceneSwitcher* switcher);
    void settingsScene(const SceneInfo& info, SceneSwitcher* switcher);

    // Initialization for persistent UI
    void initUI(struct android_app* pApp);
}

#endif //PLANEAR_APPSCENES_H
