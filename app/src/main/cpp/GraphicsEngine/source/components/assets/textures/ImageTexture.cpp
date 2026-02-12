#include "ImageTexture.h"

namespace ge {
  ImageTexture::ImageTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                             AAssetManager *assetManager, const std::string &fileName,
                             VkCommandPool commandPool, VkDescriptorPool descriptorPool,
                             VkDescriptorSetLayout descriptorSetLayout)
    : Texture(std::move(logicalDevice), VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
  {

  }
} // ge