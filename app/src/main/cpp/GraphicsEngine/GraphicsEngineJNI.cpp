#include <jni.h>
#include "GraphicsEngine.h"
#include <android/log.h>
#include <stdexcept>

#define LOG_TAG "HelloWorld"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_edu_osu_t22_planear_GraphicsEngineWrapper_nativeCreate(
    JNIEnv* env,
    jobject thiz)
{
  try
  {
    auto* engine = new ge::GraphicsEngine();
    LOGI("GraphicsEngine created: %p", engine);
    return reinterpret_cast<jlong>(engine);
  }
  catch (const std::exception& e)
  {
    LOGI("Failed to create GraphicsEngine: %s", e.what());
    return 0;
  }
}

JNIEXPORT void JNICALL
Java_edu_osu_t22_planear_GraphicsEngineWrapper_nativeDestroy(
    JNIEnv* env,
    jobject thiz,
    jlong handle)
{
  auto* engine = reinterpret_cast<ge::GraphicsEngine*>(handle);
  if (engine) {
    LOGI("Destroying GraphicsEngine: %p", engine);
    delete engine;
  } else {
    LOGI("Invalid engine handle in nativeDestroy");
  }
}

} // extern "C"