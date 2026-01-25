#include "PhysicalDevice.h"
#include "../instance/Instance.h"
#include "../surface/Surface.h"
#include <set>
#include <stdexcept>

namespace ge {
  PhysicalDevice::PhysicalDevice(const std::shared_ptr<Instance>& instance,
                                 const std::shared_ptr<Surface>& surface)
    : m_surface(surface)
  {
    pickPhysicalDevice(instance);

    m_queueFamilyIndices = findQueueFamilies(m_physicalDevice);

    updateSwapChainSupportDetails();
  }

  QueueFamilyIndices PhysicalDevice::getQueueFamilies() const
  {
    return m_queueFamilyIndices;
  }

  SwapChainSupportDetails PhysicalDevice::getSwapChainSupport() const
  {
    return m_swapChainSupportDetails;
  }

  VkSampleCountFlagBits PhysicalDevice::getMsaaSamples() const
  {
    return m_msaaSamples;
  }

  uint32_t PhysicalDevice::findMemoryType(uint32_t typeFilter,
                                          const VkMemoryPropertyFlags& properties) const
  {
    VkPhysicalDeviceMemoryProperties memoryProperties;
    vkGetPhysicalDeviceMemoryProperties(m_physicalDevice, &memoryProperties);

    for (uint32_t i = 0; i < memoryProperties.memoryTypeCount; i++)
    {
      if (typeFilter & (1 << i) && (memoryProperties.memoryTypes[i].propertyFlags & properties) == properties)
      {
        return i;
      }
    }

    throw std::runtime_error("failed to find suitable memory type!");
  }

  void PhysicalDevice::updateSwapChainSupportDetails()
  {
    m_swapChainSupportDetails = querySwapChainSupport(m_physicalDevice);
  }

  VkFormatProperties PhysicalDevice::getFormatProperties(VkFormat format) const
  {
    VkFormatProperties formatProperties{};
    vkGetPhysicalDeviceFormatProperties(m_physicalDevice, format, &formatProperties);

    return formatProperties;
  }

  VkPhysicalDeviceProperties PhysicalDevice::getDeviceProperties() const
  {
    VkPhysicalDeviceProperties deviceProperties{};
    vkGetPhysicalDeviceProperties(m_physicalDevice, &deviceProperties);

    return deviceProperties;
  }

  VkDevice PhysicalDevice::createLogicalDevice(const VkDeviceCreateInfo& deviceCreateInfo) const
  {
    VkDevice logicalDevice;

    if (vkCreateDevice(m_physicalDevice, &deviceCreateInfo, nullptr, &logicalDevice) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to create logical device!");
    }

    return logicalDevice;
  }

  VkFormat PhysicalDevice::findDepthFormat() const
  {
    return findSupportedFormat(
      {VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT},
      VK_IMAGE_TILING_OPTIMAL,
      VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT
    );
  }

  VkFormat PhysicalDevice::findSupportedFormat(const std::vector<VkFormat>& candidates,
                                               VkImageTiling tiling,
                                               VkFormatFeatureFlags features) const
  {
    for (const auto& format : candidates)
    {
      const VkFormatProperties formatProperties = getFormatProperties(format);

      if ((tiling == VK_IMAGE_TILING_LINEAR && (formatProperties.linearTilingFeatures & features) == features) ||
          (tiling == VK_IMAGE_TILING_OPTIMAL && (formatProperties.optimalTilingFeatures & features) == features))
      {
        return format;
      }
    }

    throw std::runtime_error("failed to find supported format!");
  }

  void PhysicalDevice::pickPhysicalDevice(const std::shared_ptr<Instance>& instance)
  {
    for (const auto& device : instance->getPhysicalDevices())
    {
      if (isDeviceSuitable(device))
      {
        m_physicalDevice = device;
        m_msaaSamples = getMaxUsableSampleCount();
        break;
      }
    }

    if (m_physicalDevice == VK_NULL_HANDLE)
    {
      throw std::runtime_error("failed to find a suitable GPU!");
    }
  }

