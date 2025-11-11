#ifndef PLANEAR_RENDERINGMANAGER_H
#define PLANEAR_RENDERINGMANAGER_H

#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class LogicalDevice;
  class Surface;

  class RenderingManager
  {
  public:
    RenderingManager(const std::shared_ptr<LogicalDevice>& logicalDevice,
                     const std::shared_ptr<Surface>& surface,
                     VkCommandPool commandPool);

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    std::shared_ptr<Surface> m_surface;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;
  };

} // ge

#endif //PLANEAR_RENDERINGMANAGER_H
