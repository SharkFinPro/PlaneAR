#ifndef PLANEAR_MOUSEPICKINGFRAMEBUFFER_H
#define PLANEAR_MOUSEPICKINGFRAMEBUFFER_H

#include "Framebuffer.h"

namespace ge {

  class MousePickingFramebuffer final : public Framebuffer
  {
  public:
    MousePickingFramebuffer(std::shared_ptr<LogicalDevice> logicalDevice,
                            const VkCommandPool& commandPool,
                            const std::shared_ptr<RenderPass>& renderPass,
                            VkExtent2D extent);

  private:
    [[nodiscard]] VkFormat getColorFormat() override;

    [[nodiscard]] const std::vector<VkImageView>& getImageViews() override;

    [[nodiscard]] VkSampleCountFlagBits getSampleCount() override;
  };

} // ge

#endif //PLANEAR_MOUSEPICKINGFRAMEBUFFER_H
