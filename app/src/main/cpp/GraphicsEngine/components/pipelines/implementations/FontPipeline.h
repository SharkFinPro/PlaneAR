#ifndef PLANEAR_FONTPIPELINE_H
#define PLANEAR_FONTPIPELINE_H

#include "../GraphicsPipeline.h"
#include <glm/mat4x4.hpp>
#include <unordered_map>

namespace ge {

  class AssetManager;
  struct Glyph;

  struct GlyphPushConstant {
    glm::mat4 transform;
    int screenWidth;
    int screenHeight;
    float z;
    float x;
    float y;
    float width;
    float height;
    float u0, v0;
    float u1, v1;
    float r;
    float g;
    float b;
    float a;
  };

  class FontPipeline final : public GraphicsPipeline
  {
  public:
    FontPipeline(std::shared_ptr<LogicalDevice> logicalDevice,
                 std::shared_ptr<RenderPass> renderPass,
                 AAssetManager* assetManager,
                 VkDescriptorSetLayout fontDescriptorSetLayout);

    void render(const RenderInfo* renderInfo,
                const std::unordered_map<std::string, std::unordered_map<uint32_t, std::vector<Glyph>>>* glyphs,
                const std::shared_ptr<AssetManager>& assetManager);

  private:
    void renderGlyph(const RenderInfo* renderInfo,
                     const Glyph& glyph) const;
  };

} // ge

#endif //PLANEAR_FONTPIPELINE_H
