#include "CommandBuffer.h"
#include "../logicalDevice/LogicalDevice.h"

namespace ge {
  CommandBuffer::CommandBuffer(const std::shared_ptr<LogicalDevice>& logicalDevice,
                               VkCommandPool commandPool)
    : m_logicalDevice(logicalDevice)
  {
    allocateCommandBuffers(commandPool);
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
} // ge