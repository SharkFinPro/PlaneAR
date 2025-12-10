#ifndef PLANEAR_RENDERINGMANAGER_H
#define PLANEAR_RENDERINGMANAGER_H

#include <vulkan/vulkan.h>
#include <memory>

struct AAssetManager;

namespace ge {

  class CommandBuffer;
  class LogicalDevice;
  class Renderer;
  class Renderer2D;
  class Surface;
  class Swapchain;

  class RenderingManager
  {
  public:
    RenderingManager(const std::shared_ptr<LogicalDevice>& logicalDevice,
                     const std::shared_ptr<Surface>& surface,
                     VkCommandPool commandPool,
                     AAssetManager* assetManager,
                     VkDescriptorPool descriptorPool);

    void doRendering(uint32_t currentFrame);

    void createNewFrame();

    [[nodiscard]] std::shared_ptr<Renderer2D> getRenderer2D();

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    std::shared_ptr<Surface> m_surface;

    std::shared_ptr<Renderer> m_renderer;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    std::shared_ptr<Swapchain> m_swapchain;

    std::shared_ptr<CommandBuffer> m_swapchainCommandBuffer;

    std::shared_ptr<Renderer2D> m_renderer2D;

    void recordSwapchainCommandBuffer(uint32_t currentFrame, uint32_t imageIndex) const;
  };

} // ge

#endif //PLANEAR_RENDERINGMANAGER_H
