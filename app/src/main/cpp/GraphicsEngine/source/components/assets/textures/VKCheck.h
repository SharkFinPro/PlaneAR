#pragma once

#include <vulkan/vulkan.h>
#include <cstdlib>
#include <cstdio>

#ifdef __ANDROID__
#include <android/log.h>
#define VK_LOG_TAG "Vulkan"
#define VK_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, VK_LOG_TAG, __VA_ARGS__)
#else
#define VK_LOGE(...) std::fprintf(stderr, __VA_ARGS__)
#endif

static inline void vk_check_impl(VkResult result, const char* file, int line)
{
  if (result == VK_SUCCESS)
    return;

  VK_LOGE("Vulkan Error: %d at %s:%d", result, file, line);

  // Optional: add more readable errors
  switch (result)
  {
    case VK_ERROR_DEVICE_LOST:
      VK_LOGE("VK_ERROR_DEVICE_LOST");
      break;
    case VK_ERROR_OUT_OF_DEVICE_MEMORY:
      VK_LOGE("VK_ERROR_OUT_OF_DEVICE_MEMORY");
      break;
    case VK_ERROR_OUT_OF_HOST_MEMORY:
      VK_LOGE("VK_ERROR_OUT_OF_HOST_MEMORY");
      break;
    case VK_ERROR_INITIALIZATION_FAILED:
      VK_LOGE("VK_ERROR_INITIALIZATION_FAILED");
      break;
    default:
      VK_LOGE("Unknown VkResult");
      break;
  }

#if defined(DEBUG) || defined(_DEBUG)
  std::abort();   // crash immediately in debug
#else
  // in release you may want graceful handling instead
  std::abort();
#endif
}

#define VK_CHECK(x) vk_check_impl((x), __FILE__, __LINE__)