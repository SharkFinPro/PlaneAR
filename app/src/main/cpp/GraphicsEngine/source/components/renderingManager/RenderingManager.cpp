#include "RenderingManager.h"
#include "Renderer.h"
#include "renderer2D/Renderer2D.h"
#include "../commandBuffer/CommandBuffer.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../pipelines/GraphicsPipeline.h"
#include "../surface/Swapchain.h"
#include <utility>

namespace ge {

  RenderingManager::RenderingManager(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                     const std::shared_ptr<Surface>& surface,
                                     std::shared_ptr<AssetManager> assetManager,
                                     VkCommandPool commandPool)
    : m_logicalDevice(logicalDevice), m_surface(surface), m_commandPool(commandPool)
  {
    m_swapchain = std::make_shared<Swapchain>(m_logicalDevice, m_surface);
    m_logicalDevice->createSyncObjects(m_swapchain);

    m_swapchainCommandBuffer = std::make_shared<CommandBuffer>(m_logicalDevice, m_commandPool);

    m_mousePickingCommandBuffer = std::make_shared<CommandBuffer>(m_logicalDevice, m_commandPool);

    m_renderer = std::make_shared<Renderer>(m_logicalDevice, m_swapchain, m_commandPool);

    m_renderer2D = std::make_shared<Renderer2D>(m_logicalDevice, std::move(assetManager));
  }

  void RenderingManager::doRendering(const std::shared_ptr<PipelineManager>& pipelineManager,
                                     uint32_t currentFrame)
  {
    m_logicalDevice->waitForGraphicsFences(currentFrame);

    uint32_t imageIndex;
    auto result = m_logicalDevice->acquireNextImage(currentFrame, m_swapchain, &imageIndex);

    if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR)
    {
      throw std::runtime_error("failed to acquire swap chain image!");
    }

    m_logicalDevice->resetGraphicsFences(currentFrame);

    doMousePicking(pipelineManager, currentFrame);

    m_swapchainCommandBuffer->setCurrentFrame(currentFrame);
    m_swapchainCommandBuffer->resetCommandBuffer();
    recordSwapchainCommandBuffer(pipelineManager, currentFrame, imageIndex);
    m_logicalDevice->submitGraphicsQueue(currentFrame, imageIndex, m_swapchainCommandBuffer);

    result = m_logicalDevice->queuePresent(imageIndex, m_swapchain);

    if (result != VK_SUCCESS)
    {
      throw std::runtime_error("failed to present swap chain image!");
    }
  }

  void RenderingManager::createNewFrame()
  {
    m_renderer2D->createNewFrame();
  }

  std::shared_ptr<Renderer> RenderingManager::getRenderer() const
  {
    return m_renderer;
  }

  std::shared_ptr<Renderer2D> RenderingManager::getRenderer2D()
  {
    return m_renderer2D;
  }

  void RenderingManager::recordSwapchainCommandBuffer(const std::shared_ptr<PipelineManager>& pipelineManager,
                                                      uint32_t currentFrame,
                                                      uint32_t imageIndex) const
  {
    m_swapchainCommandBuffer->record([this, pipelineManager, currentFrame, imageIndex]
    {
      RenderInfo renderInfo {
        .commandBuffer = m_swapchainCommandBuffer,
        .currentFrame = currentFrame,
        .extent = m_swapchain->getExtent()
      };

      m_renderer->beginSwapchainRendering(imageIndex, renderInfo.extent, renderInfo.commandBuffer, m_swapchain);

      const VkViewport viewport = {
        .x = 0.0f,
        .y = 0.0f,
        .width = static_cast<float>(renderInfo.extent.width),
        .height = static_cast<float>(renderInfo.extent.height),
        .minDepth = 0.0f,
        .maxDepth = 1.0f
      };
      renderInfo.commandBuffer->setViewport(viewport);

      const VkRect2D scissor = {
        .offset = {0, 0},
        .extent = renderInfo.extent
      };
      renderInfo.commandBuffer->setScissor(scissor);

      m_renderer2D->render(pipelineManager, &renderInfo);

      ge::Renderer::endRendering(renderInfo.commandBuffer);
    });
  }

  void RenderingManager::recordMousePickingCommandBuffer(const std::shared_ptr<PipelineManager>& pipelineManager,
                                                         uint32_t currentFrame) const
  {
    m_mousePickingCommandBuffer->record([this, pipelineManager, currentFrame]
    {
      const RenderInfo renderInfo {
        .commandBuffer = m_mousePickingCommandBuffer,
        .currentFrame = currentFrame,
        .extent = m_swapchain->getExtent(),
      };

      if (renderInfo.extent.width == 0 || renderInfo.extent.height == 0)
      {
        return;
      }

      m_renderer->beginMousePickingRendering(currentFrame, renderInfo.extent, renderInfo.commandBuffer);

      const VkViewport viewport = {
        .x = 0.0f,
        .y = 0.0f,
        .width = static_cast<float>(renderInfo.extent.width),
        .height = static_cast<float>(renderInfo.extent.height),
        .minDepth = 0.0f,
        .maxDepth = 1.0f
      };
      renderInfo.commandBuffer->setViewport(viewport);

      const VkRect2D scissor = {
        .offset = {0, 0},
        .extent = renderInfo.extent
      };
      renderInfo.commandBuffer->setScissor(scissor);

      m_renderer2D->renderMousePicking(pipelineManager, &renderInfo);

      ge::Renderer::endRendering(renderInfo.commandBuffer);
    });
  }

  void RenderingManager::doMousePicking(const std::shared_ptr<PipelineManager>& pipelineManager,
                                        uint32_t currentFrame) const
  {
    m_logicalDevice->resetMousePickingFences(currentFrame);

    m_mousePickingCommandBuffer->setCurrentFrame(currentFrame);
    m_mousePickingCommandBuffer->resetCommandBuffer();
    recordMousePickingCommandBuffer(pipelineManager, currentFrame);
    m_logicalDevice->submitMousePickingGraphicsQueue(currentFrame, m_mousePickingCommandBuffer->getCommandBuffer());

    m_logicalDevice->waitForMousePickingFences(currentFrame);
    m_renderer2D->handleRenderedMousePickingImage(m_renderer->getMousePickingColorImage());
  }
} // ge