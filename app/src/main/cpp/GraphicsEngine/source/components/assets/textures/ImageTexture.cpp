#include "ImageTexture.h"
#include "../../descriptorSet/DescriptorSet.h"
#include "../../../utilities/Buffers.h"
#include "../../../utilities/Images.h"
#include <android/asset_manager.h>
#ifndef STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_IMPLEMENTATION
#endif
#include <stb_image.h>
#include <stdexcept>

namespace ge {
  ImageTexture::ImageTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                             AAssetManager* assetManager,
                             const std::string& fileName,
                             VkCommandPool commandPool,
                             VkDescriptorPool descriptorPool,
                             VkDescriptorSetLayout descriptorSetLayout)
    : Texture(std::move(logicalDevice), VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
  {
    const auto imageBuffer = loadImageFromFile(assetManager, fileName);

    int texWidth, texHeight, texChannels;

    stbi_uc* pixels = stbi_load_from_memory(
      imageBuffer.data(),
      static_cast<int>(imageBuffer.size()),
      &texWidth,
      &texHeight,
      &texChannels,
      STBI_rgb_alpha
    );
    if (!pixels)
    {
      throw std::runtime_error("failed to load texture image!");
    }

    const VkDeviceSize imageSize = texWidth * texHeight * 4;

    VkBuffer stagingBuffer;
    VkDeviceMemory stagingBufferMemory;
    Buffers::createBuffer(
      m_logicalDevice,
      imageSize,
      VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
      VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
      stagingBuffer,
      stagingBufferMemory
    );

    m_logicalDevice->doMappedMemoryOperation(stagingBufferMemory, [pixels, imageSize](void* data) {
      memcpy(data, pixels, imageSize);
    });

    stbi_image_free(pixels);

    createAndPrepareImage(commandPool, texWidth, texHeight);

    copyBufferToImage(commandPool, texWidth, texHeight, stagingBuffer);

    transitionImageToShaderReadable(commandPool);

    Buffers::destroyBuffer(m_logicalDevice, stagingBuffer, stagingBufferMemory);

    createImageView();

    createDescriptorSet(descriptorPool, descriptorSetLayout);
  }

  ImageTexture::ImageTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                             VkDescriptorPool descriptorPool,
                             VkDescriptorSetLayout descriptorSetLayout)
    : Texture(std::move(logicalDevice), VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
  {
    createDescriptorSet(descriptorPool, descriptorSetLayout);
  }

  VkDescriptorSet ImageTexture::getDescriptorSet(const uint32_t currentFrame) const
  {
    return m_descriptorSet->getDescriptorSet(currentFrame);
  }

  std::vector<uint8_t> ImageTexture::loadImageFromFile(AAssetManager* assetManager,
                                                       const std::string& fileName)
  {
    AAsset* asset = AAssetManager_open(assetManager, fileName.c_str(), AASSET_MODE_BUFFER);
    if (!asset)
    {
      throw std::runtime_error(std::string("Failed to open asset: ") + fileName);
    }

    const off_t imageBufferSize = AAsset_getLength(asset);
    const void* imageBufferPtr = AAsset_getBuffer(asset);

    std::vector<uint8_t> imageBuffer(imageBufferSize);
    std::memcpy(imageBuffer.data(), imageBufferPtr, imageBufferSize);

    AAsset_close(asset);

    return imageBuffer;
  }

  void ImageTexture::createAndPrepareImage(const VkCommandPool& commandPool,
                                           const uint32_t width,
                                           const uint32_t height)
  {
    Images::createImage(
      m_logicalDevice,
      0,
      width,
      height,
      1,
      m_mipLevels,
      VK_SAMPLE_COUNT_1_BIT,
      VK_FORMAT_R8G8B8A8_UNORM,
      VK_IMAGE_TILING_OPTIMAL,
      VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
      VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
      m_textureImage,
      m_textureImageMemory,
      VK_IMAGE_TYPE_2D,
      1
    );

    Images::transitionImageLayout(
      m_logicalDevice,
      commandPool,
      m_textureImage,
      VK_FORMAT_R8G8B8A8_UNORM,
      VK_IMAGE_LAYOUT_UNDEFINED,
      VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
      m_mipLevels,
      1
    );
  }

  void ImageTexture::copyBufferToImage(const VkCommandPool& commandPool,
                                       uint32_t width,
                                       uint32_t height,
                                       VkBuffer& stagingBuffer)
  {
    VkCommandBuffer commandBuffer = Buffers::beginSingleTimeCommands(m_logicalDevice, commandPool);

    VkBufferImageCopy region {
      .bufferOffset = 0,
      .bufferRowLength = 0,
      .bufferImageHeight = 0,
      .imageSubresource = {
        .aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
        .mipLevel = 0,
        .baseArrayLayer = 0,
        .layerCount = 1,
      },
      .imageOffset = {0, 0, 0},
      .imageExtent = { width, height, 1}
    };

    vkCmdCopyBufferToImage(
      commandBuffer,
      stagingBuffer,
      m_textureImage,
      VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
      1,
      &region
    );

    Buffers::endSingleTimeCommands(m_logicalDevice, commandPool, m_logicalDevice->getGraphicsQueue(), commandBuffer);
  }

  void ImageTexture::transitionImageToShaderReadable(const VkCommandPool& commandPool)
  {
    Images::transitionImageLayout(
      m_logicalDevice,
      commandPool,
      m_textureImage,
      VK_FORMAT_R8G8B8A8_UNORM,
      VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
      VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
      m_mipLevels,
      1
    );
  }

  void ImageTexture::createImageView()
  {
    m_textureImageView = Images::createImageView(
      m_logicalDevice,
      m_textureImage,
      VK_FORMAT_R8G8B8A8_UNORM,
      VK_IMAGE_ASPECT_COLOR_BIT,
      m_mipLevels,
      VK_IMAGE_VIEW_TYPE_2D,
      1
    );

    m_imageInfo.imageView = m_textureImageView;
  }

  void ImageTexture::createDescriptorSet(VkDescriptorPool descriptorPool,
                                         VkDescriptorSetLayout descriptorSetLayout)
  {
    m_descriptorSet = std::make_shared<DescriptorSet>(m_logicalDevice, descriptorPool, descriptorSetLayout);
    m_descriptorSet->updateDescriptorSets([this](VkDescriptorSet descriptorSet, [[maybe_unused]] const size_t frame)
    {
      std::vector<VkWriteDescriptorSet> descriptorWrites{{
        getWriteDescriptorSet(0, descriptorSet)
      }};

      return descriptorWrites;
    });
  }
} // ge