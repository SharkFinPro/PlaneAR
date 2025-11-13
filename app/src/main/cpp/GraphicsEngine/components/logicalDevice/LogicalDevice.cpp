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

  VkQueue LogicalDevice::getGraphicsQueue() const
  {
    return m_graphicsQueue;
  }

  VkQueue LogicalDevice::getPresentQueue() const
  {
    return m_presentQueue;
  }

  VkQueue LogicalDevice::getComputeQueue() const
  {
    return m_computeQueue;
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

  VkSwapchainKHR LogicalDevice::createSwapchain(const VkSwapchainCreateInfoKHR& swapchainCreateInfo) const
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

  void LogicalDevice::getSwapchainImagesKHR(const VkSwapchainKHR& swapchain,
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

  void LogicalDevice::destroyImageView(VkImageView& imageView) const
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

  VkRenderPass LogicalDevice::createRenderPass(const VkRenderPassCreateInfo& renderPassCreateInfo) const
  {
    VkRenderPass renderPass = VK_NULL_HANDLE;

    if (vkCreateRenderPass(m_device, &renderPassCreateInfo, nullptr,& renderPass) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to create render pass!");
    }

    return renderPass;
  }

  void LogicalDevice::destroyRenderPass(VkRenderPass& renderPass) const
  {
    vkDestroyRenderPass(m_device, renderPass, nullptr);

    renderPass = VK_NULL_HANDLE;
  }

  VkImage LogicalDevice::createImage(const VkImageCreateInfo& imageCreateInfo) const
  {
    VkImage image = VK_NULL_HANDLE;

    if (vkCreateImage(m_device, &imageCreateInfo, nullptr, &image) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to create image!");
    }

    return image;
  }

  void LogicalDevice::destroyImage(VkImage& image) const
  {
    vkDestroyImage(m_device, image, nullptr);

    image = VK_NULL_HANDLE;
  }

  VkMemoryRequirements LogicalDevice::getImageMemoryRequirements(const VkImage& image) const
  {
    VkMemoryRequirements memoryRequirements{};

    vkGetImageMemoryRequirements(m_device, image, &memoryRequirements);

    return memoryRequirements;
  }

  void LogicalDevice::bindImageMemory(const VkImage& image,
                                      const VkDeviceMemory& deviceMemory,
                                      VkDeviceSize memoryOffset) const
  {
    vkBindImageMemory(m_device, image, deviceMemory, memoryOffset);
  }

  void LogicalDevice::allocateMemory(const VkMemoryAllocateInfo& memoryAllocateInfo,
                                     VkDeviceMemory& deviceMemory) const
  {
    if (vkAllocateMemory(m_device, &memoryAllocateInfo, nullptr, &deviceMemory) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to allocate memory!");
    }
  }

  void LogicalDevice::freeMemory(VkDeviceMemory& memory) const
  {
    if (memory == VK_NULL_HANDLE)
    {
      return;
    }

    vkFreeMemory(m_device, memory, nullptr);

    memory = VK_NULL_HANDLE;
  }

  VkFramebuffer LogicalDevice::createFramebuffer(const VkFramebufferCreateInfo& framebufferCreateInfo) const
  {
    VkFramebuffer framebuffer = VK_NULL_HANDLE;

    if (vkCreateFramebuffer(m_device, &framebufferCreateInfo, nullptr, &framebuffer) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to create framebuffer!");
    }

    return framebuffer;
  }

  void LogicalDevice::destroyFramebuffer(VkFramebuffer& framebuffer) const
  {
    vkDestroyFramebuffer(m_device, framebuffer, nullptr);

    framebuffer = VK_NULL_HANDLE;
  }
} // ge