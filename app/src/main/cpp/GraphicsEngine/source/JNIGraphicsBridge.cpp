#include "GraphicsEngine.h"
#include "components/renderingManager/RenderingManager.h"
#include "components/renderingManager/renderer2D/Renderer2D.h"
#include <game-activity/native_app_glue/android_native_app_glue.h>

namespace {
  inline ge::Renderer2D* getRenderer(JNIEnv* env,
                                     jobject thiz)
  {
    jclass clazz = env->GetObjectClass(thiz);
    if (clazz == nullptr || env->ExceptionCheck())
    {
      return nullptr;
    }

    jfieldID ptrField = env->GetFieldID(clazz, "ptr", "J");
    if (ptrField == nullptr || env->ExceptionCheck())
    {
      env->DeleteLocalRef(clazz);
      return nullptr;
    }

    jlong ptr = env->GetLongField(thiz, ptrField);
    env->DeleteLocalRef(clazz);

    if (ptr == 0)
    {
      return nullptr;
    }

    return reinterpret_cast<ge::Renderer2D*>(ptr);
  }

  void nativeFill(JNIEnv* env,
                  jobject thiz,
                  jint r,
                  jint g,
                  jint b,
                  jint a)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->fill(r, g, b, a);
  }

  void nativeRect(JNIEnv* env,
                  jobject thiz,
                  jfloat x,
                  jfloat y,
                  jfloat width,
                  jfloat height)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->rect(x, y, width, height);
  }

  void nativeEllipse(JNIEnv* env,
                     jobject thiz,
                     jfloat x,
                     jfloat y,
                     jfloat width,
                     jfloat height)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->ellipse(x, y, width, height);
  }

  void nativeTextFont(JNIEnv* env,
                      jobject thiz,
                      jstring fontName,
                      jint size)
  {
    const char* fontNameStr = env->GetStringUTFChars(fontName, nullptr);
    if (fontNameStr == nullptr)
    {
      return;
    }

    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      env->ReleaseStringUTFChars(fontName, fontNameStr);
      return;
    }

    renderer->textFont(fontNameStr, size);
    env->ReleaseStringUTFChars(fontName, fontNameStr);
  }

  void nativeTextSize(JNIEnv* env,
                      jobject thiz,
                      jint size)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->textSize(size);
  }

  void nativeText(JNIEnv* env,
                  jobject thiz,
                  jstring text,
                  jfloat x,
                  jfloat y)
  {
    const char* textStr = env->GetStringUTFChars(text, nullptr);
    if (textStr == nullptr)
    {
      return;
    }

    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      env->ReleaseStringUTFChars(text, textStr);
      return;
    }

    renderer->text(textStr, x, y);
    env->ReleaseStringUTFChars(text, textStr);
  }

  void nativeImage(JNIEnv* env,
                  jobject thiz,
                  jstring image,
                  jfloat x,
                  jfloat y,
                  jfloat w,
                  jfloat h)
  {
    const char* imageStr = env->GetStringUTFChars(image, nullptr);
    if (imageStr == nullptr)
    {
      return;
    }

    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      env->ReleaseStringUTFChars(image, imageStr);
      return;
    }

    renderer->image(imageStr, x, y, w, h);
    env->ReleaseStringUTFChars(image, imageStr);
  }

  jlong nativeGetRenderer2DPtr(JNIEnv* env,
                               jobject,
                               jlong enginePtr)
  {
    if (enginePtr == 0)
    {
      return 0;
    }

    auto engine = reinterpret_cast<ge::GraphicsEngine*>(enginePtr);

    auto renderingManager = engine->getRenderingManager();
    if (renderingManager == nullptr)
    {
      return 0;
    }

    auto renderer = renderingManager->getRenderer2D();
    return reinterpret_cast<jlong>(renderer.get());
  }

  const JNINativeMethod renderer2DMethods[] = {
    {"fill", "(IIII)V", (void*)nativeFill},
    {"rect", "(FFFF)V", (void*)nativeRect},
    {"ellipse", "(FFFF)V", (void*)nativeEllipse},
    {"textFont", "(Ljava/lang/String;I)V", (void*)nativeTextFont},
    {"textSize", "(I)V", (void*)nativeTextSize},
    {"text", "(Ljava/lang/String;FF)V", (void*)nativeText},
    {"image", "(Ljava/lang/String;FFFF)V", (void*)nativeImage}
  };

  const JNINativeMethod graphicsEngineMethods[] = {
    {"nativeGetRenderer2DPtr", "(J)J", (void*)nativeGetRenderer2DPtr}
  };

} // anonymous namespace

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm,
                          void* reserved)
{
  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
  {
    return JNI_ERR;
  }

  // Register Renderer2D methods
  jclass renderer2DClass = env->FindClass(JNI_PACKAGE_PATH "/Renderer2D");
  if (renderer2DClass == nullptr)
  {
    return JNI_ERR;
  }

  env->RegisterNatives(
    renderer2DClass,
    renderer2DMethods,
    sizeof(renderer2DMethods) / sizeof(renderer2DMethods[0])
  );

  // Register GraphicsEngineWrapper methods
  jclass graphicsEngineClass = env->FindClass(JNI_PACKAGE_PATH "/GraphicsEngineWrapper");
  if (graphicsEngineClass == nullptr)
  {
    return JNI_ERR;
  }

  env->RegisterNatives(
    graphicsEngineClass,
    graphicsEngineMethods,
    sizeof(graphicsEngineMethods) / sizeof(graphicsEngineMethods[0])
  );

  return JNI_VERSION_1_6;
}

} // extern "C"
