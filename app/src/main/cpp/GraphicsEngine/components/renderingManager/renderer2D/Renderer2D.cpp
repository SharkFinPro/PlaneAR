#include "Renderer2D.h"
#include "../../pipelines/implementations/FontPipeline.h"
#include "../../pipelines/implementations/QuadPipeline.h"
#include "../../renderingManager/LegacyRenderer.h"
#include <glm/gtc/matrix_transform.hpp>

namespace ge {
  Renderer2D::Renderer2D(const std::shared_ptr<LogicalDevice>& logicalDevice,
                         const std::shared_ptr<Surface>& surface,
                         const std::shared_ptr<Renderer>& renderer,
                         VkCommandPool commandPool,
                         AAssetManager* assetManager,
                         VkDescriptorPool descriptorPool)
  {
    m_quadPipeline = std::make_shared<QuadPipeline>(logicalDevice, renderer->getRenderPass(), assetManager, surface);

    m_fontPipeline = std::make_shared<FontPipeline>(logicalDevice, renderer->getRenderPass(), assetManager, commandPool, descriptorPool, surface);
  }

  void Renderer2D::createNewFrame()
  {
    m_quadPipeline->createNewFrame();
    m_fontPipeline->createNewFrame();

    m_currentTransform = glm::mat4(1.0f);
  }

  void Renderer2D::render(const std::shared_ptr<CommandBuffer>& commandBuffer,
                          uint32_t currentFrame)
  {
    m_quadPipeline->render(commandBuffer);

    m_fontPipeline->render(commandBuffer, currentFrame);
  }

  void Renderer2D::fill(float r, float g, float b)
  {
    m_currentFill = {
      .r = r / 255.0f,
      .g = g / 255.0f,
      .b = b / 255.0f
    };
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
      m_currentTransform
    );
  }

  void Renderer2D::text(std::string message, float x, float y)
  {
    m_fontPipeline->queueTextToRender(
      std::move(message),
      x,
      y,
      m_currentFill.r,
      m_currentFill.g,
      m_currentFill.b
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
} // ge