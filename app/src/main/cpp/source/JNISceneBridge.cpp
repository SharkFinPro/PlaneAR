#include "JNISceneBridge.h"
#include "SceneSwitcher.h"
#include "source/components/renderingManager/RenderingManager.h"
#include "source/components/renderingManager/renderer2D/Renderer2D.h"
#include <Logger.h>
#include <android/native_activity.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <mutex>

namespace {
  std::recursive_mutex g_jniBridgeMutex;
  SceneSwitcher* g_sceneSwitcher = nullptr;
  JavaVM* g_javaVM = nullptr;
  jclass g_sceneSwitcherClass = nullptr;
  jmethodID g_renderSceneMethod = nullptr;

  inline void renderKotlinScene(uint32_t sceneId, const SceneInfo& sceneInfo)
  {
    std::lock_guard<std::recursive_mutex> lock(g_jniBridgeMutex);

    if (!g_javaVM || !g_sceneSwitcherClass || !g_renderSceneMethod)
    {
      LOGE("JNI bridge not initialized!");
      return;
    }

    JNIEnv* env;
    bool needDetach = false;

    int status = g_javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (status == JNI_EDETACHED)
    {
      if (g_javaVM->AttachCurrentThread(&env, nullptr) != 0)
      {
        LOGE("Failed to attach thread to JVM!");
        return;
      }
      needDetach = true;
    }

    if (!sceneInfo.pApp || !sceneInfo.pApp->window)
    {
      LOGE("Invalid app or window in sceneInfo!");
      if (needDetach)
      {
        g_javaVM->DetachCurrentThread();
      }
      return;
    }

    const auto enginePtr = reinterpret_cast<jlong>(sceneInfo.engine.get());
    const auto screenWidth = (jfloat)ANativeWindow_getWidth(sceneInfo.pApp->window);
    const auto screenHeight = (jfloat)ANativeWindow_getHeight(sceneInfo.pApp->window);

    env->CallStaticVoidMethod(
      g_sceneSwitcherClass,
      g_renderSceneMethod,
      (jint)sceneId,
      enginePtr,
      (jfloat)sceneInfo.mouseX,
      (jfloat)sceneInfo.mouseY,
      (jboolean)sceneInfo.tapOccurred,
      screenWidth,
      screenHeight
    );

    if (env->ExceptionCheck())
    {
      env->ExceptionDescribe();
      env->ExceptionClear();
    }

    if (needDetach)
    {
      g_javaVM->DetachCurrentThread();
    }
  }

  void nativeInit(JNIEnv* env, jclass, jobject sceneSwitcher)
  {
    std::lock_guard<std::recursive_mutex> lock(g_jniBridgeMutex);

    if (g_javaVM)
    {
      return;
    }

    env->GetJavaVM(&g_javaVM);

    jclass localClass = env->GetObjectClass(sceneSwitcher);
    g_sceneSwitcherClass = (jclass)env->NewGlobalRef(localClass);
    env->DeleteLocalRef(localClass);

    g_renderSceneMethod = env->GetStaticMethodID(
      g_sceneSwitcherClass,
      "renderScene",
      "(IJFFZFF)V"
    );

    if (!g_renderSceneMethod)
    {
      LOGE("Failed to find renderScene method!");

      env->DeleteGlobalRef(g_sceneSwitcherClass);
      g_sceneSwitcherClass = nullptr;
      g_javaVM = nullptr;
      return;
    }

    LOGI("JNI bridge initialized successfully");
  }

  void nativeRegisterSceneCallback(JNIEnv* env, jclass, jint sceneId)
  {
    std::lock_guard<std::recursive_mutex> lock(g_jniBridgeMutex);

    LOGI("Registered Kotlin scene with ID: %d", sceneId);

    auto wrapper = [sceneId](const SceneInfo& sceneInfo, SceneSwitcher* sceneSwitcher) {
      renderKotlinScene(sceneId, sceneInfo);
    };

    if (g_sceneSwitcher)
    {
      g_sceneSwitcher->loadScene(sceneId, wrapper);
    }
    else
    {
      LOGE("SceneSwitcher not set when trying to register scene %d", sceneId);
    }
  }

  void nativeSetCurrentScene(JNIEnv* env, jclass, jint sceneId)
  {
    std::lock_guard<std::recursive_mutex> lock(g_jniBridgeMutex);

    if (g_sceneSwitcher)
    {
      g_sceneSwitcher->setCurrentScene((uint32_t)sceneId);
    }
    else
    {
      LOGE("SceneSwitcher not available when trying to set scene %d", sceneId);
    }
  }

  jboolean nativeCheckIfSceneExists(JNIEnv* env, jclass, jint sceneId)
  {
    std::lock_guard<std::recursive_mutex> lock(g_jniBridgeMutex);

    if (g_sceneSwitcher)
    {
      return g_sceneSwitcher->doesSceneExist((uint32_t)sceneId) ? JNI_TRUE : JNI_FALSE;
    }

    LOGE("SceneSwitcher not available when trying to check if scene %d exists", sceneId);
    return JNI_FALSE;
  }

  void nativeSetSceneSwitcher(JNIEnv* env, jclass, jlong switcherPtr)
  {
    std::lock_guard<std::recursive_mutex> lock(g_jniBridgeMutex);

    g_sceneSwitcher = reinterpret_cast<SceneSwitcher*>(switcherPtr);
  }

  const JNINativeMethod sceneSwitcherMethods[] = {
    {"nativeInit", "(Ledu/osu/t22/planear/scenes/SceneSwitcher;)V", (void*)nativeInit},
    {"nativeRegisterSceneCallback", "(I)V", (void*)nativeRegisterSceneCallback},
    {"nativeSetCurrentScene", "(I)V", (void*)nativeSetCurrentScene},
    {"nativeCheckIfSceneExists", "(I)Z", (void*)nativeCheckIfSceneExists}//,
//    {"nativeSetSceneSwitcher", "(J)V", (void*)nativeSetSceneSwitcher}
  };

} // anonymous namespace

// C++ API for setting scene switcher from main.cpp
namespace JNISceneBridge {
  void setSceneSwitcher(SceneSwitcher* switcher)
  {
    std::lock_guard<std::recursive_mutex> lock(g_jniBridgeMutex);

    g_sceneSwitcher = switcher;
  }
}

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
  {
    return JNI_ERR;
  }

  jclass sceneSwitcherClass = env->FindClass("edu/osu/t22/planear/scenes/SceneSwitcher");
  if (sceneSwitcherClass == nullptr)
  {
    return JNI_ERR;
  }

  jint result = env->RegisterNatives(
    sceneSwitcherClass,
    sceneSwitcherMethods,
    sizeof(sceneSwitcherMethods) / sizeof(sceneSwitcherMethods[0])
  );

  env->DeleteLocalRef(sceneSwitcherClass);

  if (result != 0)
  {
    return JNI_ERR;
  }

  return JNI_VERSION_1_6;
}

} // extern "C"
