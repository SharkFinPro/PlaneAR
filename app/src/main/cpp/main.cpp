#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <android/log.h>

#define LOG_TAG "HelloWorld"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

void android_main(struct android_app *pApp)
{
  LOGI("Hello World from C++!");

  int events;
  struct android_poll_source* source;

  while (true)
  {
    int result = ALooper_pollOnce(0, nullptr, &events, (void**)&source);

    if (result >= 0)
    {
      if (source != nullptr)
      {
        source->process(pApp, source);
      }

      if (pApp->destroyRequested != 0)
      {
        LOGI("App destroy requested, exiting...");
        return;
      }
    }
  }
}