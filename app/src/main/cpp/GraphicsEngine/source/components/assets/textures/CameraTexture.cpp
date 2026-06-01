#include "CameraTexture.h"
#include "../../descriptorSet/DescriptorSet.h"
#include "../../logicalDevice/LogicalDevice.h"
#include "../../../utilities/Buffers.h"
#include "../../../../Logger.h"

namespace ge {

  CameraTexture::CameraTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                               VkDescriptorPool descriptorPool,
                               VkCommandPool commandPool)
    : ImageTexture(std::move(logicalDevice)),
      m_commandPool(commandPool),
      m_descriptorPool(descriptorPool)
  {
    m_dirtyFrames.resize(m_logicalDevice->getMaxFramesInFlight(), false);
    m_bufferPool.resize(m_logicalDevice->getMaxFramesInFlight() * 3, {});
  }

  CameraTexture::~CameraTexture()
  {
    stopCamera();

    for (auto & i : m_bufferPool)
    {
      if (i.buffer)
      {
        m_logicalDevice->destroyImageView(i.imageView);
        m_logicalDevice->destroyImage(i.image);
        m_logicalDevice->freeMemory(i.memory);
        AHardwareBuffer_release(i.buffer);
      }
    }

    m_logicalDevice->destroySamplerYcbcrConversion(m_ycbcrConversion);

    m_logicalDevice->destroySampler(m_ycbcrSampler);

    m_logicalDevice->destroyDescriptorSetLayout(m_descriptorSetLayout);
  }

  bool CameraTexture::isCameraOpen() const
  {
    return m_camDevice != nullptr;
  }

  VkDescriptorSetLayout CameraTexture::getDescriptorSetLayout() const
  {
    return m_descriptorSetLayout;
  }

  void CameraTexture::startCamera(const int viewWidth,
                                  const int viewHeight)
  {
    m_camManager = ACameraManager_create();

    // Pick back-facing camera
    ACameraIdList* cameraIdList = nullptr;
    ACameraManager_getCameraIdList(m_camManager, &cameraIdList);

    std::string cameraIdStr;
    for (int i = 0; i < cameraIdList->numCameras; i++) {
      ACameraMetadata* metadata = nullptr;
      ACameraManager_getCameraCharacteristics(m_camManager, cameraIdList->cameraIds[i], &metadata);
      ACameraMetadata_const_entry entry{};
      ACameraMetadata_getConstEntry(metadata, ACAMERA_LENS_FACING, &entry);
      if (entry.data.u8[0] == ACAMERA_LENS_FACING_BACK) {
        cameraIdStr = cameraIdList->cameraIds[i];
        ACameraMetadata_free(metadata);
        break;
      }
      ACameraMetadata_free(metadata);
    }

    // Pick best YUV size matching aspect ratio
    ACameraMetadata* metadata = nullptr;
    ACameraManager_getCameraCharacteristics(m_camManager, cameraIdStr.c_str(), &metadata);

    ACameraMetadata_const_entry orientationEntry{};
    ACameraMetadata_getConstEntry(metadata, ACAMERA_SENSOR_ORIENTATION, &orientationEntry);
    const int sensorOrientation = orientationEntry.data.i32[0];
    const bool isRotated = sensorOrientation == 90 || sensorOrientation == 270;

    ACameraMetadata_const_entry sizesEntry{};
    ACameraMetadata_getConstEntry(metadata, ACAMERA_SCALER_AVAILABLE_STREAM_CONFIGURATIONS, &sizesEntry);

    ACameraMetadata_const_entry colorFilterEntry{};
    if (ACameraMetadata_getConstEntry(metadata,
                                      ACAMERA_SENSOR_INFO_COLOR_FILTER_ARRANGEMENT,
                                      &colorFilterEntry) == ACAMERA_OK)
    {
      m_colorFilterArrangement = colorFilterEntry.data.u8[0];
      LOGI("Color filter arrangement: %d", m_colorFilterArrangement);
    }

    const float portraitAspect = (viewWidth < viewHeight)
                                 ? (float)viewWidth  / (float)viewHeight
                                 : (float)viewHeight / (float)viewWidth;

    int bestWidth = 0, bestHeight = 0;
    float bestDiff = 1e9f;
    // Format is: format, width, height, input?, repeating quads
    for (uint32_t i = 0; i + 3 < sizesEntry.count; i += 4) {
      if (sizesEntry.data.i32[i]     != AIMAGE_FORMAT_YUV_420_888) continue;
      if (sizesEntry.data.i32[i + 3] != 0) continue; // skip input streams
      int w = sizesEntry.data.i32[i + 1];
      int h = sizesEntry.data.i32[i + 2];

      // Cap the max resolution
//      if (w > 1920 || h > 1080) continue;
      if (w > 1280 || h > 720) continue;

      float aspect = isRotated ? (float)h / (float)w : (float)w / (float)h;
      float diff = std::abs(aspect - portraitAspect);
      if (diff < bestDiff) { bestDiff = diff; bestWidth = w; bestHeight = h; }
    }
    ACameraMetadata_free(metadata);
    ACameraManager_deleteCameraIdList(cameraIdList);

    LOGI("Best Width: %d, Best Height: %d", bestWidth, bestHeight);

    // ImageReader
    AImageReader_newWithUsage(
      bestWidth, bestHeight,
      AIMAGE_FORMAT_YUV_420_888,
      AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE,
      static_cast<int32_t>(m_bufferPool.size()),
      &m_imageReader
    );

    AImageReader_ImageListener listener{ this, onImageAvailable };
    AImageReader_setImageListener(m_imageReader, &listener);
    AImageReader_getWindow(m_imageReader, &m_surface);
    ANativeWindow_acquire(m_surface);

    // Open camera
    m_deviceCallbacks = {
      .context = this,
      .onDisconnected = [](void*, ACameraDevice* dev) { ACameraDevice_close(dev); },
      .onError         = [](void*, ACameraDevice* dev, int) { ACameraDevice_close(dev); }
    };
    ACameraManager_openCamera(m_camManager, cameraIdStr.c_str(), &m_deviceCallbacks, &m_camDevice);

    if (m_camDevice == nullptr)
    {
      return;
    }

    // Capture request
    ACameraDevice_createCaptureRequest(m_camDevice, TEMPLATE_PREVIEW, &m_request);
    ACameraOutputTarget* outputTarget = nullptr;
    ACameraOutputTarget_create(m_surface, &outputTarget);
    ACaptureRequest_addTarget(m_request, outputTarget);

    // Session
    ACaptureSessionOutputContainer* outputContainer = nullptr;
    ACaptureSessionOutputContainer_create(&outputContainer);
    ACaptureSessionOutput* sessionOutput = nullptr;
    ACaptureSessionOutput_create(m_surface, &sessionOutput);
    ACaptureSessionOutputContainer_add(outputContainer, sessionOutput);

    m_sessionCallbacks = {
      .context = this,
      .onClosed  = nullptr,
      .onReady   = nullptr,
      .onActive  = nullptr
    };
    ACameraDevice_createCaptureSession(m_camDevice, outputContainer, &m_sessionCallbacks, &m_camSession);
    ACameraCaptureSession_setRepeatingRequest(m_camSession, nullptr, 1, &m_request, nullptr);

    // Container and output only needed for setup
    ACaptureSessionOutputContainer_free(outputContainer);
    ACaptureSessionOutput_free(sessionOutput);
    ACameraOutputTarget_free(outputTarget);
  }

  void CameraTexture::stopCamera()
  {
    if (m_camSession) {
      ACameraCaptureSession_stopRepeating(m_camSession);
      ACameraCaptureSession_close(m_camSession);
      m_camSession = nullptr;
    }
    if (m_request) {
      ACaptureRequest_free(m_request);
      m_request = nullptr;
    }
    if (m_camDevice) {
      ACameraDevice_close(m_camDevice);
      m_camDevice = nullptr;
    }
    if (m_imageReader) {
      AImageReader_delete(m_imageReader);
      m_imageReader = nullptr;
    }
    if (m_surface) {
      ANativeWindow_release(m_surface);
      m_surface = nullptr;
    }
    if (m_camManager) {
      ACameraManager_delete(m_camManager);
      m_camManager = nullptr;
    }

    // Drain any pending buffer
    std::lock_guard lock(m_bufferMutex);
    if (m_pendingBuffer) {
      AHardwareBuffer_release(m_pendingBuffer);
      m_pendingBuffer = nullptr;
    }
  }

  void CameraTexture::updateCameraTexture()
  {
    AHardwareBuffer* buffer = nullptr;
    {
      std::lock_guard lock(m_bufferMutex);
      if (!m_pendingBuffer) return;  // no new frame
      buffer = m_pendingBuffer;
      m_pendingBuffer = nullptr;
    }

    // updateFromHardwareBuffer does acquire internally,
    // so release our camera-side ref after
    updateFromHardwareBuffer(buffer);
    AHardwareBuffer_release(buffer);
  }

  void CameraTexture::importBuffer(AHardwareBuffer* hardwareBuffer,
                                   ImportedBuffer& slot)
  {
    AHardwareBuffer_Desc ahbDesc = {};
    AHardwareBuffer_describe(hardwareBuffer, &ahbDesc);

    VkAndroidHardwareBufferFormatPropertiesANDROID formatProperties = {
      .sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_FORMAT_PROPERTIES_ANDROID
    };
    VkAndroidHardwareBufferPropertiesANDROID ahbProperties = {
      .sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID,
      .pNext = &formatProperties
    };

    m_logicalDevice->getAndroidHardwareBufferPropertiesANDROID(hardwareBuffer, &ahbProperties);

    createYCBCRResources(formatProperties);

    if (slot.image == VK_NULL_HANDLE) {
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

      slot.image = m_logicalDevice->createImage(imageInfo);
    }

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

    m_logicalDevice->allocateMemory(memoryAllocateInfo, slot.memory);

    m_logicalDevice->bindImageMemory(slot.image, slot.memory, 0);

    if (slot.imageView == VK_NULL_HANDLE) {
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

      slot.imageView = m_logicalDevice->createImageView(viewInfo);
    }

    slot.buffer = hardwareBuffer;
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

    const bool needsSwap =
      m_colorFilterArrangement == ACAMERA_SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG ||
      m_colorFilterArrangement == ACAMERA_SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR;

    const VkComponentMapping componentMapping = needsSwap ? VkComponentMapping{
        VK_COMPONENT_SWIZZLE_B,
        VK_COMPONENT_SWIZZLE_G,
        VK_COMPONENT_SWIZZLE_R,
        VK_COMPONENT_SWIZZLE_A
      } : VkComponentMapping{
        VK_COMPONENT_SWIZZLE_IDENTITY,
        VK_COMPONENT_SWIZZLE_IDENTITY,
        VK_COMPONENT_SWIZZLE_IDENTITY,
        VK_COMPONENT_SWIZZLE_IDENTITY
      };

      VkSamplerYcbcrConversionCreateInfo ycbcrInfo = {
      .sType = VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_CREATE_INFO,
      .pNext = &ycbcrExtFormat,
      .format = VK_FORMAT_UNDEFINED,
      .ycbcrModel = formatProperties.suggestedYcbcrModel,
      .ycbcrRange = formatProperties.suggestedYcbcrRange,
      .components = componentMapping,
      .xChromaOffset = formatProperties.suggestedXChromaOffset,
      .yChromaOffset = formatProperties.suggestedYChromaOffset,
      .chromaFilter = (formatProperties.formatFeatures &
                       VK_FORMAT_FEATURE_SAMPLED_IMAGE_YCBCR_CONVERSION_LINEAR_FILTER_BIT)
                       ? VK_FILTER_LINEAR : VK_FILTER_NEAREST,
      .forceExplicitReconstruction = VK_FALSE
    };

    m_ycbcrConversion = m_logicalDevice->createSamplerYcbcrConversion(ycbcrInfo);

    VkSamplerYcbcrConversionInfo conversionInfo = {
      .sType = VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_INFO,
      .conversion = m_ycbcrConversion
    };

    VkSamplerCreateInfo samplerInfo = {
      .sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO,
      .pNext = &conversionInfo,
      .magFilter = (formatProperties.formatFeatures &
                    VK_FORMAT_FEATURE_SAMPLED_IMAGE_YCBCR_CONVERSION_LINEAR_FILTER_BIT)
                   ? VK_FILTER_LINEAR : VK_FILTER_NEAREST,
      .minFilter = (formatProperties.formatFeatures &
                    VK_FORMAT_FEATURE_SAMPLED_IMAGE_YCBCR_CONVERSION_LINEAR_FILTER_BIT)
                   ? VK_FILTER_LINEAR : VK_FILTER_NEAREST,
      .mipmapMode = (formatProperties.formatFeatures &
                     VK_FORMAT_FEATURE_SAMPLED_IMAGE_YCBCR_CONVERSION_LINEAR_FILTER_BIT)
                    ? VK_SAMPLER_MIPMAP_MODE_LINEAR : VK_SAMPLER_MIPMAP_MODE_NEAREST,
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

    m_ycbcrSampler = m_logicalDevice->createSampler(samplerInfo);
  }

  void CameraTexture::onImageAvailable(void* ctx,
                                       AImageReader* reader)
  {
    auto* self = static_cast<CameraTexture*>(ctx);

    AImage* image = nullptr;
    if (AImageReader_acquireLatestImage(reader, &image) != AMEDIA_OK) return;

    AHardwareBuffer* buffer = nullptr;
    AImage_getHardwareBuffer(image, &buffer);

    // Acquire before releasing the image so the buffer outlives it
    AHardwareBuffer_acquire(buffer);
    AImage_delete(image);

    std::lock_guard lock(self->m_bufferMutex);
    if (self->m_pendingBuffer) {
      AHardwareBuffer_release(self->m_pendingBuffer);
    }
    self->m_pendingBuffer = buffer;
  }

  void CameraTexture::updateFromHardwareBuffer(AHardwareBuffer* buffer)
  {
    // Evict the oldest slot
    ImportedBuffer& slot = m_bufferPool[m_poolIndex];
    m_poolIndex = (m_poolIndex + 1) % static_cast<int>(m_bufferPool.size());

    if (slot.buffer != nullptr) {
      m_logicalDevice->freeMemory(slot.memory);
      AHardwareBuffer_release(slot.buffer);
    }

    AHardwareBuffer_acquire(buffer);
    importBuffer(buffer, slot);

    m_textureImage          = slot.image;
    m_textureImageView      = slot.imageView;
    m_imageInfo.imageView   = slot.imageView;
    m_imageInfo.imageLayout = VK_IMAGE_LAYOUT_GENERAL;
    m_imageInfo.sampler     = VK_NULL_HANDLE;

    if (!m_descriptorSet)
    {
      const VkDescriptorSetLayoutBinding binding {
        .binding            = 0,
        .descriptorType     = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
        .descriptorCount    = 1,
        .stageFlags         = VK_SHADER_STAGE_FRAGMENT_BIT,
        .pImmutableSamplers = &m_ycbcrSampler
      };

      const VkDescriptorSetLayoutCreateInfo layoutInfo {
        .sType        = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO,
        .bindingCount = 1,
        .pBindings    = &binding
      };
      m_descriptorSetLayout = m_logicalDevice->createDescriptorSetLayout(layoutInfo);
      createDescriptorSet(m_descriptorPool, m_descriptorSetLayout);
    }

    markAllFramesDirty();
  }

  void CameraTexture::markAllFramesDirty()
  {
    std::fill(m_dirtyFrames.begin(), m_dirtyFrames.end(), true);
  }

  void CameraTexture::flushDescriptorUpdate(size_t frame)
  {
    if (!m_dirtyFrames[frame]) return;

    m_descriptorSet->updateDescriptorSet(frame, [this](VkDescriptorSet descriptorSet) {
      return std::vector<VkWriteDescriptorSet>{
        getWriteDescriptorSet(0, descriptorSet)
      };
    });

    m_dirtyFrames[frame] = false;
  }

} // namespace ge