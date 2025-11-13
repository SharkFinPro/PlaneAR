#ifndef PLANEAR_RENDERINGMANAGER_H
#define PLANEAR_RENDERINGMANAGER_H

#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class CommandBuffer;
  class LogicalDevice;
  class Renderer;
  class Surface;
  class Swapchain;

  class RenderingManager
  {
  public:
    RenderingManager(const std::shared_ptr<LogicalDevice>& logicalDevice,
                     const std::shared_ptr<Surface>& surface,
                     VkCommandPool commandPool);

    void doRendering(uint32_t currentFrame);

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    std::shared_ptr<Surface> m_surface;

    std::shared_ptr<Renderer> m_renderer;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    std::shared_ptr<Swapchain> m_swapchain;

    std::shared_ptr<CommandBuffer> m_swapchainCommandBuffer;

    void recordOffscreenCommandBuffer(uint32_t imageIndex) const;
  };

} // ge

#endif //PLANEAR_RENDERINGMANAGER_H
