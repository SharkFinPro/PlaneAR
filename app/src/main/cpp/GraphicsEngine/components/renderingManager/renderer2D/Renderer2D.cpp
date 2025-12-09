#include "Renderer2D.h"
#include "../pipelines/implementations/FontPipeline.h"
#include "../pipelines/implementations/QuadPipeline.h"
#include "../renderingManager/LegacyRenderer.h"

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
  }

  void Renderer2D::render(const std::shared_ptr<CommandBuffer>& commandBuffer,
                          uint32_t currentFrame)
  {
    m_quadPipeline->render(commandBuffer);

    m_fontPipeline->render(commandBuffer, currentFrame);
  }

  void Renderer2D::renderRect(float x,
                              float y,
                              float width,
                              float height,
                              float r,
                              float g,
                              float b)
  {
    m_quadPipeline->queueRectToRender(x, y, width, height, r, g, b);
  }

  void Renderer2D::renderText(std::string message,
                              float x,
                              float y,
                              float r,
                              float g,
                              float b)
  {
    m_fontPipeline->queueTextToRender(std::move(message), x, y, r, g, b);
  }
} // ge