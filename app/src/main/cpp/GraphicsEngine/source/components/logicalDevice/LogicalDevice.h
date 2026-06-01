#ifndef PLANEAR_LOGICALDEVICE_H
#define PLANEAR_LOGICALDEVICE_H

#include <vulkan/vulkan.h>
#include <vulkan/vulkan_android.h>
#include <functional>
#include <memory>
#include <vector>

namespace ge {

  class CommandBuffer;
  class PhysicalDevice;
  class Swapchain;

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

    void waitForGraphicsFences(uint32_t currentFrame) const;

    void waitForMousePickingFences(uint32_t currentFrame) const;

    void resetGraphicsFences(uint32_t currentFrame) const;

    void resetMousePickingFences(uint32_t currentFrame) const;

    VkResult acquireNextImage(uint32_t currentFrame,
                              const std::shared_ptr<Swapchain>& swapchain,
                              uint32_t* imageIndex) const;

    void submitGraphicsQueue(uint32_t currentFrame,
                             uint32_t imageIndex,
                             const std::shared_ptr<CommandBuffer>& commandBuffer);

    void submitMousePickingGraphicsQueue(uint32_t currentFrame,
                                         const VkCommandBuffer* commandBuffer) const;

    VkResult queuePresent(uint32_t imageIndex,
                          const std::shared_ptr<Swapchain>& swapchain) const;

    void createSyncObjects(const std::shared_ptr<Swapchain>& swapchain);

    [[nodiscard]] VkPipelineLayout createPipelineLayout(const VkPipelineLayoutCreateInfo& pipelineLayoutCreateInfo) const;

    void destroyPipelineLayout(VkPipelineLayout& pipelineLayout) const;

    [[nodiscard]] VkPipeline createPipeline(const VkGraphicsPipelineCreateInfo& graphicsPipelineCreateInfo) const;

    void destroyPipeline(VkPipeline& pipeline) const;

    [[nodiscard]] VkShaderModule createShaderModule(const VkShaderModuleCreateInfo& shaderModuleCreateInfo) const;

    void destroyShaderModule(VkShaderModule& shaderModule) const;

    [[nodiscard]] VkBuffer createBuffer(const VkBufferCreateInfo& bufferCreateInfo) const;

    void destroyBuffer(VkBuffer& buffer) const;

    [[nodiscard]] VkMemoryRequirements getBufferMemoryRequirements(const VkBuffer& buffer) const;

    void bindBufferMemory(const VkBuffer& buffer,
                          const VkDeviceMemory& deviceMemory,
                          VkDeviceSize memoryOffset = 0) const;

    void doMappedMemoryOperation(VkDeviceMemory deviceMemory,
                                 const std::function<void(void* data)>& operationFunction) const;

    void mapMemory(const VkDeviceMemory& memory,
                   VkDeviceSize offset,
                   VkDeviceSize size,
                   VkMemoryMapFlags flags,
                   void** data) const;

    void unmapMemory(const VkDeviceMemory& memory) const;

    [[nodiscard]] VkDescriptorSetLayout createDescriptorSetLayout(const VkDescriptorSetLayoutCreateInfo& descriptorSetLayoutCreateInfo) const;

    void destroyDescriptorSetLayout(VkDescriptorSetLayout& descriptorSetLayout) const;

    void allocateDescriptorSets(const VkDescriptorSetAllocateInfo& descriptorSetAllocateInfo,
                                VkDescriptorSet* descriptorSets) const;

    void updateDescriptorSets(uint32_t descriptorWriteCount,
                              const VkWriteDescriptorSet* descriptorWrites) const;

    [[nodiscard]] VkSampler createSampler(const VkSamplerCreateInfo& samplerCreateInfo) const;

    void destroySampler(VkSampler& sampler) const;

    void destroySamplerYcbcrConversion(VkSamplerYcbcrConversion& ycbcrConversion) const;

    [[nodiscard]] VkSamplerYcbcrConversion createSamplerYcbcrConversion(VkSamplerYcbcrConversionCreateInfo& ycbcrInfo);

    void getAndroidHardwareBufferPropertiesANDROID(const struct AHardwareBuffer* buffer,
                                                   VkAndroidHardwareBufferPropertiesANDROID* pProperties);

  private:
    std::shared_ptr<PhysicalDevice> m_physicalDevice;

    VkDevice m_device = VK_NULL_HANDLE;

    VkQueue m_graphicsQueue = VK_NULL_HANDLE;
    VkQueue m_presentQueue = VK_NULL_HANDLE;
    VkQueue m_computeQueue = VK_NULL_HANDLE;

    std::vector<VkSemaphore> m_swapchainImageAvailableSemaphores;

    std::vector<VkSemaphore> m_swapchainRenderFinishedSemaphores;

    std::vector<VkFence> m_swapchainInFlightFences;

    std::vector<VkFence> m_mousePickingInFlightFences;

    uint8_t m_maxFramesInFlight = 2;

    uint8_t m_swapchainImageCount;

    void createDevice();

    void destroySyncObjects();
  };

} // ge

#endif //PLANEAR_LOGICALDEVICE_H
