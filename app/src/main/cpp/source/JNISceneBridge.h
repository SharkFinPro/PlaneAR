#ifndef PLANEAR_JNI_SCENE_BRIDGE_H
#define PLANEAR_JNI_SCENE_BRIDGE_H

#include <jni.h>

class SceneSwitcher;

namespace JNISceneBridge {
  void setSceneSwitcher(SceneSwitcher* switcher);
}

#endif //PLANEAR_JNI_SCENE_BRIDGE_H
