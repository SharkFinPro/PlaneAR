#include "MousePicker.h"
#include "../../commandBuffer/SingleUseCommandBuffer.h"
#include "../../pipelines/GraphicsPipeline.h"
#include "../../pipelines/PipelineManager.h"
#include "../../../utilities/Buffers.h"
#include "../../../utilities/Images.h"

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
    m_objectsToMousePick.clear();
  }

  void MousePicker::addObjectToMousePick(MousePickingPoint object)
  {
    m_objectsToMousePick.push_back(object);
  }

  void MousePicker::render(const std::shared_ptr<PipelineManager>& pipelineManager,
                           const RenderInfo* renderInfo) const
  {
    pipelineManager->bindGraphicsPipeline(renderInfo->commandBuffer, PipelineType::mousePicking);

    for (const auto& object : m_objectsToMousePick)
    {
      const auto pointPC = object.createPushConstant(renderInfo->extent);

      pipelineManager->pushGraphicsPipelineConstants(
        renderInfo->commandBuffer,
        PipelineType::mousePicking,
        VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
        0,
        sizeof(pointPC),
        &pointPC
      );

      renderInfo->commandBuffer->draw(4, 1, 0, 0);
    }
  }

  void MousePicker::handleRenderedMousePickingImage(VkImage image)
  {

  }

  uint32_t MousePicker::getSelectedID() const
  {
    return m_selectedID;
  }

  void MousePicker::doMousePicking(const uint32_t mouseX,
                                   const uint32_t mouseY)
  {

  }

  uint32_t MousePicker::getIDFromMousePickingImage(VkImage image,
                                                   const int32_t mouseX,
                                                   const int32_t mouseY) const
  {
    const auto commandBuffer = SingleUseCommandBuffer(m_logicalDevice, m_commandPool, m_logicalDevice->getGraphicsQueue());

    commandBuffer.record([this, commandBuffer, image, mouseX, mouseY] {
      transitionImageForReading(commandBuffer, image);

      Images::copyImageToBuffer(
        image,
        { mouseX, mouseY, 0 },
        { 1, 1, 1 },
        commandBuffer,
        m_stagingBuffer
      );

      transitionImageForWriting(commandBuffer, image);
    });

    return getObjectIDFromBuffer(m_stagingBufferMemory);
  }

  uint32_t MousePicker::getObjectIDFromBuffer(VkDeviceMemory stagingBufferMemory) const
  {
    uint32_t objectID = 0;

    m_logicalDevice->doMappedMemoryOperation(stagingBufferMemory, [&objectID](void* data) {
      const uint8_t* pixel = static_cast<uint8_t*>(data);

      objectID = static_cast<uint32_t>(pixel[0]) << 16 |
                 static_cast<uint32_t>(pixel[1]) << 8 |
                 static_cast<uint32_t>(pixel[2]);
    });

    return objectID;
  }

  void MousePicker::transitionImageForReading(const SingleUseCommandBuffer& commandBuffer,
                                              VkImage image)
  {
    const VkImageMemoryBarrier imageMemoryBarrier {
      .sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER,
      .srcAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,
      .dstAccessMask = VK_ACCESS_TRANSFER_READ_BIT,
      .oldLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
      .newLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
      .srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
      .dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
      .image = image,
      .subresourceRange {
        .aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
        .baseMipLevel = 0,
        .levelCount = 1,
        .baseArrayLayer = 0,
        .layerCount = 1
      }
    };

    commandBuffer.pipelineBarrier(
      VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
      VK_PIPELINE_STAGE_TRANSFER_BIT,
      0,
      {},
      {},
      { imageMemoryBarrier }
    );
  }

  void MousePicker::transitionImageForWriting(const SingleUseCommandBuffer& commandBuffer,
                                              VkImage image)
  {
    const VkImageMemoryBarrier imageMemoryBarrier {
      .sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER,
      .srcAccessMask = VK_ACCESS_TRANSFER_READ_BIT,
      .dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,
      .oldLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
      .newLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
      .srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
      .dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
      .image = image,
      .subresourceRange {
        .aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
        .baseMipLevel = 0,
        .levelCount = 1,
        .baseArrayLayer = 0,
        .layerCount = 1
      }
    };

    commandBuffer.pipelineBarrier(
      VK_PIPELINE_STAGE_TRANSFER_BIT,
      VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
      0,
      {},
      {},
      { imageMemoryBarrier }
    );
  }
} // ge