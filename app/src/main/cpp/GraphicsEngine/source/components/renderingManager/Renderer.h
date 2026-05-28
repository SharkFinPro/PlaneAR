#ifndef PLANEAR_RENDERER_H
#define PLANEAR_RENDERER_H

#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class CommandBuffer;
  class LogicalDevice;
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

    void resetSwapchainImageResources(const std::shared_ptr<Swapchain>& swapchain);

    void beginSwapchainRendering(uint32_t imageIndex,
                                 VkExtent2D extent,
                                 const std::shared_ptr<CommandBuffer>& commandBuffer,
                                 const std::shared_ptr<Swapchain>& swapchain);

    static void endSwapchainRendering(uint32_t imageIndex,
                                      const std::shared_ptr<CommandBuffer>& commandBuffer,
                                      const std::shared_ptr<Swapchain>& swapchain);

  protected:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    std::shared_ptr<SwapchainFramebuffer> m_framebuffer;

    std::shared_ptr<RenderPass> m_renderPass;

    static void endRendering(const std::shared_ptr<CommandBuffer>& commandBuffer);
  };

} // ge

#endif //PLANEAR_RENDERER_H
