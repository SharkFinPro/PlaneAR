#include "CameraTexture.h"
#include "../../descriptorSet/DescriptorSet.h"
#include "../../logicalDevice/LogicalDevice.h"
#include "../../../utilities/Buffers.h"
#include "../../../utilities/Images.h"
#include "VKCheck.h"

namespace ge {

  CameraTexture::CameraTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                               VkDescriptorPool descriptorPool,
                               VkCommandPool commandPool)
    : ImageTexture(std::move(logicalDevice)),
      m_commandPool(commandPool),
      m_descriptorPool(descriptorPool)
  {}

  CameraTexture::~CameraTexture()
  {
    if (m_currentBuffer)
    {
      m_logicalDevice->destroyImageView(m_imageData.imageView);
      m_logicalDevice->destroyImage(m_imageData.image);
      m_logicalDevice->freeMemory(m_imageData.memory);
      AHardwareBuffer_release(m_currentBuffer);
    }

    if (m_ycbcrConversion != VK_NULL_HANDLE)
    {
      vkDestroySamplerYcbcrConversion(
        m_logicalDevice->getDevice(),
        m_ycbcrConversion,
        nullptr
      );
    }

    m_logicalDevice->destroySampler(m_ycbcrSampler);
  }

  void CameraTexture::updateFromHardwareBuffer(AHardwareBuffer* buffer)
  {
    if (m_currentBuffer == buffer)
    {
      return;
    }

    if (m_currentBuffer)
    {
      m_logicalDevice->waitIdle();
      m_logicalDevice->destroyImageView(m_imageData.imageView);
      m_logicalDevice->destroyImage(m_imageData.image);
      m_logicalDevice->freeMemory(m_imageData.memory);
      AHardwareBuffer_release(m_currentBuffer);
    }

    AHardwareBuffer_acquire(buffer);

    m_imageData = importBuffer(buffer);
    m_currentBuffer = buffer;

    // Point base Texture state at this image
    m_textureImage     = m_imageData.image;
    m_textureImageView = m_imageData.imageView;

    // Camera images must be sampled from GENERAL
    m_imageInfo.imageView   = m_imageData.imageView;
    m_imageInfo.imageLayout = VK_IMAGE_LAYOUT_GENERAL;
    m_imageInfo.sampler = VK_NULL_HANDLE;

    if (!m_descriptorSet)
    {
      const VkDescriptorSetLayoutBinding imageDescriptorSetLayoutBinding {
        .binding = 0,
        .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
        .descriptorCount = 1,
        .stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT,
        .pImmutableSamplers = &m_ycbcrSampler
      };

      const std::array descriptorSetLayoutBindings {
        imageDescriptorSetLayoutBinding
      };

      const VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo {
        .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO,
        .bindingCount = static_cast<uint32_t>(descriptorSetLayoutBindings.size()),
        .pBindings = descriptorSetLayoutBindings.data()
      };

      m_descriptorSetLayout = m_logicalDevice->createDescriptorSetLayout(descriptorSetLayoutCreateInfo);

      createDescriptorSet(m_descriptorPool, m_descriptorSetLayout);
    }

    // Rewrite descriptor every frame (safe & correct)
    m_descriptorSet->updateDescriptorSets(
    [this](VkDescriptorSet descriptorSet, size_t)
    {
      return std::vector<VkWriteDescriptorSet>{
        getWriteDescriptorSet(0, descriptorSet)
      };
    });
  }

  VkDescriptorSetLayout CameraTexture::getDescriptorSetLayout() const
  {
    return m_descriptorSetLayout;
  }

  CameraTexture::ImportedBuffer CameraTexture::importBuffer(AHardwareBuffer* hardware_buffer)
  {
    ImportedBuffer slot{};

// 2
    AHardwareBuffer_Desc ahb_desc = {};
    AHardwareBuffer_describe(hardware_buffer, &ahb_desc);

// 3
    VkAndroidHardwareBufferFormatPropertiesANDROID format_props = {
      .sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_FORMAT_PROPERTIES_ANDROID
    };
    VkAndroidHardwareBufferPropertiesANDROID ahb_props = {
      .sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID,
      .pNext = &format_props
    };

    VK_CHECK(vkGetAndroidHardwareBufferPropertiesANDROID(m_logicalDevice->getDevice(), hardware_buffer, &ahb_props));

// 4
    // If format is VK_FORMAT_UNDEFINED (YCbCr), use external format
    VkExternalFormatANDROID external_format = {
      .sType  = VK_STRUCTURE_TYPE_EXTERNAL_FORMAT_ANDROID,
      .externalFormat = format_props.externalFormat  // use 0 if format != UNDEFINED
    };

    VkExternalMemoryImageCreateInfo ext_mem_image_info = {
      .sType       = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO,
      .pNext       = (format_props.format == VK_FORMAT_UNDEFINED) ? &external_format : nullptr,
      .handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_ANDROID_HARDWARE_BUFFER_BIT_ANDROID
    };

    VkImageCreateInfo image_info = {
      .sType         = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
      .pNext         = &ext_mem_image_info,
      .imageType     = VK_IMAGE_TYPE_2D,
      .format        = format_props.format,  // May be VK_FORMAT_UNDEFINED
      .extent        = { ahb_desc.width, ahb_desc.height, 1 },
      .mipLevels     = 1,
      .arrayLayers   = 1,
      .samples       = VK_SAMPLE_COUNT_1_BIT,
      .tiling        = VK_IMAGE_TILING_OPTIMAL,
      .usage         = VK_IMAGE_USAGE_SAMPLED_BIT,
      .sharingMode   = VK_SHARING_MODE_EXCLUSIVE,
      .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED
    };

    VK_CHECK(vkCreateImage(m_logicalDevice->getDevice(), &image_info, nullptr, &slot.image));


// 5
    VkImportAndroidHardwareBufferInfoANDROID import_info = {
      .sType  = VK_STRUCTURE_TYPE_IMPORT_ANDROID_HARDWARE_BUFFER_INFO_ANDROID,
      .buffer = hardware_buffer
    };

    VkMemoryDedicatedAllocateInfo dedicated_info = {
      .sType  = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO,
      .pNext  = &import_info,
      .image  = slot.image
    };

// Find the right memory type index from ahb_props.memoryTypeBits
    VkMemoryAllocateInfo mem_alloc = {
      .sType           = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
      .pNext           = &dedicated_info,
      .allocationSize  = ahb_props.allocationSize,
      .memoryTypeIndex = m_logicalDevice->getPhysicalDevice()->findMemoryType(
                           ahb_props.memoryTypeBits,
                           VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
    };

    VK_CHECK(vkAllocateMemory(m_logicalDevice->getDevice(), &mem_alloc, nullptr, &slot.memory));
    VK_CHECK(vkBindImageMemory(m_logicalDevice->getDevice(), slot.image, slot.memory, 0));


// 6
    createYCBCRResources(format_props);

    VkSamplerYcbcrConversionInfo conversion_info = {
      .sType      = VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_INFO,
      .conversion = m_ycbcrConversion
    };

    VkImageViewCreateInfo view_info = {
      .sType      = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,  // was missing
      .pNext      = &conversion_info,
      .image      = slot.image,                                // was missing
      .viewType   = VK_IMAGE_VIEW_TYPE_2D,
      .format     = VK_FORMAT_UNDEFINED,
      .components = {
        VK_COMPONENT_SWIZZLE_IDENTITY,
        VK_COMPONENT_SWIZZLE_IDENTITY,
        VK_COMPONENT_SWIZZLE_IDENTITY,
        VK_COMPONENT_SWIZZLE_IDENTITY
      },
      .subresourceRange = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1 }
    };

    VK_CHECK(vkCreateImageView(m_logicalDevice->getDevice(), &view_info, nullptr, &slot.imageView));

    return slot;
  }

  void CameraTexture::createYCBCRResources(const VkAndroidHardwareBufferFormatPropertiesANDROID& format_props)
  {
    if (m_ycbcrConversion != VK_NULL_HANDLE)
    {
      return;
    }

    VkExternalFormatANDROID ycbcr_ext_format = {
      .sType          = VK_STRUCTURE_TYPE_EXTERNAL_FORMAT_ANDROID,
      .externalFormat = format_props.externalFormat
    };

    VkSamplerYcbcrConversionCreateInfo ycbcr_info = {
      .sType          = VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_CREATE_INFO,
      .pNext          = &ycbcr_ext_format,
      .format         = VK_FORMAT_UNDEFINED,
      .ycbcrModel     = format_props.suggestedYcbcrModel,
      .ycbcrRange     = format_props.suggestedYcbcrRange,
      .components     = format_props.samplerYcbcrConversionComponents,
      .xChromaOffset  = format_props.suggestedXChromaOffset,
      .yChromaOffset  = format_props.suggestedYChromaOffset,
      .chromaFilter   = VK_FILTER_LINEAR,
      .forceExplicitReconstruction = VK_FALSE
    };

    VK_CHECK(vkCreateSamplerYcbcrConversion(m_logicalDevice->getDevice(), &ycbcr_info, nullptr, &m_ycbcrConversion));

    VkSamplerYcbcrConversionInfo conversion_info = {
      .sType      = VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_INFO,
      .conversion = m_ycbcrConversion
    };

// Immutable sampler - REQUIRED for YCbCr
    VkSamplerCreateInfo sampler_info = {
      .sType                   = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO,
      .pNext                   = &conversion_info,
      .magFilter               = VK_FILTER_LINEAR,   // must match chromaFilter
      .minFilter               = VK_FILTER_LINEAR,   // must match chromaFilter
      .mipmapMode              = VK_SAMPLER_MIPMAP_MODE_LINEAR,
      .addressModeU            = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,  // required
      .addressModeV            = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,  // required
      .addressModeW            = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,  // required
      .mipLodBias              = 0.0f,
      .anisotropyEnable        = VK_FALSE,   // required
      .compareEnable           = VK_FALSE,
      .minLod                  = 0.0f,
      .maxLod                  = 0.0f,
      .unnormalizedCoordinates = VK_FALSE    // required
    };

    VK_CHECK(vkCreateSampler(m_logicalDevice->getDevice(), &sampler_info, nullptr, &m_ycbcrSampler));
  }

} // namespace ge