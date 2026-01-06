#include "FontPipeline.h"
#include "common/GraphicsPipelineStates.h"
#include "../../assets/AssetManager.h"
#include "../../assets/fonts/Font.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../renderingManager/renderer2D/Renderer2D.h"

namespace ge {
  FontPipeline::FontPipeline(std::shared_ptr<LogicalDevice> logicalDevice,
                             std::shared_ptr<RenderPass> renderPass,
                             AAssetManager* assetManager,
                             VkDescriptorSetLayout fontDescriptorSetLayout)
    : GraphicsPipeline(std::move(logicalDevice))
  {
    const GraphicsPipelineOptions graphicsPipelineOptions {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/font.vert.spv",
        .fragmentShader = "shaders/font.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendStateTransparent,
        .depthStencilState = gps::depthStencilStateNone,
        .dynamicState = gps::dynamicState,
        .inputAssemblyState = gps::inputAssemblyStateTriangleStrip,
        .multisampleState = gps::getMultsampleState(m_logicalDevice),
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = gps::vertexInputStateRaw,
        .viewportState = gps::viewportState
      },
      .pushConstantRanges {
        {
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
          .offset = 0,
          .size = sizeof(GlyphPushConstant)
        }
      },
      .descriptorSetLayouts {
        fontDescriptorSetLayout
      },
      .renderPass = renderPass
    };

    createPipeline(graphicsPipelineOptions);
  }

  void FontPipeline::render(const RenderInfo* renderInfo,
                            const std::unordered_map<std::string, std::unordered_map<uint32_t, std::vector<Glyph>>>* glyphs,
                            const std::shared_ptr<AssetManager>& assetManager)
  {
    renderInfo->commandBuffer->bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, m_pipeline);

    for (const auto& [fontName, fontSizes] : *glyphs)
    {
      for (const auto& [fontSize, text] : fontSizes)
      {
        auto descriptorSet = assetManager->getFont(fontName, fontSize)->getDescriptorSet(renderInfo->currentFrame);

        renderInfo->commandBuffer->bindDescriptorSets(
          VK_PIPELINE_BIND_POINT_GRAPHICS,
          m_pipelineLayout,
          0,
          1,
          &descriptorSet
        );

        for (const auto& glyph : text)
        {
          renderGlyph(renderInfo, glyph);
        }
      }
    }
  }

  void FontPipeline::renderGlyph(const RenderInfo* renderInfo, const Glyph& glyph) const
  {
    const GlyphPushConstant glyphPC {
      .transform = glyph.transform,
      .screenWidth = static_cast<int>(renderInfo->extent.width),
      .screenHeight = static_cast<int>(renderInfo->extent.height),
      .z = glyph.z,
      .x = glyph.bounds.x,
      .y = glyph.bounds.y,
      .width = glyph.bounds.z,
      .height = glyph.bounds.w,
      .u0 = glyph.uv.x,
      .v0 = glyph.uv.y,
      .u1 = glyph.uv.z,
      .v1 = glyph.uv.w,
      .r = glyph.color.r,
      .g = glyph.color.g,
      .b = glyph.color.b,
      .a = glyph.color.a
    };

    renderInfo->commandBuffer->pushConstants(m_pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                                             0, sizeof(GlyphPushConstant), &glyphPC);

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }
} // ge