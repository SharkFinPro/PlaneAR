#include "CameraTexture.h"
#include "../../descriptorSet/DescriptorSet.h"
#include "../../logicalDevice/LogicalDevice.h"
#include "../../../utilities/Buffers.h"
#include "../../../utilities/Images.h"

namespace ge {

// ------------------------------------------------------------
// Constructor
// ------------------------------------------------------------

  CameraTexture::CameraTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                               VkDescriptorPool descriptorPool,
                               VkDescriptorSetLayout descriptorSetLayout,
                               VkCommandPool commandPool,
                               uint32_t width,
                               uint32_t height)
    : ImageTexture(std::move(logicalDevice),
                   descriptorPool,
                   descriptorSetLayout),
      m_commandPool(commandPool),
      m_width(width),
      m_height(height)
  {
  }

// ------------------------------------------------------------
// Per-frame update
// ------------------------------------------------------------

  void CameraTexture::updateFromHardwareBuffer(AHardwareBuffer* buffer)
  {
    // DO NOT early-return — camera often reuses the same AHB
    if (m_bufferPool.find(buffer) == m_bufferPool.end())
    {
      m_bufferPool[buffer] = importBuffer(buffer);
      AHardwareBuffer_acquire(buffer);
    }

    m_currentBuffer = buffer;
    const auto& slot = m_bufferPool[buffer];

    // Point base Texture state at this image
    m_textureImage     = slot.image;
    m_textureImageView = slot.imageView;

    // Camera images must be sampled from GENERAL
    m_imageInfo.imageView   = slot.imageView;
    m_imageInfo.imageLayout = VK_IMAGE_LAYOUT_GENERAL;

    // Rewrite descriptor every frame (safe & correct)
    m_descriptorSet->updateDescriptorSets(
      [this](VkDescriptorSet descriptorSet, size_t)
      {
        return std::vector<VkWriteDescriptorSet>{
          getWriteDescriptorSet(0, descriptorSet)
        };
      });

    m_ready = true;
  }

