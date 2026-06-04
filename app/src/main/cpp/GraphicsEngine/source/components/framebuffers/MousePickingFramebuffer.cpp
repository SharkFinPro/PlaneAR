#include "MousePickingFramebuffer.h"

namespace ge {
  MousePickingFramebuffer::MousePickingFramebuffer(std::shared_ptr<LogicalDevice> logicalDevice,
                                                   const VkCommandPool& commandPool,
                                                   const std::shared_ptr<RenderPass>& renderPass,
                                                   VkExtent2D extent)
    : Framebuffer(std::move(logicalDevice))
  {
    initializeFramebuffer(commandPool, renderPass, extent);
  }

  VkFormat MousePickingFramebuffer::getColorFormat()
  {
    return VK_FORMAT_R8G8B8A8_UINT;
  }

  const std::vector<VkImageView>& MousePickingFramebuffer::getImageViews()
  {
    static const std::vector<VkImageView> empty{};

    return empty;
  }

  VkSampleCountFlagBits MousePickingFramebuffer::getSampleCount()
  {
    return VK_SAMPLE_COUNT_1_BIT;
  }

  VkImageUsageFlags MousePickingFramebuffer::getColorUsageFlags()
  {
    return VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT |
           VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
  }
} // ge