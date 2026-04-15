#include "GraphicsEngine.h"
#include "components/assets/AssetManager.h"
#include "components/assets/textures/CameraTexture.h"
#include "components/renderingManager/RenderingManager.h"
#include "components/renderingManager/renderer2D/Renderer2D.h"
#include <android/hardware_buffer_jni.h>
#include <android/hardware_buffer.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>

#include "../Logger.h"

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

  void nativeFillRGB(JNIEnv* env,
                     jobject thiz,
                     jint rgb,
                     jint a)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->fill(rgb, a);
  }

  void nativeRotate(JNIEnv* env,
                    jobject thiz,
                    jfloat angle)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->rotate(angle);
  }

  void nativeTranslate(JNIEnv* env,
                       jobject thiz,
                       jfloat x,
                       jfloat y)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->translate(x, y);
  }

  void nativeScaleXY(JNIEnv* env,
                     jobject thiz,
                     jfloat x,
                     jfloat y)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->scale(x, y);
  }

  void nativeScale(JNIEnv* env,
                   jobject thiz,
                   jfloat xy)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->scale(xy);
  }

  void nativePushMatrix(JNIEnv* env,
                        jobject thiz)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->pushMatrix();
  }

  void nativePopMatrix(JNIEnv* env,
                       jobject thiz)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->popMatrix();
  }

  void nativeResetMatrix(JNIEnv* env,
                         jobject thiz)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->resetMatrix();
  }

  void nativeRectMode(JNIEnv* env,
                      jobject thiz,
                      jint mode)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->rectMode(static_cast<ge::RectMode>(mode));
  }

  void nativeEllipseMode(JNIEnv* env,
                         jobject thiz,
                         jint mode)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->ellipseMode(static_cast<ge::EllipseMode>(mode));
  }

  void nativeImageMode(JNIEnv* env,
                       jobject thiz,
                       jint mode)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->imageMode(static_cast<ge::ImageMode>(mode));
  }

  void nativeRect(JNIEnv* env,
                  jobject thiz,
                  jfloat x,
                  jfloat y,
                  jfloat width,
                  jfloat height,
                  jfloat radius)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->rect(x, y, width, height, radius);
  }

  void nativeTriangle(JNIEnv* env,
                      jobject thiz,
                      jfloat x1,
                      jfloat y1,
                      jfloat x2,
                      jfloat y2,
                      jfloat x3,
                      jfloat y3)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->triangle(x1, y1, x2, y2, x3, y3);
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

  void nativeTextAlign(JNIEnv* env,
                       jobject thiz,
                       jint alignH,
                       jint alignV)
  {
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      return;
    }
    renderer->textAlign(static_cast<ge::TextAlignH>(alignH),
                        static_cast<ge::TextAlignV>(alignV));
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

  void nativeUpdateCameraBuffer(JNIEnv* env,
                                jobject thiz,
                                jobject hardwareBuffer)
  {
    LOGI("HB - nativeUpdateCameraBuffer");
    ge::Renderer2D* renderer = getRenderer(env, thiz);
    if (renderer == nullptr)
    {
      LOGE("HB - renderer not found!");
      return;
    }

    AHardwareBuffer* ahb = AHardwareBuffer_fromHardwareBuffer(env, hardwareBuffer);
    if (ahb == nullptr)
    {
      LOGW("HB Not Available!");
      return;
    }

    LOGI("HB Updating...");

    renderer->getAssetManager()->getCameraTexture()->updateFromHardwareBuffer(ahb);
  }

  const JNINativeMethod renderer2DMethods[] = {
    {"fill",        "(IIII)V",                    (void*)nativeFill},
    {"fill",        "(II)V",                      (void*)nativeFillRGB},
    {"rotate",      "(F)V",                       (void*)nativeRotate},
    {"translate",   "(FF)V",                      (void*)nativeTranslate},
    {"scale",       "(FF)V",                      (void*)nativeScaleXY},
    {"scale",       "(F)V",                       (void*)nativeScale},
    {"pushMatrix",  "()V",                        (void*)nativePushMatrix},
    {"popMatrix",   "()V",                        (void*)nativePopMatrix},
    {"resetMatrix", "()V",                        (void*)nativeResetMatrix},
    {"rectMode",    "(I)V",                       (void*)nativeRectMode},
    {"ellipseMode", "(I)V",                       (void*)nativeEllipseMode},
    {"imageMode",   "(I)V",                       (void*)nativeImageMode},
    {"rect",        "(FFFFF)V",                    (void*)nativeRect},
    {"triangle",    "(FFFFFF)V",                  (void*)nativeTriangle},
    {"ellipse",     "(FFFF)V",                    (void*)nativeEllipse},
    {"textFont",    "(Ljava/lang/String;I)V",     (void*)nativeTextFont},
    {"textSize",    "(I)V",                       (void*)nativeTextSize},
    {"textAlign",   "(II)V",                      (void*)nativeTextAlign},
    {"text",        "(Ljava/lang/String;FF)V",    (void*)nativeText},
    {"image",       "(Ljava/lang/String;FFFF)V",  (void*)nativeImage},
    {"updateCameraBuffer", "(Landroid/hardware/HardwareBuffer;)V", (void*)nativeUpdateCameraBuffer}
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