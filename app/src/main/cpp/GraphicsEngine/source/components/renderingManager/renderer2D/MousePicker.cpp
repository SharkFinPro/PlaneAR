#include "MousePicker.h"

namespace ge {
  MousePicker::MousePicker(std::shared_ptr<LogicalDevice> logicalDevice,
                           VkCommandPool commandPool)
    : m_logicalDevice(std::move(logicalDevice)), m_commandPool(commandPool)
  {

  }

  void MousePicker::clearObjectsToMousePick()
  {

  }

  void MousePicker::render(const std::shared_ptr<PipelineManager>& pipelineManager,
                           const RenderInfo* renderInfo) const
  {

  }

  void MousePicker::handleRenderedMousePickingImage(VkImage image)
  {

  }
} // ge