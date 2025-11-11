#include "RenderingManager.h"
#include "../surface/Swapchain.h"

namespace ge {
  RenderingManager::RenderingManager(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                     const std::shared_ptr<Surface>& surface,
                                     VkCommandPool commandPool)
    : m_logicalDevice(logicalDevice), m_surface(surface), m_commandPool(commandPool)
  {
    m_swapchain = std::make_shared<Swapchain>(m_logicalDevice, m_surface);
  }
} // ge