#ifndef PLANEAR_CAMERATEXTURE_H
#define PLANEAR_CAMERATEXTURE_H

#include "ImageTexture.h"
#include <android/hardware_buffer.h>
#include <camera/NdkCameraManager.h>
#include <camera/NdkCameraDevice.h>
#include <camera/NdkCameraCaptureSession.h>
#include <media/NdkImageReader.h>
#include <vulkan/vulkan_android.h>
#include <mutex>
#include <unordered_map>

namespace ge {

  class CameraTexture final : public ImageTexture
  {
  public:
    CameraTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                  VkDescriptorPool descriptorPool,
                  VkCommandPool commandPool);

    ~CameraTexture() override;

    [[nodiscard]] bool isCameraOpen() const;

    [[nodiscard]] VkDescriptorSetLayout getDescriptorSetLayout() const;

    void startCamera(int viewWidth,
                     int viewHeight);

    void stopCamera();

    void updateCameraTexture();

    void flushDescriptorUpdate(size_t frame);

  private:
    VkCommandPool m_commandPool;

    VkDescriptorPool m_descriptorPool = VK_NULL_HANDLE;
    VkDescriptorSetLayout m_descriptorSetLayout = VK_NULL_HANDLE;

    struct ImportedBuffer {
      VkImage        image     = VK_NULL_HANDLE;
      VkDeviceMemory memory    = VK_NULL_HANDLE;
      VkImageView    imageView = VK_NULL_HANDLE;
      AHardwareBuffer* buffer  = nullptr;
    };

    std::vector<ImportedBuffer> m_bufferPool;
    int m_poolIndex = 0;

    VkSampler m_ycbcrSampler = VK_NULL_HANDLE;
    VkSamplerYcbcrConversion m_ycbcrConversion = VK_NULL_HANDLE;

    ACameraManager*          m_camManager  = nullptr;
    ACameraDevice*           m_camDevice   = nullptr;
    ACameraCaptureSession*   m_camSession  = nullptr;
    ACaptureRequest*         m_request     = nullptr;
    AImageReader*            m_imageReader = nullptr;
    ANativeWindow*           m_surface     = nullptr;

    AHardwareBuffer*         m_pendingBuffer = nullptr;
    std::mutex               m_bufferMutex;

    ACameraDevice_StateCallbacks          m_deviceCallbacks{};
    ACameraCaptureSession_stateCallbacks  m_sessionCallbacks{};

    std::vector<bool> m_dirtyFrames; // one entry per frame-in-flight

    uint8_t m_colorFilterArrangement = 0;

    static void onImageAvailable(void* ctx,
                                 AImageReader* reader);

    ImportedBuffer importBuffer(AHardwareBuffer* buffer);

    void createYCBCRResources(const VkAndroidHardwareBufferFormatPropertiesANDROID& formatProperties);

    void updateFromHardwareBuffer(AHardwareBuffer* buffer);

    void markAllFramesDirty();
  };

} // ge

#endif //PLANEAR_CAMERATEXTURE_H