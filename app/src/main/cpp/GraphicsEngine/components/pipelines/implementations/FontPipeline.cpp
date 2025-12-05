#include "FontPipeline.h"
#include "common/GraphicsPipelineStates.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../descriptorSet/DescriptorSet.h"
#include "../../textures/GlyphTexture.h"
#include <android/asset_manager.h>
#include <freetype/freetype.h>


namespace ge {
  FontPipeline::FontPipeline(const std::shared_ptr<LogicalDevice>& logicalDevice,
                             std::shared_ptr<RenderPass> renderPass,
                             AAssetManager* assetManager,
                             VkCommandPool commandPool,
                             VkDescriptorPool descriptorPool)
    : GraphicsPipeline(logicalDevice)
  {
    loadFont(assetManager, commandPool);

    createDescriptorSets(descriptorPool);

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
      .descriptorSetLayouts {
        m_fontDescriptorSet->getDescriptorSetLayout()
      },
      .renderPass = renderPass
    };

    createPipeline(graphicsPipelineOptions);
  }

  void FontPipeline::render(const std::shared_ptr<CommandBuffer>& commandBuffer, uint32_t currentFrame)
  {
    commandBuffer->bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, m_pipeline);

    bindDescriptorSets(commandBuffer, currentFrame);

    commandBuffer->draw(4, 1, 0, 0);
  }

  void FontPipeline::loadFont(AAssetManager* assetManager, VkCommandPool commandPool)
  {
    const char* fontPath = "fonts/Roboto-VariableFont_wdth,wght.ttf";

    AAsset* asset = AAssetManager_open(assetManager, fontPath, AASSET_MODE_BUFFER);
    if (!asset)
    {
      throw std::runtime_error(std::string("Failed to open asset: ") + fontPath);
    }

    FT_Library ft;
    if (FT_Init_FreeType(&ft)) {
      AAsset_close(asset);
      throw std::runtime_error("Failed to initialize FreeType");
    }

    const auto fontBufferSize = AAsset_getLength(asset);
    const void* fontBuffer = AAsset_getBuffer(asset);

    FT_Face face;
    if (FT_New_Memory_Face(ft, static_cast<const FT_Byte*>(fontBuffer), fontBufferSize, 0, &face))
    {
      FT_Done_FreeType(ft);
      AAsset_close(asset);
      throw std::runtime_error("Failed to load font from memory");
    }

    FT_Set_Pixel_Sizes(face, 0, 256);

    if (FT_Load_Char(face, 'H', FT_LOAD_RENDER))
    {
      throw std::runtime_error("Failed to load glyph");
    }

    m_glyphTexture = std::make_shared<GlyphTexture>(
      m_logicalDevice,
      commandPool,
      face->glyph->bitmap.buffer,
      face->glyph->bitmap.width,
      face->glyph->bitmap.rows
    );

    AAsset_close(asset);
  }

  void FontPipeline::createDescriptorSets(VkDescriptorPool descriptorPool)
  {
    constexpr VkDescriptorSetLayoutBinding glyphDescriptorSetLayoutBinding {
      .binding = 0,
      .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
      .descriptorCount = 1,
      .stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT
    };

    std::vector<VkDescriptorSetLayoutBinding> descriptorSetLayoutBindings {
      glyphDescriptorSetLayoutBinding
    };

    m_fontDescriptorSet = std::make_shared<DescriptorSet>(m_logicalDevice, descriptorPool, descriptorSetLayoutBindings);
    m_fontDescriptorSet->updateDescriptorSets([this](const VkDescriptorSet descriptorSet, const size_t frame)
    {
      std::vector<VkWriteDescriptorSet> descriptorWrites{{
        m_glyphTexture->getDescriptorSet(0, descriptorSet)
      }};

      return descriptorWrites;
    });
  }

  void FontPipeline::bindDescriptorSets(const std::shared_ptr<CommandBuffer>& commandBuffer, uint32_t currentFrame)
  {
    commandBuffer->bindDescriptorSets(
      VK_PIPELINE_BIND_POINT_GRAPHICS,
      m_pipelineLayout,
      0,
      1,
      &m_fontDescriptorSet->getDescriptorSet(currentFrame)
    );
  }
} // ge