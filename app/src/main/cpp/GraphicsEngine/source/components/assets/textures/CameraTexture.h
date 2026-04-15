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

    [[nodiscard]] VkDescriptorSetLayout getDescriptorSetLayout() const;

  private:
    VkCommandPool m_commandPool;

    VkDescriptorPool m_descriptorPool = VK_NULL_HANDLE;
    VkDescriptorSetLayout m_descriptorSetLayout = VK_NULL_HANDLE;

    struct ImportedBuffer {
      VkImage        image     = VK_NULL_HANDLE;
      VkDeviceMemory memory    = VK_NULL_HANDLE;
      VkImageView    imageView = VK_NULL_HANDLE;
    };

    ImportedBuffer m_imageData;
    AHardwareBuffer* m_currentBuffer = nullptr;

    VkSampler m_ycbcrSampler = VK_NULL_HANDLE;
    VkSamplerYcbcrConversion m_ycbcrConversion = VK_NULL_HANDLE;

    ImportedBuffer importBuffer(AHardwareBuffer* buffer);

    void createYCBCRResources(const VkAndroidHardwareBufferFormatPropertiesANDROID& format_props);
  };

} // ge

#endif //PLANEAR_CAMERATEXTURE_H