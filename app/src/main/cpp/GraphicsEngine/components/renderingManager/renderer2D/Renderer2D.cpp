#include "Renderer2D.h"
#include "../../assets/AssetManager.h"
#include "../../assets/fonts/Font.h"
#include "../../pipelines/GraphicsPipeline.h"
#include "../../pipelines/implementations/FontPipeline.h"
#include "../../pipelines/implementations/QuadPipeline.h"
#include "../../renderingManager/LegacyRenderer.h"
#include <glm/gtc/matrix_transform.hpp>

namespace ge {
  Renderer2D::Renderer2D(const std::shared_ptr<LogicalDevice>& logicalDevice,
                         const std::shared_ptr<Surface>& surface,
                         const std::shared_ptr<Renderer>& renderer,
                         std::shared_ptr<AssetManager> assetManager,
                         VkCommandPool commandPool,
                         VkDescriptorPool descriptorPool)
    : m_assetManager(std::move(assetManager))
  {
    m_quadPipeline = std::make_shared<QuadPipeline>(logicalDevice, renderer->getRenderPass(), m_assetManager->getAAssetManager(), surface);

    m_fontPipeline = std::make_shared<FontPipeline>(logicalDevice, renderer->getRenderPass(), m_assetManager->getAAssetManager(), m_assetManager->getFontDescriptorSetLayout());
  }

  void Renderer2D::createNewFrame()
  {
    m_currentZ = 0.0f;

    m_quadPipeline->createNewFrame();

    m_currentTransform = glm::mat4(1.0f);

    m_glyphsToRender.clear();
  }

  void Renderer2D::render(const RenderInfo* renderInfo)
  {
    m_quadPipeline->render(renderInfo->commandBuffer);

    m_fontPipeline->render(renderInfo, &m_glyphsToRender, m_assetManager);
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

  void Renderer2D::rotate(float angle)
  {
    m_currentTransform *= glm::rotate(glm::mat4(1.0), glm::radians(angle), {0.0f, 0.0f, 1.0f});
  }

  void Renderer2D::translate(float x, float y)
  {
    m_currentTransform *= glm::translate(glm::mat4(1.0), {x, y, 0});
  }

  void Renderer2D::scale(float xy)
  {
    m_currentTransform *= glm::scale(glm::mat4(1.0), glm::vec3(xy));
  }

  void Renderer2D::scale(float x, float y)
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

  void Renderer2D::rect(float x, float y, float width, float height)
  {
    m_quadPipeline->queueRectToRender(
      x,
      y,
      width,
      height,
      m_currentFill.r,
      m_currentFill.g,
      m_currentFill.b,
      m_currentFill.a,
      m_currentTransform
    );
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

  void Renderer2D::text(std::string text,
                        float x,
                        float y)
  {
    const float maxGlyphHeight = m_currentFont->getMaxGlyphHeight();

    float currentX = x;

    for (const auto& character : text)
    {
      if (const auto glyphInfo = m_currentFont->getGlyphInfo(character))
      {
        m_glyphsToRender[m_currentFontName][m_currentFontSize].push_back({
          .bounds = glm::vec4(
            currentX + glyphInfo->bearingX,
            y - glyphInfo->bearingY + maxGlyphHeight,
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

  void Renderer2D::updateCurrentFont()
  {
    m_currentFont = m_assetManager->getFont(m_currentFontName, m_currentFontSize);
  }

  void Renderer2D::increaseCurrentZ()
  {
    m_currentZ += 1.0f;
  }
} // ge