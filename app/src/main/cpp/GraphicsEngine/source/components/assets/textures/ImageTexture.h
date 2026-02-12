#ifndef PLANEAR_IMAGETEXTURE_H
#define PLANEAR_IMAGETEXTURE_H

#include "Texture.h"
#include <memory>

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

  private:
    std::shared_ptr<DescriptorSet> m_descriptorSet;

    void createImageView() override;
  };

} // ge

#endif //PLANEAR_IMAGETEXTURE_H
