#ifndef PLANEAR_SINGLEUSECOMMANDBUFFER_H
#define PLANEAR_SINGLEUSECOMMANDBUFFER_H

#include "CommandBuffer.h"

namespace ge {

  class SingleUseCommandBuffer final : public CommandBuffer {
  public:
    SingleUseCommandBuffer(std::shared_ptr<LogicalDevice> logicalDevice,
                           VkCommandPool commandPool,
                           VkQueue queue);

    void record(const std::function<void()>& renderFunction) const override;

  private:
    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    VkQueue m_queue = VK_NULL_HANDLE;

    void allocateCommandBuffers(VkCommandPool commandPool) override;
  };

} // ge

#endif //PLANEAR_SINGLEUSECOMMANDBUFFER_H
