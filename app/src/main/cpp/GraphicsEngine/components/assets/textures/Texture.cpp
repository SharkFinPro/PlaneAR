#include "Texture.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../physicalDevice/PhysicalDevice.h"

namespace ge {
  Texture::Texture(const std::shared_ptr<LogicalDevice>& logicalDevice,
                   VkSamplerAddressMode samplerAddressMode)
    : m_logicalDevice(logicalDevice)
  {
    createTextureSampler(samplerAddressMode);

    m_imageInfo.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
  }

  Texture::~Texture()
  {
    m_logicalDevice->destroySampler(m_textureSampler);
    m_logicalDevice->destroyImageView(m_textureImageView);

    m_logicalDevice->destroyImage(m_textureImage);

    m_logicalDevice->freeMemory(m_textureImageMemory);
  }

  VkDescriptorPoolSize Texture::getDescriptorPoolSize() const
  {
    const VkDescriptorPoolSize poolSize {
      .type = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
      .descriptorCount = m_logicalDevice->getMaxFramesInFlight(),
    };

    return poolSize;
  }

  VkWriteDescriptorSet Texture::getDescriptorSet(uint32_t binding,
                                                 const VkDescriptorSet& dstSet) const
  {
    const VkWriteDescriptorSet descriptorSet {
      .sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET,
      .dstSet = dstSet,
      .dstBinding = binding,
      .dstArrayElement = 0,
      .descriptorCount = 1,
      .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
      .pImageInfo = &m_imageInfo
    };

    return descriptorSet;
  }

  void Texture::createTextureSampler(VkSamplerAddressMode addressMode)
  {
    const VkPhysicalDeviceProperties deviceProperties = m_logicalDevice->getPhysicalDevice()->getDeviceProperties();

    const VkSamplerCreateInfo samplerCreateInfo {
      .sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO,
      .magFilter = VK_FILTER_LINEAR,
      .minFilter = VK_FILTER_LINEAR,
      .mipmapMode = VK_SAMPLER_MIPMAP_MODE_LINEAR,
      .addressModeU = addressMode,
      .addressModeV = addressMode,
      .addressModeW = addressMode,
      .mipLodBias = 0.0f,
      .anisotropyEnable = VK_TRUE,
      .maxAnisotropy = deviceProperties.limits.maxSamplerAnisotropy,
      .compareEnable = VK_FALSE,
      .compareOp = VK_COMPARE_OP_ALWAYS,
      .minLod = 0.0f,
      .maxLod = VK_LOD_CLAMP_NONE,
      .borderColor = VK_BORDER_COLOR_INT_OPAQUE_BLACK,
      .unnormalizedCoordinates = VK_FALSE
    };

    m_textureSampler = m_logicalDevice->createSampler(samplerCreateInfo);

    m_imageInfo.sampler = m_textureSampler;
  }
} // ge