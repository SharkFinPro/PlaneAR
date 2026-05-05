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

    [[nodiscard]] VkDescriptorSetLayout getDescriptorSetLayout() const;

    void startCamera(int viewWidth,
                     int viewHeight);

    void stopCamera();

    void updateCameraTexture();

  private:
    VkCommandPool m_commandPool;

    VkDescriptorPool m_descriptorPool = VK_NULL_HANDLE;
    VkDescriptorSetLayout m_descriptorSetLayout = VK_NULL_HANDLE;

    struct ImportedBuffer {
      VkImage image = VK_NULL_HANDLE;
      VkDeviceMemory memory = VK_NULL_HANDLE;
      VkImageView imageView = VK_NULL_HANDLE;
    };

    ImportedBuffer m_imageData;
    AHardwareBuffer* m_currentBuffer = nullptr;

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

    static void onImageAvailable(void* ctx,
                                 AImageReader* reader);

    ImportedBuffer importBuffer(AHardwareBuffer* buffer);

    void createYCBCRResources(const VkAndroidHardwareBufferFormatPropertiesANDROID& formatProperties);

    void updateFromHardwareBuffer(AHardwareBuffer* buffer);
  };

} // ge

#endif //PLANEAR_CAMERATEXTURE_H