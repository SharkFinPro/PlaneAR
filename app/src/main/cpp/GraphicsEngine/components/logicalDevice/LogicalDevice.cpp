#include "LogicalDevice.h"
#include "../instance/Instance.h"
#include "../physicalDevice/PhysicalDevice.h"
#include <array>
#include <set>
#include <stdexcept>

namespace ge {
  LogicalDevice::LogicalDevice(const std::shared_ptr<PhysicalDevice>& physicalDevice)
    : m_physicalDevice(physicalDevice)
  {
    createDevice();
  }

  LogicalDevice::~LogicalDevice()
  {
    vkDestroyDevice(m_device, nullptr);
  }

  std::shared_ptr<PhysicalDevice> LogicalDevice::getPhysicalDevice() const
  {
    return m_physicalDevice;
  }

  void LogicalDevice::waitIdle() const
  {
    vkDeviceWaitIdle(m_device);
  }

  uint32_t LogicalDevice::getMaxFramesInFlight() const
  {
    return m_maxFramesInFlight;
  }

  void LogicalDevice::createDevice()
  {
    auto queueFamilyIndices = m_physicalDevice->getQueueFamilies();

    std::vector<VkDeviceQueueCreateInfo> queueCreateInfos;
    std::set uniqueQueueFamilies = {
      queueFamilyIndices.graphicsFamily.value(),
      queueFamilyIndices.presentFamily.value()
    };

    float queuePriority = 1.0f;
    for (uint32_t queueFamily : uniqueQueueFamilies)
    {
      const VkDeviceQueueCreateInfo queueCreateInfo {
        .sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO,
        .queueFamilyIndex = queueFamily,
        .queueCount = 1,
        .pQueuePriorities = &queuePriority
      };

      queueCreateInfos.push_back(queueCreateInfo);
    }

    VkPhysicalDeviceFeatures2 deviceFeatures2 {
      .sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2,
      .features {
        .fillModeNonSolid = VK_TRUE,
        .samplerAnisotropy = VK_TRUE
      }
    };

    const VkDeviceCreateInfo createInfo {
      .sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO,
      .pNext = &deviceFeatures2,
      .queueCreateInfoCount = static_cast<uint32_t>(queueCreateInfos.size()),
      .pQueueCreateInfos = queueCreateInfos.data(),
      .enabledLayerCount = Instance::validationLayersEnabled() ? static_cast<uint32_t>(validationLayers.size()) : 0,
      .ppEnabledLayerNames = Instance::validationLayersEnabled() ? validationLayers.data() : nullptr,
      .enabledExtensionCount = static_cast<uint32_t>(deviceExtensions.size()),
      .ppEnabledExtensionNames = deviceExtensions.data()
    };

    m_device = m_physicalDevice->createLogicalDevice(createInfo);

    vkGetDeviceQueue(m_device, queueFamilyIndices.computeFamily.value(), 0, &m_computeQueue);
    vkGetDeviceQueue(m_device, queueFamilyIndices.graphicsFamily.value(), 0, &m_graphicsQueue);
    vkGetDeviceQueue(m_device, queueFamilyIndices.presentFamily.value(), 0, &m_presentQueue);
  }

  VkCommandPool LogicalDevice::createCommandPool(const VkCommandPoolCreateInfo& commandPoolCreateInfo) const
  {
    VkCommandPool commandPool = VK_NULL_HANDLE;

    if (vkCreateCommandPool(m_device, &commandPoolCreateInfo, nullptr, &commandPool) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to create command pool!");
    }

    return commandPool;
  }

  void LogicalDevice::destroyCommandPool(VkCommandPool& commandPool) const
  {
    vkDestroyCommandPool(m_device, commandPool, nullptr);

    commandPool = VK_NULL_HANDLE;
  }

  VkDescriptorPool LogicalDevice::createDescriptorPool(const VkDescriptorPoolCreateInfo& descriptorPoolCreateInfo) const
  {
    VkDescriptorPool descriptorPool = VK_NULL_HANDLE;

    if (vkCreateDescriptorPool(m_device, &descriptorPoolCreateInfo, nullptr, &descriptorPool) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to create descriptor pool!");
    }

    return descriptorPool;
  }

  void LogicalDevice::destroyDescriptorPool(VkDescriptorPool& descriptorPool) const
  {
    vkDestroyDescriptorPool(m_device, descriptorPool, nullptr);

    descriptorPool = VK_NULL_HANDLE;
  }

  VkSwapchainKHR LogicalDevice::createSwapchain(const VkSwapchainCreateInfoKHR &swapchainCreateInfo) const
  {
    VkSwapchainKHR swapchain = VK_NULL_HANDLE;

    if (vkCreateSwapchainKHR(m_device, &swapchainCreateInfo, nullptr, &swapchain) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to create swapchain!");
    }

    return swapchain;
  }

  void LogicalDevice::destroySwapchainKHR(VkSwapchainKHR& swapchain) const
  {
    vkDestroySwapchainKHR(m_device, swapchain, nullptr);

    swapchain = VK_NULL_HANDLE;
  }

  void LogicalDevice::getSwapchainImagesKHR(VkSwapchainKHR const& swapchain,
                                            uint32_t* swapchainImageCount,
                                            VkImage* swapchainImages) const
  {
    vkGetSwapchainImagesKHR(m_device, swapchain, swapchainImageCount, swapchainImages);
  }

  VkImageView LogicalDevice::createImageView(const VkImageViewCreateInfo& imageViewCreateInfo) const
  {
    VkImageView imageView = VK_NULL_HANDLE;

    if (vkCreateImageView(m_device, &imageViewCreateInfo, nullptr, &imageView) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to create image view!");
    }

    return imageView;
  }

  void LogicalDevice::destroyImageView(VkImageView &imageView) const
  {
    vkDestroyImageView(m_device, imageView, nullptr);

    imageView = VK_NULL_HANDLE;
  }

  void LogicalDevice::allocateCommandBuffers(const VkCommandBufferAllocateInfo& commandBufferAllocateInfo,
                                             VkCommandBuffer* commandBuffers) const
  {
    if (vkAllocateCommandBuffers(m_device, &commandBufferAllocateInfo, commandBuffers) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to allocate command buffers!");
    }
  }

  void LogicalDevice::freeCommandBuffers(VkCommandPool commandPool, uint32_t commandBufferCount,
                                         const VkCommandBuffer* commandBuffers) const
  {
    vkFreeCommandBuffers(m_device, commandPool, commandBufferCount, commandBuffers);
  }
} // ge