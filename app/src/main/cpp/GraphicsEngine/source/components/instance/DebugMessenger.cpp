#include "DebugMessenger.h"
#include "../../../Logger.h"

namespace ge {
  VkBool32 DebugMessenger::debugCallback(VkDebugUtilsMessageSeverityFlagBitsEXT messageSeverity,
                                         VkDebugUtilsMessageTypeFlagsEXT messageType,
                                         const VkDebugUtilsMessengerCallbackDataEXT* pCallbackData,
                                         void *pUserData)
  {
    switch (messageSeverity)
    {
      case VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT:
        LOGV("[verbose] validation layer: %s", pCallbackData->pMessage);
        break;
      case VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT:
        LOGI("[info] validation layer: %s", pCallbackData->pMessage);
        break;
      case VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT:
        LOGW("[warning] validation layer: %s", pCallbackData->pMessage);
        break;
      case VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT:
        LOGE("[error] validation layer: %s", pCallbackData->pMessage);
        break;
      default:
        break;
    }

    return VK_FALSE;
  }

  void DebugMessenger::populateCreateInfo(VkDebugUtilsMessengerCreateInfoEXT& createInfo)
  {
    createInfo = {
      .sType = VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT,
      .messageSeverity = VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                         VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                         VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT |
                         VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT,
      .messageType = VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                     VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                     VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT,
      .pfnUserCallback = debugCallback
    };
  }
} // ge