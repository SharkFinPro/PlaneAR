#ifndef PLANEAR_LOGICALDEVICE_H
#define PLANEAR_LOGICALDEVICE_H

#include <vulkan/vulkan.h>
#include <functional>
#include <memory>
#include <vector>

namespace ge {

  class PhysicalDevice;

  class LogicalDevice
  {
  public:
    explicit LogicalDevice(const std::shared_ptr<PhysicalDevice>& physicalDevice);
    ~LogicalDevice();

    [[nodiscard]] std::shared_ptr<PhysicalDevice> getPhysicalDevice() const;

    void waitIdle() const;

    [[nodiscard]] uint32_t getMaxFramesInFlight() const;

    [[nodiscard]] VkCommandPool createCommandPool(const VkCommandPoolCreateInfo& commandPoolCreateInfo) const;

    void destroyCommandPool(VkCommandPool& commandPool) const;

    [[nodiscard]] VkDescriptorPool createDescriptorPool(const VkDescriptorPoolCreateInfo& descriptorPoolCreateInfo) const;

    void destroyDescriptorPool(VkDescriptorPool& descriptorPool) const;

    [[nodiscard]] VkSwapchainKHR createSwapchain(const VkSwapchainCreateInfoKHR& swapchainCreateInfo) const;

    void destroySwapchainKHR(VkSwapchainKHR& swapchain) const;

    void getSwapchainImagesKHR(const VkSwapchainKHR& swapchain, uint32_t* swapchainImageCount, VkImage* swapchainImages) const;

    [[nodiscard]] VkImageView createImageView(const VkImageViewCreateInfo& imageViewCreateInfo) const;

    void destroyImageView(VkImageView& imageView) const;

  private:
    std::shared_ptr<PhysicalDevice> m_physicalDevice;

    VkDevice m_device = VK_NULL_HANDLE;

    VkQueue m_graphicsQueue = VK_NULL_HANDLE;
    VkQueue m_presentQueue = VK_NULL_HANDLE;
    VkQueue m_computeQueue = VK_NULL_HANDLE;

    uint8_t m_maxFramesInFlight = 2;

    void createDevice();
  };

} // ge

#endif //PLANEAR_LOGICALDEVICE_H