  bool PhysicalDevice::isDeviceSuitable(VkPhysicalDevice device)
  {
    QueueFamilyIndices indices = findQueueFamilies(device);

    bool extensionsSupported = checkDeviceExtensionSupport(device);

    bool swapChainAdequate = false;
    if (extensionsSupported)
    {
      SwapChainSupportDetails swapChainSupport = querySwapChainSupport(device);
      swapChainAdequate = !swapChainSupport.formats.empty() && !swapChainSupport.presentModes.empty();
    }

    VkPhysicalDeviceFeatures supportedFeatures;
    vkGetPhysicalDeviceFeatures(device, &supportedFeatures);

    return indices.isComplete() && extensionsSupported && swapChainAdequate && supportedFeatures.samplerAnisotropy;
  }

  QueueFamilyIndices PhysicalDevice::findQueueFamilies(VkPhysicalDevice device)
  {
    QueueFamilyIndices indices;

    uint32_t queueFamilyCount = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, nullptr);

    std::vector<VkQueueFamilyProperties> queueFamilies(queueFamilyCount);
    vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, queueFamilies.data());

    int i = 0;
    for (const auto& queueFamily : queueFamilies)
    {
      if (queueFamily.queueFlags & VK_QUEUE_GRAPHICS_BIT)
      {
        indices.graphicsFamily = i;
      }

      if (queueFamily.queueFlags & VK_QUEUE_COMPUTE_BIT)
      {
        indices.computeFamily = i;
      }

      VkBool32 presentSupport = false;
      vkGetPhysicalDeviceSurfaceSupportKHR(device, i, m_surface->getSurface(), &presentSupport);

      if (presentSupport)
      {
        indices.presentFamily = i;
      }

      if (indices.isComplete())
      {
        break;
      }

      i++;
    }

    return indices;
  }

  bool PhysicalDevice::checkDeviceExtensionSupport(VkPhysicalDevice device)
  {
    uint32_t extensionCount;
    vkEnumerateDeviceExtensionProperties(device, nullptr, &extensionCount, nullptr);

    std::vector<VkExtensionProperties> availableExtensions(extensionCount);
    vkEnumerateDeviceExtensionProperties(device, nullptr, &extensionCount, availableExtensions.data());

    std::set<std::string> requiredExtensions(deviceExtensions.begin(), deviceExtensions.end());

    for (const auto& extension : availableExtensions)
    {
      requiredExtensions.erase(extension.extensionName);
    }

    return requiredExtensions.empty();
  }

  SwapChainSupportDetails PhysicalDevice::querySwapChainSupport(VkPhysicalDevice device)
  {
    SwapChainSupportDetails details;

    const auto sufaceHandle = m_surface->getSurface();

    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, sufaceHandle, &details.capabilities);

    uint32_t formatCount;
    vkGetPhysicalDeviceSurfaceFormatsKHR(device, sufaceHandle, &formatCount, nullptr);

    if (formatCount != 0)
    {
      details.formats.resize(formatCount);
      vkGetPhysicalDeviceSurfaceFormatsKHR(device, sufaceHandle, &formatCount, details.formats.data());
    }

    uint32_t presentModeCount;
    vkGetPhysicalDeviceSurfacePresentModesKHR(device, sufaceHandle, &presentModeCount, nullptr);

    if (presentModeCount != 0)
    {
      details.presentModes.resize(presentModeCount);
      vkGetPhysicalDeviceSurfacePresentModesKHR(device, sufaceHandle, &presentModeCount, details.presentModes.data());
    }

    return details;
  }

  VkSampleCountFlagBits PhysicalDevice::getMaxUsableSampleCount() const
  {
    VkPhysicalDeviceProperties physicalDeviceProperties;
    vkGetPhysicalDeviceProperties(m_physicalDevice, &physicalDeviceProperties);

    const VkSampleCountFlags counts = physicalDeviceProperties.limits.framebufferColorSampleCounts &
                                      physicalDeviceProperties.limits.framebufferDepthSampleCounts;

    constexpr std::array<VkSampleCountFlagBits, 6> sampleCounts {
        VK_SAMPLE_COUNT_64_BIT,
        VK_SAMPLE_COUNT_32_BIT,
        VK_SAMPLE_COUNT_16_BIT,
        VK_SAMPLE_COUNT_8_BIT,
        VK_SAMPLE_COUNT_4_BIT,
        VK_SAMPLE_COUNT_2_BIT
    };

    for (const VkSampleCountFlagBits count : sampleCounts)
    {
      if (counts & count)
      {
        return count;
      }
    }

    return VK_SAMPLE_COUNT_1_BIT;
  }
} // ge