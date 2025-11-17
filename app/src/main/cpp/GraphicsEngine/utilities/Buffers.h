#ifndef PLANEAR_BUFFERS_H
#define PLANEAR_BUFFERS_H

#include "../components/logicalDevice/LogicalDevice.h"
#include <vulkan/vulkan.h>
#include <memory>

namespace ge::Buffers {
  inline VkCommandBuffer beginSingleTimeCommands(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                                 const VkCommandPool commandPool)
  {
    const VkCommandBufferAllocateInfo allocateInfo {
      .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO,
      .commandPool = commandPool,
      .level = VK_COMMAND_BUFFER_LEVEL_PRIMARY,
      .commandBufferCount = 1
    };

    VkCommandBuffer commandBuffer = VK_NULL_HANDLE;
    logicalDevice->allocateCommandBuffers(allocateInfo, &commandBuffer);

    constexpr VkCommandBufferBeginInfo beginInfo {
      .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO,
      .flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT
    };

    if (vkBeginCommandBuffer(commandBuffer, &beginInfo) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to begin command buffer!");
    }

    return commandBuffer;
  }

  inline void endSingleTimeCommands(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                    const VkCommandPool commandPool,
                                    const VkQueue queue,
                                    VkCommandBuffer commandBuffer)
  {
    if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to end command buffer!");
    }

    const VkSubmitInfo submitInfo {
      .sType = VK_STRUCTURE_TYPE_SUBMIT_INFO,
      .commandBufferCount = 1,
      .pCommandBuffers = &commandBuffer
    };

    if (vkQueueSubmit(queue, 1, &submitInfo, VK_NULL_HANDLE) != VK_SUCCESS)
    {
      throw std::runtime_error("failed to submit command buffer!");
    }

    vkQueueWaitIdle(queue);

    logicalDevice->freeCommandBuffers(commandPool, 1, &commandBuffer);
  }
}

#endif //PLANEAR_BUFFERS_H
