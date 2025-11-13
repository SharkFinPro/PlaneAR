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

    [[nodiscard]] VkQueue getGraphicsQueue() const;

    [[nodiscard]] VkQueue getPresentQueue() const;

    [[nodiscard]] VkQueue getComputeQueue() const;

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

    void allocateCommandBuffers(const VkCommandBufferAllocateInfo& commandBufferAllocateInfo,
                                VkCommandBuffer* commandBuffers) const;

    void freeCommandBuffers(VkCommandPool commandPool,
                            uint32_t commandBufferCount,
                            const VkCommandBuffer* commandBuffers) const;

    [[nodiscard]] VkRenderPass createRenderPass(const VkRenderPassCreateInfo& renderPassCreateInfo) const;

    void destroyRenderPass(VkRenderPass& renderPass) const;

    [[nodiscard]] VkImage createImage(const VkImageCreateInfo& imageCreateInfo) const;

    void destroyImage(VkImage& image) const;

    [[nodiscard]] VkMemoryRequirements getImageMemoryRequirements(const VkImage& image) const;

    void bindImageMemory(const VkImage& image, const VkDeviceMemory& deviceMemory, VkDeviceSize memoryOffset = 0) const;

    void allocateMemory(const VkMemoryAllocateInfo& memoryAllocateInfo, VkDeviceMemory& deviceMemory) const;

    void freeMemory(VkDeviceMemory& memory) const;

    [[nodiscard]] VkFramebuffer createFramebuffer(const VkFramebufferCreateInfo& framebufferCreateInfo) const;

    void destroyFramebuffer(VkFramebuffer& framebuffer) const;

  private:
    std::shared_ptr<PhysicalDevice> m_physicalDevice;

    VkDevice m_device = VK_NULL_HANDLE;

    VkQueue m_graphicsQueue = VK_NULL_HANDLE;
    VkQueue m_presentQueue = VK_NULL_HANDLE;
    VkQueue m_computeQueue = VK_NULL_HANDLE;

    std::vector<VkSemaphore> m_swapchainImageAvailableSemaphores;

    std::vector<VkSemaphore> m_swapchainRenderFinishedSemaphores;

    std::vector<VkFence> m_swapchainInFlightFences;

    uint8_t m_maxFramesInFlight = 2;

    void createDevice();

    void createSyncObjects();
  };

} // ge

#endif //PLANEAR_LOGICALDEVICE_H
