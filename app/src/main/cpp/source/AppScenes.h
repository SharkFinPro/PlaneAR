#ifndef PLANEAR_APPSCENES_H
#define PLANEAR_APPSCENES_H

#include "SceneSwitcher.h"
#include <source/GraphicsEngine.h>
#include <memory>

namespace AppScenes {
  void homeScene(const SceneInfo& info, SceneSwitcher* switcher);
  void arScene(const SceneInfo& info, SceneSwitcher* switcher);
  void filesScene(const SceneInfo& info, SceneSwitcher* switcher);
  void mapScene(const SceneInfo& info, SceneSwitcher* switcher);
  void settingsScene(const SceneInfo& info, SceneSwitcher* switcher);

  void initialize(const std::unique_ptr<ge::GraphicsEngine>& engine, struct android_app* pApp);
}

#endif //PLANEAR_APPSCENES_H
