#include "GraphicsEngine.h"
#include "components/instance/Instance.h"
#include <android/log.h>

#define LOG_TAG "HelloWorld"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace ge {

  GraphicsEngine::GraphicsEngine()
  {
    LOGI("Creating Graphics Engine!");

    m_instance = std::make_shared<Instance>();
  }

  GraphicsEngine::~GraphicsEngine()
  {
    LOGI("Destroying Graphics Engine!");
  }

} // ge