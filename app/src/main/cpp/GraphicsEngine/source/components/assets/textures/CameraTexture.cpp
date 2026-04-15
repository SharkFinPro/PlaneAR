#include "CameraTexture.h"
#include "../../descriptorSet/DescriptorSet.h"
#include "../../logicalDevice/LogicalDevice.h"
#include "../../../utilities/Buffers.h"
#include "../../../utilities/Images.h"

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

    m_textureImage = m_imageData.image;
    m_textureImageView = m_imageData.imageView;

    m_imageInfo.imageView = m_imageData.imageView;
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

  CameraTexture::ImportedBuffer CameraTexture::importBuffer(AHardwareBuffer* hardwareBuffer)
  {
    ImportedBuffer slot{};

    AHardwareBuffer_Desc ahbDesc = {};
    AHardwareBuffer_describe(hardwareBuffer, &ahbDesc);

    VkAndroidHardwareBufferFormatPropertiesANDROID formatProperties = {
      .sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_FORMAT_PROPERTIES_ANDROID
    };
    VkAndroidHardwareBufferPropertiesANDROID ahbProperties = {
      .sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID,
      .pNext = &formatProperties
    };

    vkGetAndroidHardwareBufferPropertiesANDROID(m_logicalDevice->getDevice(), hardwareBuffer, &ahbProperties);

    createYCBCRResources(formatProperties);

    VkExternalFormatANDROID externalFormat = {
      .sType = VK_STRUCTURE_TYPE_EXTERNAL_FORMAT_ANDROID,
      .externalFormat = formatProperties.externalFormat
    };

    VkExternalMemoryImageCreateInfo extMemImageInfo = {
      .sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO,
      .pNext = (formatProperties.format == VK_FORMAT_UNDEFINED) ? &externalFormat : nullptr,
      .handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_ANDROID_HARDWARE_BUFFER_BIT_ANDROID
    };

    VkImageCreateInfo imageInfo = {
      .sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
      .pNext = &extMemImageInfo,
      .imageType = VK_IMAGE_TYPE_2D,
      .format = formatProperties.format,
      .extent = { ahbDesc.width, ahbDesc.height, 1 },
      .mipLevels = 1,
      .arrayLayers = 1,
      .samples = VK_SAMPLE_COUNT_1_BIT,
      .tiling = VK_IMAGE_TILING_OPTIMAL,
      .usage = VK_IMAGE_USAGE_SAMPLED_BIT,
      .sharingMode = VK_SHARING_MODE_EXCLUSIVE,
      .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED
    };

    vkCreateImage(m_logicalDevice->getDevice(), &imageInfo, nullptr, &slot.image);

    VkImportAndroidHardwareBufferInfoANDROID importInfo = {
      .sType = VK_STRUCTURE_TYPE_IMPORT_ANDROID_HARDWARE_BUFFER_INFO_ANDROID,
      .buffer = hardwareBuffer
    };

    VkMemoryDedicatedAllocateInfo dedicatedInfo = {
      .sType = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO,
      .pNext = &importInfo,
      .image = slot.image
    };

    VkMemoryAllocateInfo memoryAllocateInfo = {
      .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
      .pNext = &dedicatedInfo,
      .allocationSize = ahbProperties.allocationSize,
      .memoryTypeIndex = m_logicalDevice->getPhysicalDevice()->findMemoryType(
                           ahbProperties.memoryTypeBits,
                           VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
    };

    vkAllocateMemory(m_logicalDevice->getDevice(), &memoryAllocateInfo, nullptr, &slot.memory);
    vkBindImageMemory(m_logicalDevice->getDevice(), slot.image, slot.memory, 0);

    VkSamplerYcbcrConversionInfo conversionInfo = {
      .sType = VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_INFO,
      .conversion = m_ycbcrConversion
    };

    VkImageViewCreateInfo viewInfo = {
      .sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,
      .pNext = &conversionInfo,
      .image = slot.image,
      .viewType = VK_IMAGE_VIEW_TYPE_2D,
      .format = VK_FORMAT_UNDEFINED,
      .components = {
        VK_COMPONENT_SWIZZLE_IDENTITY,
        VK_COMPONENT_SWIZZLE_IDENTITY,
        VK_COMPONENT_SWIZZLE_IDENTITY,
        VK_COMPONENT_SWIZZLE_IDENTITY
      },
      .subresourceRange = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1 }
    };

    vkCreateImageView(m_logicalDevice->getDevice(), &viewInfo, nullptr, &slot.imageView);

    return slot;
  }

  void CameraTexture::createYCBCRResources(const VkAndroidHardwareBufferFormatPropertiesANDROID& formatProperties)
  {
    if (m_ycbcrConversion != VK_NULL_HANDLE)
    {
      return;
    }

    VkExternalFormatANDROID ycbcrExtFormat = {
      .sType = VK_STRUCTURE_TYPE_EXTERNAL_FORMAT_ANDROID,
      .externalFormat = formatProperties.externalFormat
    };

    VkSamplerYcbcrConversionCreateInfo ycbcrInfo = {
      .sType = VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_CREATE_INFO,
      .pNext = &ycbcrExtFormat,
      .format = VK_FORMAT_UNDEFINED,
      .ycbcrModel = formatProperties.suggestedYcbcrModel,
      .ycbcrRange = formatProperties.suggestedYcbcrRange,
      .components = formatProperties.samplerYcbcrConversionComponents,
      .xChromaOffset = formatProperties.suggestedXChromaOffset,
      .yChromaOffset = formatProperties.suggestedYChromaOffset,
      .chromaFilter = VK_FILTER_LINEAR,
      .forceExplicitReconstruction = VK_FALSE
    };

    vkCreateSamplerYcbcrConversion(m_logicalDevice->getDevice(), &ycbcrInfo, nullptr, &m_ycbcrConversion);

    VkSamplerYcbcrConversionInfo conversionInfo = {
      .sType = VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_INFO,
      .conversion = m_ycbcrConversion
    };

    VkSamplerCreateInfo samplerInfo = {
      .sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO,
      .pNext = &conversionInfo,
      .magFilter = VK_FILTER_LINEAR,
      .minFilter = VK_FILTER_LINEAR,
      .mipmapMode = VK_SAMPLER_MIPMAP_MODE_LINEAR,
      .addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
      .addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
      .addressModeW = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
      .mipLodBias = 0.0f,
      .anisotropyEnable = VK_FALSE,
      .compareEnable = VK_FALSE,
      .minLod = 0.0f,
      .maxLod = 0.0f,
      .unnormalizedCoordinates = VK_FALSE
    };

    vkCreateSampler(m_logicalDevice->getDevice(), &samplerInfo, nullptr, &m_ycbcrSampler);
  }

} // namespace ge