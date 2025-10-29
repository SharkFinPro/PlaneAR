#include "GraphicsEngine.h"
#include <android/log.h>

#define LOG_TAG "HelloWorld"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace ge {

  GraphicsEngine::GraphicsEngine()
  {
    LOGI("Creating Graphics Engine!");
  }

  GraphicsEngine::~GraphicsEngine()
  {
    LOGI("Destroying Graphics Engine!");
  }

} // ge