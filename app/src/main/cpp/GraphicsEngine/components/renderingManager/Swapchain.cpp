#include "Swapchain.h"
#include "../logicalDevice/LogicalDevice.h"

namespace ge {
  Swapchain::Swapchain(const std::shared_ptr<LogicalDevice>& logicalDevice,
                       const std::shared_ptr<Surface>& surface)
    : m_logicalDevice(logicalDevice), m_surface(surface)
  {

  }

  Swapchain::~Swapchain()
  {
    m_logicalDevice->destroySwapchainKHR(m_swapchain);
  }
} // ge