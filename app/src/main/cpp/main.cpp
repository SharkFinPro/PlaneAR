#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <android/log.h>

#include <vulkan/vulkan.h>
#include <stdexcept>

#define LOG_TAG "HelloWorld"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

void android_main(struct android_app *pApp)
{
  LOGI("Hello World from C++!");

  VkInstance instance;

  VkApplicationInfo appInfo{};
  appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
  appInfo.pApplicationName = "PlaneAR";
  appInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
  appInfo.pEngineName = "No Engine";
  appInfo.engineVersion = VK_MAKE_VERSION(1, 0, 0);
  appInfo.apiVersion = VK_API_VERSION_1_0;

  VkInstanceCreateInfo createInfo{};
  createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
  createInfo.pApplicationInfo = &appInfo;

  createInfo.enabledExtensionCount = 0;

  createInfo.enabledLayerCount = 0;

  if (vkCreateInstance(&createInfo, nullptr, &instance) != VK_SUCCESS) {
    throw std::runtime_error("failed to create instance!");
  }
  else
  {
    LOGI("VK INSTANCE CREATED!");
  }

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

  vkDestroyInstance(instance, nullptr);
}
