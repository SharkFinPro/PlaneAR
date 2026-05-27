#ifndef PLANEAR_COMMANDBUFFER_H
#define PLANEAR_COMMANDBUFFER_H

#include <vulkan/vulkan.h>
#include <functional>
#include <memory>
#include <vector>

namespace ge {

  class LogicalDevice;

  class CommandBuffer
  {
  public:
    explicit CommandBuffer(std::shared_ptr<LogicalDevice> logicalDevice);

    CommandBuffer(const std::shared_ptr<LogicalDevice>& logicalDevice, VkCommandPool commandPool);

    [[nodiscard]] VkCommandBuffer* getCommandBuffer();

    void resetCommandBuffer() const;

    void setCurrentFrame(uint32_t currentFrame);

    void beginRenderPass(const VkRenderPassBeginInfo& renderPassBeginInfo) const;

    void endRenderPass() const;

    virtual void record(const std::function<void()>& renderFunction) const;

    void setViewport(const VkViewport& viewport) const;

    void setScissor(const VkRect2D& scissor) const;

    void bindPipeline(VkPipelineBindPoint pipelineBindPoint, VkPipeline pipeline) const;

    void draw(uint32_t vertexCount, uint32_t instanceCount, uint32_t firstVertex, uint32_t firstInstance) const;

    void pushConstants(VkPipelineLayout layout,
                       VkShaderStageFlags stageFlags,
                       uint32_t offset,
                       uint32_t size,
                       const void* values) const;

    void bindDescriptorSets(VkPipelineBindPoint pipelineBindPoint,
                            VkPipelineLayout pipelineLayout,
                            uint32_t firstSet,
                            uint32_t descriptorSetCount,
                            const VkDescriptorSet* descriptorSets) const;

    void clearAttachments(const std::vector<VkClearAttachment>& clearAttachments,
                          const std::vector<VkClearRect>& clearRects) const;

    void pipelineBarrier(VkPipelineStageFlags srcStageMask,
                         VkPipelineStageFlags dstStageMask,
                         VkDependencyFlags dependencyFlags,
                         const std::vector<VkMemoryBarrier>& memoryBarriers,
                         const std::vector<VkBufferMemoryBarrier>& bufferMemoryBarriers,
                         const std::vector<VkImageMemoryBarrier>& imageMemoryBarriers) const;

    void copyImageToBuffer(VkImage srcImage,
                           VkImageLayout srcImageLayout,
                           VkBuffer dstBuffer,
                           const std::vector<VkBufferImageCopy>& regions) const;

  protected:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    std::vector<VkCommandBuffer> m_commandBuffers;

    uint32_t m_currentFrame = 0;

    virtual void allocateCommandBuffers(VkCommandPool commandPool);
  };

} // ge

#endif //PLANEAR_COMMANDBUFFER_H
