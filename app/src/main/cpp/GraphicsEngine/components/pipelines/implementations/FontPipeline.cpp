#include "FontPipeline.h"
#include "common/GraphicsPipelineStates.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../descriptorSet/DescriptorSet.h"
#include "../../surface/Surface.h"
#include "../../textures/GlyphTexture.h"
#include <android/asset_manager.h>
#include <utility>

constexpr uint32_t MAX_ASCII_CODE = 255;

const std::string FONT_PATH = "fonts/Roboto-VariableFont_wdth,wght.ttf";

namespace ge {
  FontPipeline::FontPipeline(const std::shared_ptr<LogicalDevice>& logicalDevice,
                             std::shared_ptr<RenderPass> renderPass,
                             AAssetManager* assetManager,
                             VkCommandPool commandPool,
                             VkDescriptorPool descriptorPool,
                             std::shared_ptr<Surface> surface)
    : GraphicsPipeline(logicalDevice), m_surface(std::move(surface))
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
      .pushConstantRanges {
        {
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
          .offset = 0,
          .size = sizeof(GlyphPushConstant)
        }
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

    for (const auto& [message, x, y, r, g, b, a, transformation] : m_textsToRender)
    {
      renderText(commandBuffer, message, x, y, r, g, b, a, transformation);
    }
  }

  void FontPipeline::queueTextToRender(std::string message,
                                       float x,
                                       float y,
                                       float r,
                                       float g,
                                       float b,
                                       float a,
                                       glm::mat4 transformation)
  {
    m_textsToRender.push_back({std::move(message), x, y, r, g, b, a, transformation});
  }

  void FontPipeline::createNewFrame()
  {
    m_textsToRender.clear();
  }

  void FontPipeline::renderText(const std::shared_ptr<CommandBuffer>& commandBuffer,
                                const std::string& message,
                                float x,
                                float y,
                                float r,
                                float g,
                                float b,
                                float a,
                                glm::mat4 transformation)
  {
    float currentX = x;

    for (const auto& character : message)
    {
      auto it = m_glyphMap.find(character);
      if (it != m_glyphMap.end())
      {
        renderGlyph(commandBuffer, character, currentX, y, r, g, b, a, transformation);

        currentX += it->second.advance;
      }
    }
  }

