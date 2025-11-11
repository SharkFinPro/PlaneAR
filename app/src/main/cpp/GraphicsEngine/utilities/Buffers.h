#ifndef PLANEAR_BUFFERS_H
#define PLANEAR_BUFFERS_H

#include "../components/logicalDevice/LogicalDevice.h"
#include <vulkan/vulkan.h>
#include <memory>

namespace ge::Images {
  inline VkImageView createImageView(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                     const VkImage image,
                                     const VkFormat format,
                                     const VkImageAspectFlags aspectFlags,
                                     const uint32_t mipLevels,
                                     const VkImageViewType viewType,
                                     const uint32_t layerCount)
  {
    const VkImageViewCreateInfo imageViewCreateInfo {
      .sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,
      .image = image,
      .viewType = viewType,
      .format = format,

      .components = {
        .r = VK_COMPONENT_SWIZZLE_IDENTITY,
        .g = VK_COMPONENT_SWIZZLE_IDENTITY,
        .b = VK_COMPONENT_SWIZZLE_IDENTITY,
        .a = VK_COMPONENT_SWIZZLE_IDENTITY
      },
      .subresourceRange = {
        .aspectMask = aspectFlags,
        .baseMipLevel = 0,
        .levelCount = mipLevels,
        .baseArrayLayer = 0,
        .layerCount = layerCount
      }
    };

    return logicalDevice->createImageView(imageViewCreateInfo);
  }
} // ge

#endif //PLANEAR_BUFFERS_H
