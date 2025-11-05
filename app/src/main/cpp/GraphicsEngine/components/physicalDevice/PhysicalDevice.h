#ifndef PLANEAR_PHYSICALDEVICE_H
#define PLANEAR_PHYSICALDEVICE_H

#include <vulkan/vulkan.h>
#include <optional>
#include <array>
#include <vector>
#include <memory>

namespace ge {

  struct Instance;
  struct Surface;

  constexpr std::array<const char*, 1> deviceExtensions {
    VK_KHR_SWAPCHAIN_EXTENSION_NAME
  };

  struct QueueFamilyIndices {
    std::optional<uint32_t> graphicsFamily;
    std::optional<uint32_t> presentFamily;
    std::optional<uint32_t> computeFamily;

    [[nodiscard]] bool isComplete() const
    {
      return graphicsFamily.has_value() &&
             presentFamily.has_value() &&
             computeFamily.has_value();
    }
  };

  struct SwapChainSupportDetails {
    VkSurfaceCapabilitiesKHR capabilities {};
    std::vector<VkSurfaceFormatKHR> formats;
    std::vector<VkPresentModeKHR> presentModes;
  };

  class PhysicalDevice
  {
  public:
    PhysicalDevice(const std::shared_ptr<Instance>& instance, const std::shared_ptr<Surface>& surface);

  private:
    VkPhysicalDevice m_physicalDevice = VK_NULL_HANDLE;

    VkSampleCountFlagBits m_msaaSamples = VK_SAMPLE_COUNT_1_BIT;

    void pickPhysicalDevice(const std::shared_ptr<Instance>& instance,
                            const std::shared_ptr<Surface>& surface);

    static bool isDeviceSuitable(VkPhysicalDevice device, const std::shared_ptr<Surface>& surface) ;

    static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device,
                                         const std::shared_ptr<Surface>& surface) ;

    static bool checkDeviceExtensionSupport(VkPhysicalDevice device);

    static SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device,
                                                         const std::shared_ptr<Surface>& surface);

    [[nodiscard]] VkSampleCountFlagBits getMaxUsableSampleCount() const;
  };

} // ge

#endif //PLANEAR_PHYSICALDEVICE_H
