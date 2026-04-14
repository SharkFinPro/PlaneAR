#ifndef PLANEAR_PHYSICALDEVICE_H
#define PLANEAR_PHYSICALDEVICE_H

#include <vulkan/vulkan.h>
#include <vulkan/vulkan_android.h>
#include <optional>
#include <array>
#include <vector>
#include <memory>

namespace ge {

  struct Instance;
  struct Surface;

  constexpr std::array deviceExtensions {
    VK_KHR_SWAPCHAIN_EXTENSION_NAME,

    VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME,
    VK_EXT_QUEUE_FAMILY_FOREIGN_EXTENSION_NAME,
    VK_ANDROID_EXTERNAL_MEMORY_ANDROID_HARDWARE_BUFFER_EXTENSION_NAME,
    VK_KHR_SAMPLER_YCBCR_CONVERSION_EXTENSION_NAME,
    VK_KHR_BIND_MEMORY_2_EXTENSION_NAME,
    VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME
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

    [[nodiscard]] QueueFamilyIndices getQueueFamilies() const;

    [[nodiscard]] SwapChainSupportDetails getSwapChainSupport() const;

    [[nodiscard]] VkSampleCountFlagBits getMsaaSamples() const;

    [[nodiscard]] uint32_t findMemoryType(uint32_t typeFilter, const VkMemoryPropertyFlags& properties) const;

    void updateSwapChainSupportDetails();

    [[nodiscard]] VkFormatProperties getFormatProperties(VkFormat format) const;

    [[nodiscard]] VkPhysicalDeviceProperties getDeviceProperties() const;

    [[nodiscard]] VkDevice createLogicalDevice(const VkDeviceCreateInfo& deviceCreateInfo) const;

    [[nodiscard]] VkFormat findDepthFormat() const;

    [[nodiscard]] VkFormat findSupportedFormat(const std::vector<VkFormat>& candidates,
                                               VkImageTiling tiling,
                                               VkFormatFeatureFlags features) const;

  private:
    VkPhysicalDevice m_physicalDevice = VK_NULL_HANDLE;

    std::shared_ptr<Surface> m_surface;

    VkSampleCountFlagBits m_msaaSamples = VK_SAMPLE_COUNT_1_BIT;

    QueueFamilyIndices m_queueFamilyIndices;

    SwapChainSupportDetails m_swapChainSupportDetails;

    void pickPhysicalDevice(const std::shared_ptr<Instance>& instance);

    bool isDeviceSuitable(VkPhysicalDevice device);

    [[nodiscard]] QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device);

    static bool checkDeviceExtensionSupport(VkPhysicalDevice device);

    SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device);

    [[nodiscard]] VkSampleCountFlagBits getMaxUsableSampleCount() const;
  };

} // ge

#endif //PLANEAR_PHYSICALDEVICE_H
