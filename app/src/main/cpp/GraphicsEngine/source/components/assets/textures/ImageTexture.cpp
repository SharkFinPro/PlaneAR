#include "ImageTexture.h"
#include <android/asset_manager.h>
#ifndef STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_IMPLEMENTATION
#endif
#include <stb_image.h>
#include <stdexcept>
#include <vector>

namespace ge {
  ImageTexture::ImageTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                             AAssetManager* assetManager,
                             const std::string& fileName,
                             VkCommandPool commandPool,
                             VkDescriptorPool descriptorPool,
                             VkDescriptorSetLayout descriptorSetLayout)
    : Texture(std::move(logicalDevice), VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
  {
    /* Load Image Buffer */
    AAsset* asset = AAssetManager_open(assetManager, fileName.c_str(), AASSET_MODE_BUFFER);
    if (!asset)
    {
      throw std::runtime_error(std::string("Failed to open asset: ") + fileName);
    }

    off_t fontBufferSize = AAsset_getLength(asset);
    const void* fontBufferPtr = AAsset_getBuffer(asset);

    std::vector<uint8_t> fontBuffer(fontBufferSize);
    std::memcpy(fontBuffer.data(), fontBufferPtr, fontBufferSize);

    AAsset_close(asset);

    /* Load Image */
  }

  void ImageTexture::createImageView()
  {

  }
} // ge