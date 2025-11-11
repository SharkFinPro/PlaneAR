#ifndef PLANEAR_RENDERER_H
#define PLANEAR_RENDERER_H

#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class CommandBuffer;
  class LogicalDevice;
  class RenderPass;
  class Swapchain;

  class Renderer
  {
  public:
    inline explicit Renderer(const std::shared_ptr<LogicalDevice>& logicalDevice,
                             VkCommandPool commandPool)
      : m_logicalDevice(logicalDevice), m_commandPool(commandPool)
    {}

    virtual ~Renderer() = default;

    [[nodiscard]] virtual std::shared_ptr<RenderPass> getRenderPass() const = 0;

    [[nodiscard]] virtual VkDescriptorSet& getOffscreenImageDescriptorSet(uint32_t imageIndex) = 0;

    virtual void resetSwapchainImageResources(std::shared_ptr<Swapchain> swapchain) = 0;

    virtual void resetOffscreenImageResources(VkExtent2D offscreenViewportExtent) = 0;

    virtual void beginSwapchainRendering(uint32_t imageIndex, VkExtent2D extent,
                                         std::shared_ptr<CommandBuffer> commandBuffer,
                                         std::shared_ptr<Swapchain> swapchain) = 0;

    virtual void beginOffscreenRendering(uint32_t imageIndex, VkExtent2D extent,
                                         std::shared_ptr<CommandBuffer> commandBuffer) = 0;

    virtual void endSwapchainRendering(uint32_t imageIndex,
                                       std::shared_ptr<CommandBuffer> commandBuffer,
                                       std::shared_ptr<Swapchain> swapchain) = 0;

    virtual void endOffscreenRendering(uint32_t imageIndex,
                                       std::shared_ptr<CommandBuffer> commandBuffer) = 0;

  protected:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;
  };

} // ge

#endif //PLANEAR_RENDERER_H
