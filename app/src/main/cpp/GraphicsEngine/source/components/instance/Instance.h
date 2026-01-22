#ifndef PLANEAR_INSTANCE_H
#define PLANEAR_INSTANCE_H

#include <vulkan/vulkan.h>
#include <array>
#include <vector>

struct ANativeWindow;

namespace ge {

  constexpr std::array<const char*, 1> validationLayers {
      "VK_LAYER_KHRONOS_validation"
  };

  class Instance
  {
  public:
    Instance();
    ~Instance();

    void createDebugUtilsMessenger();

    void destroyDebugUtilsMessenger();

    static bool validationLayersEnabled();

    [[nodiscard]] VkSurfaceKHR createSurface(ANativeWindow* window) const;

    void destroySurface(VkSurfaceKHR& surface) const;

    [[nodiscard]] std::vector<VkPhysicalDevice> getPhysicalDevices() const;

  private:
    VkInstance m_instance = VK_NULL_HANDLE;

    VkDebugUtilsMessengerEXT m_debugMessenger = VK_NULL_HANDLE;

    static bool checkValidationLayerSupport();

    static std::vector<const char*> getRequiredExtensions();
  };

} // ge

#endif //PLANEAR_INSTANCE_H
