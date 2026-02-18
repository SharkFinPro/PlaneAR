#include "Renderer2D.h"
#include "../../assets/AssetManager.h"
#include "../../assets/fonts/Font.h"
#include "../../assets/textures/ImageTexture.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../pipelines/GraphicsPipeline.h"
#include "../../pipelines/PipelineManager.h"
#include "../LegacyRenderer.h"
#include <glm/gtc/matrix_transform.hpp>

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

    m_rectsToRender.clear();

    m_trianglesToRender.clear();

    m_ellipsesToRender.clear();

    m_glyphsToRender.clear();

    m_imagesToRender.clear();
  }

  void Renderer2D::render(const std::shared_ptr<PipelineManager>& pipelineManager,
                          const RenderInfo* renderInfo)
  {
    normalizeZValues();

    renderRects(pipelineManager, renderInfo);

    renderTriangles(pipelineManager, renderInfo);

    renderEllipses(pipelineManager, renderInfo);

    renderGlyphs(pipelineManager, renderInfo);

    renderImages(pipelineManager, renderInfo);
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
                        float height)
  {
    const auto bounds = resolveRectBounds(x, y, width, height);

    m_rectsToRender.push_back({
      .bounds = bounds,
      .color = m_currentFill,
      .transform = m_currentTransform,
      .z = m_currentZ
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
    m_trianglesToRender.push_back({
      .p1 = glm::vec2(x1, y1),
      .p2 = glm::vec2(x2, y2),
      .p3 = glm::vec2(x3, y3),
      .color = m_currentFill,
      .transform = m_currentTransform,
      .z = m_currentZ
    });

    increaseCurrentZ();
  }

  void Renderer2D::ellipse(const float x,
                           const float y,
                           const float width,
                           const float height)
  {
    const auto bounds = resolveEllipseBounds(x, y, width, height);

    m_ellipsesToRender.push_back({
      .bounds = bounds,
      .color = m_currentFill,
      .transform = m_currentTransform,
      .z = m_currentZ
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
    m_textAlignH = h,
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

    for (const auto codepoint : decodeUTF8(text))
    {
      if (const auto glyphInfo = m_currentFont->getGlyphInfo(codepoint))
      {
        m_glyphsToRender[m_currentFontName][m_currentFontSize].push_back({
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
          .z = m_currentZ
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

    m_imagesToRender.push_back({
      .imageName = std::move(image),
      .bounds = bounds,
      .transform = m_currentTransform,
      .z = m_currentZ
    });

    increaseCurrentZ();
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
    for (auto& rect : m_rectsToRender)
    {
      rect.z /= m_currentZ;
      rect.z = 1.0f - rect.z;
    }

    for (auto& triangle : m_trianglesToRender)
    {
      triangle.z /= m_currentZ;
      triangle.z = 1.0f - triangle.z;
    }

    for (auto& ellipse : m_ellipsesToRender)
    {
      ellipse.z /= m_currentZ;
      ellipse.z = 1.0f - ellipse.z;
    }

    for (auto& font : m_glyphsToRender)
    {
      for (auto& fontSize : font.second)
      {
        for (auto& glyph : fontSize.second)
        {
          glyph.z /= m_currentZ;
          glyph.z = 1.0f - glyph.z;
        }
      }
    }

    for (auto& image : m_imagesToRender)
    {
      image.z /= m_currentZ;
      image.z = 1.0f - image.z;
    }
  }

  void Renderer2D::renderRects(const std::shared_ptr<PipelineManager>& pipelineManager,
                               const RenderInfo* renderInfo) const
  {
    pipelineManager->bindGraphicsPipeline(renderInfo->commandBuffer, PipelineType::rect);

    for (const auto& rect : m_rectsToRender)
    {
      renderRect(pipelineManager, renderInfo, rect);
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

  void Renderer2D::renderTriangles(const std::shared_ptr<PipelineManager>& pipelineManager,
                                   const RenderInfo* renderInfo) const
  {
    pipelineManager->bindGraphicsPipeline(renderInfo->commandBuffer, PipelineType::triangle);

    for (const auto& triangle : m_trianglesToRender)
    {
      renderTriangle(pipelineManager, renderInfo, triangle);
    }
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

  void Renderer2D::renderEllipses(const std::shared_ptr<PipelineManager>& pipelineManager,
                                  const RenderInfo* renderInfo) const
  {
    pipelineManager->bindGraphicsPipeline(renderInfo->commandBuffer, PipelineType::ellipse);

    for (const auto& ellipse : m_ellipsesToRender)
    {
      renderEllipse(pipelineManager, renderInfo, ellipse);
    }
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

  void Renderer2D::renderGlyphs(const std::shared_ptr<PipelineManager>& pipelineManager,
                                const RenderInfo* renderInfo) const
  {
    pipelineManager->bindGraphicsPipeline(renderInfo->commandBuffer, PipelineType::font);

    for (const auto& [fontName, fontSizes] : m_glyphsToRender)
    {
      for (const auto& [fontSize, text] : fontSizes)
      {
        const auto descriptorSet = m_assetManager->getFont(fontName, fontSize)->getDescriptorSet(renderInfo->currentFrame);

        pipelineManager->bindGraphicsPipelineDescriptorSet(
          renderInfo->commandBuffer,
          PipelineType::font,
          descriptorSet,
          0
        );

        for (const auto& glyph : text)
        {
          renderGlyph(pipelineManager, renderInfo, glyph);
        }
      }
    }
  }

  void Renderer2D::renderGlyph(const std::shared_ptr<PipelineManager>& pipelineManager,
                               const RenderInfo* renderInfo,
                               const Glyph& glyph)
  {
    const auto glyphPC = glyph.createPushConstant(renderInfo->extent);

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

  void Renderer2D::renderImages(const std::shared_ptr<PipelineManager>& pipelineManager,
                                const RenderInfo* renderInfo) const
  {
    pipelineManager->bindGraphicsPipeline(renderInfo->commandBuffer, PipelineType::image);

    for (const auto& image : m_imagesToRender)
    {
      renderImage(pipelineManager, renderInfo, image);
    }
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