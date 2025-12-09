#ifndef PLANEAR_FONTPIPELINE_H
#define PLANEAR_FONTPIPELINE_H

#include "../GraphicsPipeline.h"

struct AAssetManager;

namespace ge {

  class CommandBuffer;
  class DescriptorSet;
  class GlyphTexture;

  class FontPipeline final : public GraphicsPipeline
  {
  public:
    FontPipeline(const std::shared_ptr<LogicalDevice>& logicalDevice,
                 std::shared_ptr<RenderPass> renderPass,
                 AAssetManager* assetManager,
                 VkCommandPool commandPool,
                 VkDescriptorPool descriptorPool);

    void render(const std::shared_ptr<CommandBuffer>& commandBuffer,
                uint32_t currentFrame);

  private:
    std::shared_ptr<DescriptorSet> m_fontDescriptorSet;

    std::shared_ptr<GlyphTexture> m_glyphTexture;

    std::unique_ptr<uint8_t[]> m_fontBuffer;
    size_t m_fontBufferSize;

    void createDescriptorSets(VkDescriptorPool descriptorPool);

    void bindDescriptorSets(const std::shared_ptr<CommandBuffer>& commandBuffer,
                            uint32_t currentFrame);

    void loadFont(AAssetManager* assetManager, VkCommandPool commandPool);

    void loadFontFromAsset(AAssetManager* assetManager);
  };

} // ge

#endif //PLANEAR_FONTPIPELINE_H
