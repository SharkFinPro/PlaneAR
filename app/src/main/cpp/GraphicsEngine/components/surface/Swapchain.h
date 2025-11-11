#ifndef PLANEAR_SWAPCHAIN_H
#define PLANEAR_SWAPCHAIN_H

#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class LogicalDevice;
  class PhysicalDevice;
  class Surface;

  class Swapchain
  {
  public:
    Swapchain(const std::shared_ptr<LogicalDevice>& logicalDevice,
              const std::shared_ptr<Surface>& surface);

    ~Swapchain();

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;
    std::shared_ptr<Surface> m_surface;

    VkSwapchainKHR m_swapchain = VK_NULL_HANDLE;
  };

} // ge

#endif //PLANEAR_SWAPCHAIN_H