// ------------------------------------------------------------
// Import AHardwareBuffer into Vulkan
// ------------------------------------------------------------

  CameraTexture::ImportedBuffer
  CameraTexture::importBuffer(AHardwareBuffer* buffer)
  {
    ImportedBuffer slot{};

    // --- 1. Query AHB properties ---
    VkAndroidHardwareBufferFormatPropertiesANDROID formatProps{
      .sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_FORMAT_PROPERTIES_ANDROID
    };
    VkAndroidHardwareBufferPropertiesANDROID props{
      .sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID,
      .pNext = &formatProps
    };

    vkGetAndroidHardwareBufferPropertiesANDROID(
      m_logicalDevice->getDevice(), buffer, &props);

    // Create YCbCr conversion once
    if (m_ycbcrConversion == VK_NULL_HANDLE)
      createYcbcrSampler(formatProps);

    // --- 2. Create VkImage ---
    VkExternalMemoryImageCreateInfo extMemInfo{
      .sType       = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO,
      .handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_ANDROID_HARDWARE_BUFFER_BIT_ANDROID
    };

    VkImageCreateInfo imageInfo{
      .sType         = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
      .pNext         = &extMemInfo,
      .imageType     = VK_IMAGE_TYPE_2D,
      .format        = formatProps.format,
      .extent        = { m_width, m_height, 1 },
      .mipLevels     = 1,
      .arrayLayers   = 1,
      .samples       = VK_SAMPLE_COUNT_1_BIT,
      .tiling        = VK_IMAGE_TILING_OPTIMAL,
      .usage         = VK_IMAGE_USAGE_SAMPLED_BIT |
                       VK_IMAGE_USAGE_TRANSFER_DST_BIT,
      .sharingMode   = VK_SHARING_MODE_EXCLUSIVE,
      .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED
    };

    vkCreateImage(
      m_logicalDevice->getDevice(), &imageInfo, nullptr, &slot.image);

    // --- 3. Allocate & bind external memory ---
    VkImportAndroidHardwareBufferInfoANDROID importInfo{
      .sType  = VK_STRUCTURE_TYPE_IMPORT_ANDROID_HARDWARE_BUFFER_INFO_ANDROID,
      .buffer = buffer
    };

    VkMemoryDedicatedAllocateInfo dedicatedInfo{
      .sType  = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO,
      .pNext  = &importInfo,
      .image  = slot.image
    };

    VkMemoryAllocateInfo allocInfo{
      .sType           = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
      .pNext           = &dedicatedInfo,
      .allocationSize  = props.allocationSize,
      .memoryTypeIndex =
      m_logicalDevice->getPhysicalDevice()->findMemoryType(
        props.memoryTypeBits,
        VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
    };

    vkAllocateMemory(
      m_logicalDevice->getDevice(), &allocInfo, nullptr, &slot.memory);

    VkBindImageMemoryInfo bindInfo{
      .sType  = VK_STRUCTURE_TYPE_BIND_IMAGE_MEMORY_INFO,
      .image  = slot.image,
      .memory = slot.memory
    };

    vkBindImageMemory2(
      m_logicalDevice->getDevice(), 1, &bindInfo);

    // --- 4. Transition to GENERAL ---
    Images::transitionImageLayout(
      m_logicalDevice,
      m_commandPool,
      slot.image,
      formatProps.format,
      VK_IMAGE_LAYOUT_UNDEFINED,
      VK_IMAGE_LAYOUT_GENERAL,
      1,
      1);

    // --- 5. Create image view with YCbCr conversion ---
    VkSamplerYcbcrConversionInfo ycbcrInfo{
      .sType      = VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_INFO,
      .conversion = m_ycbcrConversion
    };

    VkImageViewCreateInfo viewInfo{
      .sType    = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,
      .pNext    = &ycbcrInfo,
      .image    = slot.image,
      .viewType = VK_IMAGE_VIEW_TYPE_2D,
      .format   = formatProps.format,
      .subresourceRange = {
        .aspectMask     = VK_IMAGE_ASPECT_COLOR_BIT,
        .baseMipLevel   = 0,
        .levelCount     = 1,
        .baseArrayLayer = 0,
        .layerCount     = 1
      }
    };

    vkCreateImageView(
      m_logicalDevice->getDevice(), &viewInfo, nullptr, &slot.imageView);

    return slot;
  }

// ------------------------------------------------------------
// Create YCbCr sampler
// ------------------------------------------------------------

  void CameraTexture::createYcbcrSampler(
    const VkAndroidHardwareBufferFormatPropertiesANDROID& formatProps)
  {
    VkSamplerYcbcrConversionCreateInfo ycbcrCreateInfo{
      .sType       = VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_CREATE_INFO,
      .format      = formatProps.format,
      .ycbcrModel  = formatProps.suggestedYcbcrModel,
      .ycbcrRange  = formatProps.suggestedYcbcrRange,
      .components  = formatProps.samplerYcbcrConversionComponents,
      .xChromaOffset = formatProps.suggestedXChromaOffset,
      .yChromaOffset = formatProps.suggestedYChromaOffset,
      .chromaFilter  = VK_FILTER_LINEAR
    };

    vkCreateSamplerYcbcrConversion(
      m_logicalDevice->getDevice(),
      &ycbcrCreateInfo,
      nullptr,
      &m_ycbcrConversion);

    VkSamplerYcbcrConversionInfo conversionInfo{
      .sType      = VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_INFO,
      .conversion = m_ycbcrConversion
    };

    VkSamplerCreateInfo samplerInfo{
      .sType                   = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO,
      .pNext                   = &conversionInfo,
      .magFilter               = VK_FILTER_LINEAR,
      .minFilter               = VK_FILTER_LINEAR,
      .mipmapMode              = VK_SAMPLER_MIPMAP_MODE_LINEAR,
      .addressModeU            = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
      .addressModeV            = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
      .addressModeW            = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
      .unnormalizedCoordinates = VK_FALSE
    };

    m_logicalDevice->destroySampler(m_textureSampler);
    m_textureSampler = m_logicalDevice->createSampler(samplerInfo);
    m_imageInfo.sampler = m_textureSampler;
  }

// ------------------------------------------------------------
// Destructor
// ------------------------------------------------------------

  CameraTexture::~CameraTexture()
  {
    for (auto& [ahb, slot] : m_bufferPool)
    {
      m_logicalDevice->destroyImageView(slot.imageView);
      m_logicalDevice->destroyImage(slot.image);
      m_logicalDevice->freeMemory(slot.memory);
      AHardwareBuffer_release(ahb);
    }

    if (m_ycbcrConversion != VK_NULL_HANDLE)
      vkDestroySamplerYcbcrConversion(
        m_logicalDevice->getDevice(), m_ycbcrConversion, nullptr);

    m_textureImage       = VK_NULL_HANDLE;
    m_textureImageMemory = VK_NULL_HANDLE;
    m_textureImageView   = VK_NULL_HANDLE;
  }

} // namespace ge