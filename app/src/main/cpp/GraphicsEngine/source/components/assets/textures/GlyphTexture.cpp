#include "GlyphTexture.h"
#include "../../../utilities/Buffers.h"
#include "../../../utilities/Images.h"

namespace ge {
  GlyphTexture::GlyphTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                             const VkCommandPool& commandPool,
                             const unsigned char* pixelData,
                             uint32_t width,
                             uint32_t height)
    : Texture(std::move(logicalDevice), VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
  {
    createTextureImage(commandPool, pixelData, width, height);

    createImageView();
  }

  void GlyphTexture::createTextureImage(const VkCommandPool& commandPool,
                                        const unsigned char* pixelData,
                                        uint32_t width,
                                        uint32_t height)
  {
    VkBuffer stagingBuffer = VK_NULL_HANDLE;
    VkDeviceMemory stagingBufferMemory = VK_NULL_HANDLE;

    createAndFillStagingBuffer(pixelData, width, height, stagingBuffer, stagingBufferMemory);

    createAndPrepareImage(commandPool, width, height);

    copyBufferToImage(commandPool, width, height, stagingBuffer);

    transitionImageToShaderReadable(commandPool);

    cleanupStagingBuffer(stagingBuffer, stagingBufferMemory);
  }

  void GlyphTexture::createAndFillStagingBuffer(const unsigned char* pixelData,
                                                uint32_t width,
                                                uint32_t height,
                                                VkBuffer& stagingBuffer,
                                                VkDeviceMemory& stagingBufferMemory)
  {
    const VkDeviceSize imageSize = width * height;

    Buffers::createBuffer(
      m_logicalDevice,
      imageSize,
      VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
      VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
      stagingBuffer,
      stagingBufferMemory
    );

    m_logicalDevice->doMappedMemoryOperation(stagingBufferMemory, [pixelData, imageSize](void* data) {
      memcpy(data, pixelData, imageSize);
    });
  }

  void GlyphTexture::createAndPrepareImage(const VkCommandPool& commandPool,
                                           uint32_t width,
                                           uint32_t height)
  {
    Images::createImage(
      m_logicalDevice,
      0,
      width,
      height,
      1,
      m_mipLevels,
      VK_SAMPLE_COUNT_1_BIT,
      VK_FORMAT_R8_UNORM,
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
      VK_FORMAT_R8_UNORM,
      VK_IMAGE_LAYOUT_UNDEFINED,
      VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
      m_mipLevels,
      1
    );
  }

  void GlyphTexture::copyBufferToImage(const VkCommandPool& commandPool,
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
      .imageExtent = {width, height, 1}
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

  void GlyphTexture::transitionImageToShaderReadable(const VkCommandPool& commandPool)
  {
    Images::transitionImageLayout(
      m_logicalDevice,
      commandPool,
      m_textureImage,
      VK_FORMAT_R8_UNORM,
      VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
      VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
      m_mipLevels,
      1
    );
  }

  void GlyphTexture::cleanupStagingBuffer(VkBuffer& stagingBuffer,
                                          VkDeviceMemory& stagingBufferMemory)
  {
    Buffers::destroyBuffer(m_logicalDevice, stagingBuffer, stagingBufferMemory);
  }

  void GlyphTexture::createImageView()
  {
    m_textureImageView = Images::createImageView(
      m_logicalDevice,
      m_textureImage,
      VK_FORMAT_R8_UNORM,
      VK_IMAGE_ASPECT_COLOR_BIT,
      m_mipLevels,
      VK_IMAGE_VIEW_TYPE_2D,
      1
    );

    m_imageInfo.imageView = m_textureImageView;
  }
} // ge