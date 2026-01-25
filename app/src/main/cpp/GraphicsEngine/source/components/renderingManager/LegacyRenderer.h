#ifndef PLANEAR_LEGACYRENDERER_H
#define PLANEAR_LEGACYRENDERER_H

#include "Renderer.h"
#include <memory>

namespace ge {

  class RenderPass;
  class Swapchain;
  class SwapchainFramebuffer;

  class LegacyRenderer final : public Renderer
  {
  public:
    LegacyRenderer(const std::shared_ptr<LogicalDevice>& logicalDevice,
                   const std::shared_ptr<Swapchain>& swapchain,
                   VkCommandPool commandPool);

    [[nodiscard]] std::shared_ptr<RenderPass> getRenderPass() const override;

    void resetSwapchainImageResources(std::shared_ptr<Swapchain> swapchain) override;

    void beginSwapchainRendering(uint32_t imageIndex,
                                 VkExtent2D extent,
                                 std::shared_ptr<CommandBuffer> commandBuffer,
                                 std::shared_ptr<Swapchain> swapchain) override;

    void endSwapchainRendering(uint32_t imageIndex,
                               std::shared_ptr<CommandBuffer> commandBuffer,
                               std::shared_ptr<Swapchain> swapchain) override;

  private:
    std::shared_ptr<SwapchainFramebuffer> m_framebuffer;

    std::shared_ptr<RenderPass> m_renderPass;

    static void endRendering(const std::shared_ptr<CommandBuffer>& commandBuffer);
  };

} // ge

#endif //PLANEAR_LEGACYRENDERER_H
