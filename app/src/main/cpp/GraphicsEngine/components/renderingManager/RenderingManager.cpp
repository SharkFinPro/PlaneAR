#include "RenderingManager.h"
#include "LegacyRenderer.h"
#include "renderer2D/Renderer2D.h"
#include "../commandBuffer/CommandBuffer.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../pipelines/GraphicsPipeline.h"
#include "../pipelines/implementations/FontPipeline.h"
#include "../pipelines/implementations/RectPipeline.h"
#include "../surface/Swapchain.h"
#include <utility>

namespace ge {

  RenderingManager::RenderingManager(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                     const std::shared_ptr<Surface>& surface,
                                     std::shared_ptr<AssetManager> assetManager,
                                     VkCommandPool commandPool,
                                     VkDescriptorPool descriptorPool)
    : m_logicalDevice(logicalDevice), m_surface(surface), m_commandPool(commandPool)
  {
    m_swapchain = std::make_shared<Swapchain>(m_logicalDevice, m_surface);
    m_logicalDevice->createSyncObjects(m_swapchain);

    m_swapchainCommandBuffer = std::make_shared<CommandBuffer>(m_logicalDevice, m_commandPool);

    m_renderer = std::make_shared<LegacyRenderer>(m_logicalDevice, m_swapchain, m_commandPool);

    m_renderer2D = std::make_shared<Renderer2D>(m_logicalDevice, m_renderer, std::move(assetManager), m_commandPool, descriptorPool);
  }

  void RenderingManager::doRendering(uint32_t currentFrame)
  {
    m_logicalDevice->waitForGraphicsFences(currentFrame);

    uint32_t imageIndex;
    auto result = m_logicalDevice->acquireNextImage(currentFrame, m_swapchain, &imageIndex);

    if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR)
    {
      throw std::runtime_error("failed to acquire swap chain image!");
    }

    m_logicalDevice->resetGraphicsFences(currentFrame);

    m_swapchainCommandBuffer->setCurrentFrame(currentFrame);
    m_swapchainCommandBuffer->resetCommandBuffer();
    recordSwapchainCommandBuffer(currentFrame, imageIndex);
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

  void RenderingManager::recordSwapchainCommandBuffer(uint32_t currentFrame, uint32_t imageIndex) const
  {
    m_swapchainCommandBuffer->record([this, currentFrame, imageIndex]()
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

      m_renderer2D->render(&renderInfo);

      m_renderer->endSwapchainRendering(imageIndex, renderInfo.commandBuffer, m_swapchain);
    });
  }
} // ge