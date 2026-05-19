#ifndef PLANEAR_MOUSEPICKER_H
#define PLANEAR_MOUSEPICKER_H

#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class LogicalDevice;
  class PipelineManager;
  struct RenderInfo;

  class MousePicker
  {
  public:
    MousePicker(std::shared_ptr<LogicalDevice> logicalDevice,
                VkCommandPool commandPool);

    ~MousePicker();

    void clearObjectsToMousePick();

    void render(const std::shared_ptr<PipelineManager>& pipelineManager,
                const RenderInfo* renderInfo) const;

    void handleRenderedMousePickingImage(VkImage image);

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    VkBuffer m_stagingBuffer = VK_NULL_HANDLE;
    VkDeviceMemory m_stagingBufferMemory = VK_NULL_HANDLE;
  };

} // ge

#endif //PLANEAR_MOUSEPICKER_H
