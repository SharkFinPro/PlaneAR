#include "RenderingManager.h"
#include "LegacyRenderer.h"
#include "../commandBuffer/CommandBuffer.h"
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
} // ge