#ifndef PLANEAR_DEBUGMESSENGER_H
#define PLANEAR_DEBUGMESSENGER_H

#include <vulkan/vulkan.h>

namespace ge {

  class DebugMessenger {
  public:
    static VKAPI_ATTR VkBool32 VKAPI_CALL debugCallback(
      VkDebugUtilsMessageSeverityFlagBitsEXT messageSeverity,
      VkDebugUtilsMessageTypeFlagsEXT messageType,
      const VkDebugUtilsMessengerCallbackDataEXT* pCallbackData,
      void* pUserData);

    static void populateCreateInfo(VkDebugUtilsMessengerCreateInfoEXT& createInfo);
  };

} // ge

#endif //PLANEAR_DEBUGMESSENGER_H
