#ifndef PLANEAR_CAMERATEXTURE_H
#define PLANEAR_CAMERATEXTURE_H

#include "ImageTexture.h"
#include <android/hardware_buffer.h>
#include <unordered_map>
#include <vulkan/vulkan_android.h>

namespace ge {

  class CameraTexture final : public ImageTexture
  {
  public:
    CameraTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                  VkDescriptorPool descriptorPool,
                  VkDescriptorSetLayout descriptorSetLayout,
                  VkCommandPool commandPool,
                  uint32_t width,
                  uint32_t height);

    ~CameraTexture() override;

    // Call each ARCore frame
    void updateFromHardwareBuffer(AHardwareBuffer* buffer);

  private:
    VkCommandPool m_commandPool;

    struct ImportedBuffer {
      VkImage        image     = VK_NULL_HANDLE;
      VkDeviceMemory memory    = VK_NULL_HANDLE;
      VkImageView    imageView = VK_NULL_HANDLE;
    };

    std::unordered_map<AHardwareBuffer*, ImportedBuffer> m_bufferPool;
    AHardwareBuffer* m_currentBuffer = nullptr;
    VkSamplerYcbcrConversion m_ycbcrConversion = VK_NULL_HANDLE;

    uint32_t m_width;
    uint32_t m_height;

    ImportedBuffer importBuffer(AHardwareBuffer* buffer);
    void createYcbcrSampler(const VkAndroidHardwareBufferFormatPropertiesANDROID& formatProps);
  };

} // ge

#endif //PLANEAR_CAMERATEXTURE_H