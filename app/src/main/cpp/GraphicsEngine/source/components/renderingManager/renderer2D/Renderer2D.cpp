#include "Renderer2D.h"
#include "../../assets/AssetManager.h"
#include "../../assets/fonts/Font.h"
#include "../../assets/textures/CameraTexture.h"
#include "../../assets/textures/ImageTexture.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../pipelines/GraphicsPipeline.h"
#include "../../pipelines/PipelineManager.h"
#include "../LegacyRenderer.h"
#include <algorithm>
#include <glm/gtc/matrix_transform.hpp>

#include "../../../../Logger.h"

namespace ge {
  Renderer2D::Renderer2D(std::shared_ptr<AssetManager> assetManager)
    : m_assetManager(std::move(assetManager))
  {}

  void Renderer2D::createNewFrame()
  {
    m_currentZ = 0.01f;

    fill(255);

    resetMatrix();

    rectMode(RectMode::CORNER);

    ellipseMode(EllipseMode::CENTER);

    imageMode(ImageMode::CORNER);

    textAlign(TextAlignH::LEFT, TextAlignV::BASELINE);

    m_drawList.clear();
  }

  void Renderer2D::render(const std::shared_ptr<PipelineManager>& pipelineManager,
                          const RenderInfo* renderInfo)
  {
    normalizeZValues();

    std::stable_sort(m_drawList.begin(), m_drawList.end(), [](const DrawEntry& a, const DrawEntry& b) {
      return a.z > b.z;
    });

    PipelineType currentPipeline = PipelineType::rect;
    bool firstDraw = true;

    auto bindIfNeeded = [&](PipelineType type)
    {
      if (firstDraw || currentPipeline != type)
      {
        pipelineManager->bindGraphicsPipeline(renderInfo->commandBuffer, type);
        currentPipeline = type;
        firstDraw = false;
      }
    };

    for (const auto& entry : m_drawList)
    {
      std::visit([&](const auto& cmd) {
        using T = std::decay_t<decltype(cmd)>;

        if constexpr (std::is_same_v<T, Rect>)
        {
          bindIfNeeded(PipelineType::rect);
          renderRect(pipelineManager, renderInfo, cmd);
        }
        else if constexpr (std::is_same_v<T, Triangle>)
        {
          bindIfNeeded(PipelineType::triangle);
          renderTriangle(pipelineManager, renderInfo, cmd);
        }
        else if constexpr (std::is_same_v<T, Ellipse>)
        {
          bindIfNeeded(PipelineType::ellipse);
          renderEllipse(pipelineManager, renderInfo, cmd);
        }
        else if constexpr (std::is_same_v<T, GlyphCommand>)
        {
          bindIfNeeded(PipelineType::font);
          renderGlyph(pipelineManager, renderInfo, cmd);
        }
        else if constexpr (std::is_same_v<T, Image>)
        {
          bindIfNeeded(PipelineType::image);
          renderImage(pipelineManager, renderInfo, cmd);
        }
      }, entry.command);
    }

    //

    if (!m_assetManager->getCameraTexture() ||
        m_assetManager->getCameraTexture()->getDescriptorSet(renderInfo->currentFrame) == VK_NULL_HANDLE)
    {
      LOGE("Camera texture not found!");
      return;
    }

    if (!pipelineManager->hasCameraPipeline())
    {
      pipelineManager->createCameraPipeline(m_assetManager->getCameraTexture()->getDescriptorSetLayout());
    }

    pipelineManager->bindGraphicsPipelineDescriptorSet(
      renderInfo->commandBuffer,
      PipelineType::camera,
      m_assetManager->getCameraTexture()->getDescriptorSet(renderInfo->currentFrame),
      0
    );

    Image image {
      .bounds {
        100, 100, 800, 800
      },
      .transform = glm::mat4(1.0),
      .z = 0.99f
    };

    const auto imagePC = image.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::image,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(imagePC),
      &imagePC
    );

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }

  void Renderer2D::fill(const float r,
                        const float g,
                        const float b,
                        const float a)
  {
    m_currentFill = glm::vec4(
      r / 255.0f,
      g / 255.0f,
      b / 255.0f,
      a / 255.0f
    );
  }

  void Renderer2D::fill(const float rgb,
                        const float a)
  {
    float rgbScaled = rgb / 255.0f;

    m_currentFill = glm::vec4(
      rgbScaled,
      rgbScaled,
      rgbScaled,
      a / 255.0f
    );
  }

  void Renderer2D::rotate(float angle)
  {
    m_currentTransform *= glm::rotate(glm::mat4(1.0), glm::radians(angle), {0.0f, 0.0f, 1.0f});
  }

  void Renderer2D::translate(float x,
                             float y)
  {
    m_currentTransform *= glm::translate(glm::mat4(1.0), {x, y, 0});
  }

  void Renderer2D::scale(float xy)
  {
    m_currentTransform *= glm::scale(glm::mat4(1.0), glm::vec3(xy));
  }

  void Renderer2D::scale(float x,
                         float y)
  {
    m_currentTransform *= glm::scale(glm::mat4(1.0), {x, y, 1});
  }

  void Renderer2D::pushMatrix()
  {
    m_transformStack.push_back(m_currentTransform);
  }

  void Renderer2D::popMatrix()
  {
    if (m_transformStack.empty())
    {
      return;
    }

    m_currentTransform = m_transformStack.back();
    m_transformStack.pop_back();
  }

  void Renderer2D::resetMatrix()
  {
    m_transformStack.clear();

    m_currentTransform = glm::mat4(1.0f);
  }

  void Renderer2D::rectMode(const RectMode mode)
  {
    m_rectMode = mode;
  }

  void Renderer2D::ellipseMode(const EllipseMode mode)
  {
    m_ellipseMode = mode;
  }

  void Renderer2D::imageMode(const ImageMode mode)
  {
    m_imageMode = mode;
  }

  void Renderer2D::rect(float x,
                        float y,
                        float width,
                        float height,
                        float radius)
  {
    const auto bounds = resolveRectBounds(x, y, width, height);

    m_drawList.push_back({
      Rect{
        .bounds = bounds,
        .color = m_currentFill,
        .transform = m_currentTransform,
        .z = m_currentZ,
        .radius = radius
      },
      m_currentZ
    });

    increaseCurrentZ();
  }

  void Renderer2D::triangle(const float x1,
                            const float y1,
                            const float x2,
                            const float y2,
                            const float x3,
                            const float y3)
  {
    m_drawList.push_back({
      Triangle{
        .p1 = glm::vec2(x1, y1),
        .p2 = glm::vec2(x2, y2),
        .p3 = glm::vec2(x3, y3),
        .color = m_currentFill,
        .transform = m_currentTransform,
        .z = m_currentZ
      },
      m_currentZ
    });

    increaseCurrentZ();
  }

  void Renderer2D::ellipse(const float x,
                           const float y,
                           const float width,
                           const float height)
  {
    const auto bounds = resolveEllipseBounds(x, y, width, height);

    m_drawList.push_back({
      Ellipse{
        .bounds = bounds,
        .color = m_currentFill,
        .transform = m_currentTransform,
        .z = m_currentZ
      },
      m_currentZ
    });

    increaseCurrentZ();
  }

  void Renderer2D::textFont(const std::string &font)
  {
    m_currentFontName = font;

    updateCurrentFont();
  }

  void Renderer2D::textFont(const std::string &font, uint32_t size)
  {
    m_currentFontName = font;
    m_currentFontSize = size;

    updateCurrentFont();
  }

  void Renderer2D::textSize(uint32_t size)
  {
    m_currentFontSize = size;

    updateCurrentFont();
  }

  void Renderer2D::textAlign(const TextAlignH h,
                             const TextAlignV v)
  {
    m_textAlignH = h;
    m_textAlignV = v;
  }

  float Renderer2D::textWidth(const std::string& text) const
  {
    if (!m_currentFont)
    {
      return 0.0f;
    }

    float width = 0.0f;
    for (const auto codepoint : decodeUTF8(text))
    {
      if (const auto glyphInfo = m_currentFont->getGlyphInfo(codepoint))
      {
        width += glyphInfo->advance;
      }
    }

    return width;
  }

  float Renderer2D::textAscent(const std::string &text) const
  {
    if (!m_currentFont)
    {
      return 0.0f;
    }

    float maxAscent = 0.0f;
    for (const auto codepoint : decodeUTF8(text))
    {
      if (const auto glyphInfo = m_currentFont->getGlyphInfo(codepoint))
      {
        maxAscent = std::max(maxAscent, glyphInfo->bearingY);
      }
    }

    return maxAscent;
  }

  float Renderer2D::textDescent(const std::string &text) const
  {
    if (!m_currentFont)
    {
      return 0.0f;
    }

    float maxDescent = 0.0f;
    for (const auto codepoint : decodeUTF8(text))
    {
      if (const auto glyphInfo = m_currentFont->getGlyphInfo(codepoint))
      {
        const float descent = glyphInfo->height - glyphInfo->bearingY;
        maxDescent = std::max(maxDescent, descent);
      }
    }

    return maxDescent;
  }

  void Renderer2D::text(const std::string& text,
                        float x,
                        float y)
  {
    float xOffset = 0.0f;
    if (m_textAlignH == TextAlignH::CENTER || m_textAlignH == TextAlignH::RIGHT)
    {
      const float stringWidth = textWidth(text);
      xOffset = (m_textAlignH == TextAlignH::CENTER) ? -stringWidth * 0.5f : -stringWidth;
    }

    float yOffset = 0.0f;
    const float ascent  = textAscent(text);
    const float descent = textDescent(text);
    const float height  = ascent + descent;

    switch (m_textAlignV)
    {
      case TextAlignV::BASELINE:
        yOffset = 0.0f;
        break;

      case TextAlignV::TOP:
        yOffset = ascent;
        break;

      case TextAlignV::CENTER:
        yOffset = ascent - height * 0.5f;
        break;

      case TextAlignV::BOTTOM:
        yOffset = -descent;
        break;
    }

    float currentX = x + xOffset;
    const float adjustedY = y + yOffset;

    // All glyphs in a text() call share the same Z so they sort together
    const float textZ = m_currentZ;

    for (const auto codepoint : decodeUTF8(text))
    {
      if (const auto glyphInfo = m_currentFont->getGlyphInfo(codepoint))
      {
        m_drawList.push_back({
          GlyphCommand{
            .glyph = {
              .bounds = glm::vec4(
                currentX + glyphInfo->bearingX,
                adjustedY - glyphInfo->bearingY,
                glyphInfo->width,
                glyphInfo->height
              ),
              .color = m_currentFill,
              .transform = m_currentTransform,
              .uv = glm::vec4(
                glyphInfo->u0,
                glyphInfo->v0,
                glyphInfo->u1,
                glyphInfo->v1
              ),
              .z = textZ
            },
            .fontName = m_currentFontName,
            .fontSize = m_currentFontSize
          },
          textZ
        });

        currentX += glyphInfo->advance;
      }
    }

    increaseCurrentZ();
  }

  void Renderer2D::image(std::string image,
                         float x,
                         float y,
                         float width,
                         float height)
  {
    const auto bounds = resolveImageBounds(x, y, width, height);

    m_drawList.push_back({
      Image{
        .imageName = std::move(image),
        .bounds = bounds,
        .transform = m_currentTransform,
        .z = m_currentZ
      },
      m_currentZ
    });

    increaseCurrentZ();
  }

  std::shared_ptr<AssetManager> Renderer2D::getAssetManager() const
  {
    return m_assetManager;
  }

  glm::vec4 Renderer2D::resolveRectBounds(float a,
                                          float b,
                                          float c,
                                          float d)
  {
    switch(m_rectMode)
    {
      case RectMode::CORNER:
        return { a, b, c, d };

      case RectMode::CORNERS:
        return { a, b, c - a, d - b };

      case RectMode::CENTER:
        return { a - c * 0.5f, b - d * 0.5f, c, d };

      case RectMode::RADIUS:
        return { a - c, b - d, c * 2.0f, d * 2.0f };
    }

    return { a, b, c, d };
  }

  glm::vec4 Renderer2D::resolveEllipseBounds(float a,
                                             float b,
                                             float c,
                                             float d)
  {
    switch(m_ellipseMode)
    {
      case EllipseMode::CENTER:
        return { a, b, c, d };

      case EllipseMode::RADIUS:
        return { a, b, c * 2.0f, d * 2.0f };

      case EllipseMode::CORNER:
        return { a + c * 0.5f, b + d * 0.5f, c, d };

      case EllipseMode::CORNERS:
        const float w = c - a;
        const float h = d - b;
        return { a + w * 0.5f, b + h * 0.5f, w, h };
    }

    return { a, b, c, d };
  }

  glm::vec4 Renderer2D::resolveImageBounds(float a,
                                           float b,
                                           float c,
                                           float d)
  {
    switch(m_imageMode)
    {
      case ImageMode::CORNER:
        return { a, b, c, d };

      case ImageMode::CORNERS:
        return { a, b, c - a, d - b };

      case ImageMode::CENTER:
        return { a - c * 0.5f, b - d * 0.5f, c, d };
    }

    return { a, b, c, d };
  }

  void Renderer2D::updateCurrentFont()
  {
    m_currentFont = m_assetManager->getFont(m_currentFontName, m_currentFontSize);
  }

  void Renderer2D::increaseCurrentZ()
  {
    m_currentZ += 1.0f;
  }

  void Renderer2D::normalizeZValues()
  {
    for (auto& entry : m_drawList)
    {
      float normalized = entry.z / m_currentZ;
      float flipped = 1.0f - normalized;

      entry.z = flipped;

      std::visit([flipped](auto& cmd) {
        using T = std::decay_t<decltype(cmd)>;

        if constexpr (std::is_same_v<T, GlyphCommand>)
        {
          cmd.glyph.z = flipped;
        }
        else
        {
          cmd.z = flipped;
        }
      }, entry.command);
    }
  }

  void Renderer2D::renderRect(const std::shared_ptr<PipelineManager>& pipelineManager,
                              const RenderInfo* renderInfo,
                              const Rect& rect)
  {
    const auto rectPC = rect.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::rect,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(rectPC),
      &rectPC
    );

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }

  void Renderer2D::renderTriangle(const std::shared_ptr<PipelineManager>& pipelineManager,
                                  const RenderInfo* renderInfo,
                                  const Triangle& triangle)
  {
    const auto trianglePC = triangle.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::triangle,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(trianglePC),
      &trianglePC
    );

    renderInfo->commandBuffer->draw(3, 1, 0, 0);
  }

  void Renderer2D::renderEllipse(const std::shared_ptr<PipelineManager>& pipelineManager,
                                 const RenderInfo* renderInfo,
                                 const Ellipse& ellipse)
  {
    const auto ellipsePC = ellipse.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::ellipse,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(ellipsePC),
      &ellipsePC
    );

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }

  void Renderer2D::renderGlyph(const std::shared_ptr<PipelineManager>& pipelineManager,
                               const RenderInfo* renderInfo,
                               const GlyphCommand& glyphCmd) const
  {
    const auto descriptorSet = m_assetManager->getFont(glyphCmd.fontName, glyphCmd.fontSize)->getDescriptorSet(renderInfo->currentFrame);

    pipelineManager->bindGraphicsPipelineDescriptorSet(
      renderInfo->commandBuffer,
      PipelineType::font,
      descriptorSet,
      0
    );

    const auto glyphPC = glyphCmd.glyph.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::font,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(glyphPC),
      &glyphPC
    );

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }

  void Renderer2D::renderImage(const std::shared_ptr<PipelineManager>& pipelineManager,
                               const RenderInfo* renderInfo,
                               const Image& image) const
  {
    auto imageTexture = m_assetManager->getImage(image.imageName);

    pipelineManager->bindGraphicsPipelineDescriptorSet(
      renderInfo->commandBuffer,
      PipelineType::image,
      imageTexture->getDescriptorSet(renderInfo->currentFrame),
      0
    );

    const auto imagePC = image.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::image,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(imagePC),
      &imagePC
    );

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }
} // ge