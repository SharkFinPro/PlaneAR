#ifndef PLANEAR_SWAPCHAINFRAMEBUFFER_H
#define PLANEAR_SWAPCHAINFRAMEBUFFER_H

#include "Framebuffer.h"

namespace ge {

  class Swapchain;

  class SwapchainFramebuffer final : public Framebuffer
  {
  public:
    SwapchainFramebuffer(std::shared_ptr<LogicalDevice> logicalDevice,
                         const std::shared_ptr<Swapchain>& swapchain,
                         const VkCommandPool& commandPool,
                         const std::shared_ptr<RenderPass>& renderPass,
                         VkExtent2D extent);

  private:
    std::shared_ptr<Swapchain> m_swapchain;

    [[nodiscard]] VkFormat getColorFormat() override;

    [[nodiscard]] const std::vector<VkImageView>& getImageViews() override;

    [[nodiscard]] VkSampleCountFlagBits getSampleCount() override;
  };

} // ge

#endif //PLANEAR_SWAPCHAINFRAMEBUFFER_H
