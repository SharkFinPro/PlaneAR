#ifndef PLANEAR_GLYPHTEXTURE_H
#define PLANEAR_GLYPHTEXTURE_H

#include "Texture.h"

namespace ge {

  class GlyphTexture final : public Texture
  {
  public:
    GlyphTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                 const VkCommandPool& commandPool,
                 const unsigned char* pixelData,
                 uint32_t width,
                 uint32_t height);

    ~GlyphTexture() override = default;

  private:
    void createTextureImage(const VkCommandPool& commandPool,
                            const unsigned char* pixelData,
                            uint32_t width,
                            uint32_t height);

    void createAndFillStagingBuffer(const unsigned char* pixelData,
                                    uint32_t width,
                                    uint32_t height,
                                    VkBuffer& stagingBuffer,
                                    VkDeviceMemory& stagingBufferMemory);

    void createAndPrepareImage(const VkCommandPool& commandPool,
                               uint32_t width,
                               uint32_t height);

    void copyBufferToImage(const VkCommandPool& commandPool,
                           uint32_t width,
                           uint32_t height,
                           VkBuffer& stagingBuffer);

    void transitionImageToShaderReadable(const VkCommandPool& commandPool);

    void cleanupStagingBuffer(VkBuffer& stagingBuffer,
                              VkDeviceMemory& stagingBufferMemory);

    void createImageView() override;
  };

} // ge

#endif //PLANEAR_GLYPHTEXTURE_H
