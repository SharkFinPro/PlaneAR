#ifndef PLANEAR_COMMANDBUFFER_H
#define PLANEAR_COMMANDBUFFER_H

#include <vulkan/vulkan.h>
#include <memory>
#include <vector>

namespace ge {

  class LogicalDevice;

  class CommandBuffer
  {
  public:
    CommandBuffer(const std::shared_ptr<LogicalDevice>& logicalDevice, VkCommandPool commandPool);

    void setCurrentFrame(uint32_t currentFrame);

    void beginRenderPass(const VkRenderPassBeginInfo& renderPassBeginInfo) const;

    void endRenderPass() const;

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    std::vector<VkCommandBuffer> m_commandBuffers;

    uint32_t m_currentFrame = 0;

    void allocateCommandBuffers(VkCommandPool commandPool);
  };

} // ge

#endif //PLANEAR_COMMANDBUFFER_H
