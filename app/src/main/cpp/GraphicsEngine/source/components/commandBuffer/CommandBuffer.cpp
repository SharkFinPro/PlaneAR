#include "CommandBuffer.h"
#include "../logicalDevice/LogicalDevice.h"

namespace ge {
  CommandBuffer::CommandBuffer(std::shared_ptr<LogicalDevice> logicalDevice)
    : m_logicalDevice(std::move(logicalDevice))
  {}

  CommandBuffer::CommandBuffer(const std::shared_ptr<LogicalDevice>& logicalDevice,
                               VkCommandPool commandPool)
    : m_logicalDevice(logicalDevice)
  {
    CommandBuffer::allocateCommandBuffers(commandPool);
  }

  VkCommandBuffer* CommandBuffer::getCommandBuffer()
  {
    return &m_commandBuffers[m_currentFrame];
  }

  void CommandBuffer::resetCommandBuffer() const
  {
    vkResetCommandBuffer(m_commandBuffers[m_currentFrame], 0);
  }

  void CommandBuffer::setCurrentFrame(uint32_t currentFrame)
  {
    m_currentFrame = currentFrame;
  }

  void CommandBuffer::allocateCommandBuffers(VkCommandPool commandPool)
  {
    m_commandBuffers.resize(m_logicalDevice->getMaxFramesInFlight());

    const VkCommandBufferAllocateInfo allocInfo {
      .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO,
      .commandPool = commandPool,
      .level = VK_COMMAND_BUFFER_LEVEL_PRIMARY,
      .commandBufferCount = static_cast<uint32_t>(m_commandBuffers.size())
    };

    m_logicalDevice->allocateCommandBuffers(allocInfo, m_commandBuffers.data());
  }

  void CommandBuffer::beginRenderPass(const VkRenderPassBeginInfo& renderPassBeginInfo) const
  {
    vkCmdBeginRenderPass(m_commandBuffers[m_currentFrame], &renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
  }

  void CommandBuffer::endRenderPass() const
  {
    vkCmdEndRenderPass(m_commandBuffers[m_currentFrame]);
  }

  void CommandBuffer::record(const std::function<void()>& renderFunction) const
  {
    constexpr VkCommandBufferBeginInfo beginInfo {
      .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO
    };

    if (vkBeginCommandBuffer(m_commandBuffers[m_currentFrame], &beginInfo) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to begin recording command buffer!");
    }

    renderFunction();

    if (vkEndCommandBuffer(m_commandBuffers[m_currentFrame]) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to record command buffer!");
    }
  }

  void CommandBuffer::setViewport(const VkViewport& viewport) const
  {
    vkCmdSetViewport(m_commandBuffers[m_currentFrame], 0, 1, &viewport);
  }

  void CommandBuffer::setScissor(const VkRect2D& scissor) const
  {
    vkCmdSetScissor(m_commandBuffers[m_currentFrame], 0, 1, &scissor);
  }

  void CommandBuffer::bindPipeline(VkPipelineBindPoint pipelineBindPoint,
                                   VkPipeline pipeline) const
  {
    vkCmdBindPipeline(m_commandBuffers[m_currentFrame], pipelineBindPoint, pipeline);
  }

  void CommandBuffer::draw(uint32_t vertexCount,
                           uint32_t instanceCount,
                           uint32_t firstVertex,
                           uint32_t firstInstance) const
  {
    vkCmdDraw(m_commandBuffers[m_currentFrame], vertexCount, instanceCount, firstVertex, firstInstance);
  }

  void CommandBuffer::pushConstants(VkPipelineLayout layout,
                                    VkShaderStageFlags stageFlags,
                                    uint32_t offset,
                                    uint32_t size,
                                    const void* values) const
  {
    vkCmdPushConstants(m_commandBuffers[m_currentFrame], layout, stageFlags, offset, size, values);
  }

  void CommandBuffer::bindDescriptorSets(VkPipelineBindPoint pipelineBindPoint,
                                         VkPipelineLayout pipelineLayout,
                                         uint32_t firstSet,
                                         uint32_t descriptorSetCount,
                                         const VkDescriptorSet* descriptorSets) const
  {
    vkCmdBindDescriptorSets(
      m_commandBuffers[m_currentFrame],
      pipelineBindPoint,
      pipelineLayout,
      firstSet,
      descriptorSetCount,
      descriptorSets,
      0,
      nullptr
    );
  }

  void CommandBuffer::clearAttachments(const std::vector<VkClearAttachment>& clearAttachments,
                                       const std::vector<VkClearRect>& clearRects) const
  {
    vkCmdClearAttachments(
      m_commandBuffers[m_currentFrame],
      clearAttachments.size(),
      clearAttachments.data(),
      clearRects.size(),
      clearRects.data()
    );
  }

  void CommandBuffer::pipelineBarrier(const VkPipelineStageFlags srcStageMask,
                                      const VkPipelineStageFlags dstStageMask,
                                      const VkDependencyFlags dependencyFlags,
                                      const std::vector<VkMemoryBarrier>& memoryBarriers,
                                      const std::vector<VkBufferMemoryBarrier>& bufferMemoryBarriers,
                                      const std::vector<VkImageMemoryBarrier>& imageMemoryBarriers) const
  {
    vkCmdPipelineBarrier(
      m_commandBuffers[m_currentFrame],
      srcStageMask,
      dstStageMask,
      dependencyFlags,
      memoryBarriers.size(),
      memoryBarriers.data(),
      bufferMemoryBarriers.size(),
      bufferMemoryBarriers.data(),
      imageMemoryBarriers.size(),
      imageMemoryBarriers.data()
    );
  }

  void CommandBuffer::copyImageToBuffer(VkImage srcImage,
                                        const VkImageLayout srcImageLayout,
                                        VkBuffer dstBuffer,
                                        const std::vector<VkBufferImageCopy>& regions) const
  {
    vkCmdCopyImageToBuffer(
      m_commandBuffers[m_currentFrame],
      srcImage,
      srcImageLayout,
      dstBuffer,
      regions.size(),
      regions.data()
    );
  }

} // ge