#ifndef PLANEAR_RENDERER_H
#define PLANEAR_RENDERER_H

#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class CommandBuffer;
  class Framebuffer;
  class LogicalDevice;
  class MousePickingFramebuffer;
  class RenderPass;
  class Swapchain;
  class SwapchainFramebuffer;

  class Renderer
  {
  public:
    Renderer(std::shared_ptr<LogicalDevice> logicalDevice,
                             const std::shared_ptr<Swapchain>& swapchain,
                             VkCommandPool commandPool);

    [[nodiscard]] std::shared_ptr<RenderPass> getRenderPass() const;

    [[nodiscard]] std::shared_ptr<RenderPass> getMousePickingRenderPass() const;

    void resetSwapchainImageResources(const std::shared_ptr<Swapchain>& swapchain);

    void resetMousePickingImageResources(VkExtent2D mousePickingExtent);

    void beginSwapchainRendering(uint32_t imageIndex,
                                 VkExtent2D extent,
                                 const std::shared_ptr<CommandBuffer>& commandBuffer,
                                 const std::shared_ptr<Swapchain>& swapchain);

    void beginMousePickingRendering(uint32_t imageIndex,
                                    VkExtent2D extent,
                                    const std::shared_ptr<CommandBuffer>& commandBuffer);

    static void endRendering(const std::shared_ptr<CommandBuffer>& commandBuffer);

  protected:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    std::shared_ptr<SwapchainFramebuffer> m_framebuffer;
    std::shared_ptr<MousePickingFramebuffer> m_mousePickingFramebuffer;

    std::shared_ptr<RenderPass> m_renderPass;
    std::shared_ptr<RenderPass> m_mousePickingRenderPass;
  };

} // ge

#endif //PLANEAR_RENDERER_H
