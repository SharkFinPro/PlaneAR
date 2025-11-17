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
    CommandBuffer(const std::shared_ptr<LogicalDevice>& logicalDevice, VkCommandPool commandPool);

    [[nodiscard]] VkCommandBuffer* getCommandBuffer();

    void resetCommandBuffer() const;

    void setCurrentFrame(uint32_t currentFrame);

    void beginRenderPass(const VkRenderPassBeginInfo& renderPassBeginInfo) const;

    void endRenderPass() const;

    void record(const std::function<void()>& renderFunction) const;

    void setViewport(const VkViewport& viewport) const;

    void setScissor(const VkRect2D& scissor) const;

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    std::vector<VkCommandBuffer> m_commandBuffers;

    uint32_t m_currentFrame = 0;

    void allocateCommandBuffers(VkCommandPool commandPool);
  };

} // ge

#endif //PLANEAR_COMMANDBUFFER_H