  void FontPipeline::renderGlyph(const std::shared_ptr<CommandBuffer>& commandBuffer,
                                 char character,
                                 float x,
                                 float y,
                                 float r,
                                 float g,
                                 float b,
                                 float a,
                                 glm::mat4 transformation)
  {
    auto it = m_glyphMap.find(character);
    if (it == m_glyphMap.end())
    {
      return;
    }

    const GlyphInfo& glyph = it->second;

    const GlyphPushConstant glyphPushConstant {
      .transformation = transformation,
      .screenWidth = m_surface->getWidth(),
      .screenHeight = m_surface->getHeight(),
      .x = x + glyph.bearingX,
      .y = y - glyph.bearingY + m_maxGlyphHeight,
      .width = glyph.width,
      .height = glyph.height,
      .u0 = glyph.u0,
      .v0 = glyph.v0,
      .u1 = glyph.u1,
      .v1 = glyph.v1,
      .r = r,
      .g = g,
      .b = b,
      .a = a
    };

    commandBuffer->pushConstants(m_pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                                 0, sizeof(GlyphPushConstant), &glyphPushConstant);

    commandBuffer->draw(4, 1, 0, 0);
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

  void FontPipeline::loadFont(AAssetManager* assetManager, VkCommandPool commandPool)
  {
    loadFontFromAsset(assetManager);

    createGlyphAtlas(commandPool);
  }

  void FontPipeline::loadFontFromAsset(AAssetManager* assetManager)
  {
    AAsset* asset = AAssetManager_open(assetManager, FONT_PATH.c_str(), AASSET_MODE_BUFFER);
    if (!asset)
    {
      throw std::runtime_error(std::string("Failed to open asset: ") + FONT_PATH);
    }

    m_fontBufferSize = AAsset_getLength(asset);
    const void* fontBufferPtr = AAsset_getBuffer(asset);

    m_fontBuffer = std::make_unique<uint8_t[]>(m_fontBufferSize);
    std::memcpy(m_fontBuffer.get(), fontBufferPtr, m_fontBufferSize);

    AAsset_close(asset);
  }

  void FontPipeline::createGlyphAtlas(VkCommandPool commandPool)
  {
    FT_Library ft;
    if (FT_Init_FreeType(&ft))
    {
      throw std::runtime_error("Failed to initialize FreeType");
    }

    FT_Face face;
    if (FT_New_Memory_Face(ft, m_fontBuffer.get(), static_cast<FT_Long>(m_fontBufferSize), 0, &face))
    {
      FT_Done_FreeType(ft);
      throw std::runtime_error("Failed to load font from memory");
    }

    FT_Set_Pixel_Sizes(face, 0, 64);

    const auto charset = getCharset(face);

    uint32_t maxGlyphWidth, maxGlyphHeight, glyphsPerRow, atlasWidth, atlasHeight;
    auto atlasBuffer = createAtlasBuffer(face, charset, maxGlyphWidth, maxGlyphHeight,
                                         glyphsPerRow, atlasWidth, atlasHeight);


    populateAtlasBuffer(face, charset, atlasBuffer, maxGlyphWidth, maxGlyphHeight,
                        glyphsPerRow, atlasWidth, atlasHeight);

    m_glyphTexture = std::make_shared<GlyphTexture>(
      m_logicalDevice,
      commandPool,
      atlasBuffer.data(),
      atlasWidth,
      atlasHeight
    );

    m_maxGlyphHeight = static_cast<float>(maxGlyphHeight);
  }

  std::vector<FT_ULong> FontPipeline::getCharset(FT_Face face)
  {
    std::vector<FT_ULong> charset;

    FT_ULong charcode;
    FT_UInt gindex;

    charcode = FT_Get_First_Char(face, &gindex);
    while (gindex != 0)
    {
      if (charcode <= MAX_ASCII_CODE)
      {
        charset.push_back(charcode);
      }
      charcode = FT_Get_Next_Char(face, charcode, &gindex);
    }

    return charset;
  }

  std::vector<uint8_t> FontPipeline::createAtlasBuffer(FT_Face face,
                                                       const std::vector<FT_ULong>& charset,
                                                       uint32_t& maxGlyphWidth,
                                                       uint32_t& maxGlyphHeight,
                                                       uint32_t& glyphsPerRow,
                                                       uint32_t& atlasWidth,
                                                       uint32_t& atlasHeight)
  {
    maxGlyphWidth = 0;
    maxGlyphHeight = 0;

    for (FT_ULong charcode : charset)
    {
      if (FT_Load_Char(face, charcode, FT_LOAD_RENDER))
      {
        continue;
      }
      maxGlyphWidth = std::max(maxGlyphWidth, face->glyph->bitmap.width);
      maxGlyphHeight = std::max(maxGlyphHeight, face->glyph->bitmap.rows);
    }

    glyphsPerRow = static_cast<uint32_t>(std::ceil(std::sqrt(charset.size())));
    atlasWidth = glyphsPerRow * maxGlyphWidth;
    atlasHeight = glyphsPerRow * maxGlyphHeight;

    std::vector<uint8_t> atlasBuffer(atlasWidth * atlasHeight, 0);

    return atlasBuffer;
  }

  void FontPipeline::populateAtlasBuffer(FT_Face face,
                                         const std::vector<FT_ULong>& charset,
                                         std::vector<uint8_t>& atlasBuffer,
                                         uint32_t maxGlyphWidth,
                                         uint32_t maxGlyphHeight,
                                         uint32_t glyphsPerRow,
                                         uint32_t atlasWidth,
                                         uint32_t atlasHeight)
  {
    uint32_t x = 0;
    uint32_t y = 0;
    uint32_t currentGlyph = 0;

    for (FT_ULong charcode : charset)
    {
      if (FT_Load_Char(face, charcode, FT_LOAD_RENDER))
      {
        continue;
      }

      const FT_Bitmap& bitmap = face->glyph->bitmap;

      for (uint32_t row = 0; row < bitmap.rows; ++row)
      {
        for (uint32_t col = 0; col < bitmap.width; ++col)
        {
          const uint32_t atlasX = x + col;
          const uint32_t atlasY = y + row;
          const uint32_t atlasIndex = atlasY * atlasWidth + atlasX;
          const uint32_t bitmapIndex = row * bitmap.width + col;

          if (atlasIndex < atlasBuffer.size())
          {
            atlasBuffer[atlasIndex] = bitmap.buffer[bitmapIndex];
          }
        }
      }

      m_glyphMap[static_cast<char>(charcode)] = {
        .u0 = static_cast<float>(x) / static_cast<float>(atlasWidth),
        .v0 = static_cast<float>(y) / static_cast<float>(atlasHeight),
        .u1 = static_cast<float>(x + bitmap.width) / static_cast<float>(atlasWidth),
        .v1 = static_cast<float>(y + bitmap.rows) / static_cast<float>(atlasHeight),
        .width = static_cast<float>(bitmap.width),
        .height = static_cast<float>(bitmap.rows),
        .bearingX = static_cast<float>(face->glyph->bitmap_left),
        .bearingY = static_cast<float>(face->glyph->bitmap_top),
        .advance = static_cast<float>(face->glyph->advance.x >> 6)
      };

      x += maxGlyphWidth;
      currentGlyph++;

      if (currentGlyph % glyphsPerRow == 0)
      {
        x = 0;
        y += maxGlyphHeight;
      }
    }
  }
} // ge