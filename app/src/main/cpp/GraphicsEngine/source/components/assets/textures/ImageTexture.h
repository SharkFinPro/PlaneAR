#ifndef PLANEAR_IMAGETEXTURE_H
#define PLANEAR_IMAGETEXTURE_H

#include "Texture.h"
#include <memory>
#include <vector>

struct AAssetManager;

namespace ge {

  class DescriptorSet;

  class ImageTexture final : public Texture
  {
  public:
    ImageTexture(std::shared_ptr<LogicalDevice> logicalDevice,
                 AAssetManager* assetManager,
                 const std::string& fileName,
                 VkCommandPool commandPool,
                 VkDescriptorPool descriptorPool,
                 VkDescriptorSetLayout descriptorSetLayout);

    [[nodiscard]] VkDescriptorSet getDescriptorSet(uint32_t currentFrame) const;

  private:
    std::shared_ptr<DescriptorSet> m_descriptorSet;

    static std::vector<uint8_t> loadImageFromFile(AAssetManager* assetManager,
                                                  const std::string& fileName);

    void copyBufferToImage(const VkCommandPool& commandPool,
                           uint32_t width,
                           uint32_t height,
                           VkBuffer& stagingBuffer);

    void createImageView() override;

    void createDescriptorSet(VkDescriptorPool descriptorPool,
                             VkDescriptorSetLayout descriptorSetLayout);
  };

} // ge

#endif //PLANEAR_IMAGETEXTURE_H
