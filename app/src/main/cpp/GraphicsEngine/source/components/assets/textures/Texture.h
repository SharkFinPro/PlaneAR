#ifndef PLANEAR_TEXTURE_H
#define PLANEAR_TEXTURE_H

#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class LogicalDevice;

  class Texture
  {
  public:
    Texture(std::shared_ptr<LogicalDevice> logicalDevice,
            VkSamplerAddressMode samplerAddressMode);

    virtual ~Texture();

    [[nodiscard]] VkDescriptorPoolSize getDescriptorPoolSize() const;

    [[nodiscard]] VkWriteDescriptorSet getDescriptorSet(uint32_t binding,
                                                        const VkDescriptorSet& dstSet) const;

  protected:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkImage m_textureImage = VK_NULL_HANDLE;
    VkDeviceMemory m_textureImageMemory = VK_NULL_HANDLE;
    VkImageView m_textureImageView = VK_NULL_HANDLE;
    VkSampler m_textureSampler = VK_NULL_HANDLE;

    VkDescriptorImageInfo m_imageInfo{};

    uint32_t m_mipLevels = 1;

    void createTextureSampler(VkSamplerAddressMode addressMode);

    virtual void createImageView() = 0;
  };

} // ge

#endif //PLANEAR_TEXTURE_H
