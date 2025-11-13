#include "RenderingManager.h"
#include "LegacyRenderer.h"
#include "../commandBuffer/CommandBuffer.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../surface/Swapchain.h"

namespace ge {
  RenderingManager::RenderingManager(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                     const std::shared_ptr<Surface>& surface,
                                     VkCommandPool commandPool)
    : m_logicalDevice(logicalDevice), m_surface(surface), m_commandPool(commandPool)
  {
    m_swapchain = std::make_shared<Swapchain>(m_logicalDevice, m_surface);

    m_swapchainCommandBuffer = std::make_shared<CommandBuffer>(m_logicalDevice, m_commandPool);

    m_renderer = std::make_shared<LegacyRenderer>(m_logicalDevice, m_swapchain, m_commandPool);
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

    // TODO: Record swapchain command buffer

    m_logicalDevice->submitGraphicsQueue(currentFrame, m_swapchainCommandBuffer);

    result = m_logicalDevice->queuePresent(currentFrame, m_swapchain, &imageIndex);

    if (result != VK_SUCCESS)
    {
      throw std::runtime_error("failed to present swap chain image!");
    }
  }
} // ge