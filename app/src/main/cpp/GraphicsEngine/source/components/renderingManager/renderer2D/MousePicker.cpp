#include "MousePicker.h"
#include "../../pipelines/GraphicsPipeline.h"
#include "../../pipelines/PipelineManager.h"
#include "../../../utilities/Buffers.h"

namespace ge {
  MousePicker::MousePicker(std::shared_ptr<LogicalDevice> logicalDevice,
                           VkCommandPool commandPool)
    : m_logicalDevice(std::move(logicalDevice)), m_commandPool(commandPool)
  {
    constexpr VkDeviceSize bufferSize = 4;

    Buffers::createBuffer(
      m_logicalDevice,
      bufferSize,
      VK_BUFFER_USAGE_TRANSFER_DST_BIT,
      VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
      m_stagingBuffer,
      m_stagingBufferMemory
    );
  }

  MousePicker::~MousePicker()
  {
    Buffers::destroyBuffer(m_logicalDevice, m_stagingBuffer, m_stagingBufferMemory);
  }

  void MousePicker::clearObjectsToMousePick()
  {

  }

  void MousePicker::render(const std::shared_ptr<PipelineManager>& pipelineManager,
                           const RenderInfo* renderInfo) const
  {
    pipelineManager->bindGraphicsPipeline(renderInfo->commandBuffer, PipelineType::mousePicking);
  }

  void MousePicker::handleRenderedMousePickingImage(VkImage image)
  {

  }
} // ge