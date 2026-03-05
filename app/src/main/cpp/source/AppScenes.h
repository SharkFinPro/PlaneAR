#ifndef PLANEAR_APPSCENES_H
#define PLANEAR_APPSCENES_H

#include "SceneSwitcher.h"
#include <source/GraphicsEngine.h>
#include <memory>

namespace AppScenes {
  void initialize(const std::unique_ptr<ge::GraphicsEngine>& engine, struct android_app* pApp);
}

#endif //PLANEAR_APPSCENES_H
