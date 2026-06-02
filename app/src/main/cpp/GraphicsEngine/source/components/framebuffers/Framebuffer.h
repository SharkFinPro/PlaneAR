#ifndef PLANEAR_FRAMEBUFFER_H
#define PLANEAR_FRAMEBUFFER_H

#include <vulkan/vulkan.h>
#include <memory>
#include <vector>

namespace ge {

  class LogicalDevice;
  class RenderPass;

  class Framebuffer
  {
  public:
    inline explicit Framebuffer(std::shared_ptr<LogicalDevice> logicalDevice)
      : m_logicalDevice(std::move(logicalDevice))
    {}

    virtual ~Framebuffer();

    void initializeFramebuffer(const VkCommandPool& commandPool,
                               const std::shared_ptr<RenderPass>& renderPass,
                               VkExtent2D extent);

    VkFramebuffer& getFramebuffer(uint32_t imageIndex);

    VkImage& getColorImage();

  protected:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    std::vector<VkFramebuffer> m_framebuffers;

    VkImage m_depthImage = VK_NULL_HANDLE;
    VkDeviceMemory m_depthImageMemory = VK_NULL_HANDLE;
    VkImageView m_depthImageView = VK_NULL_HANDLE;

    VkImage m_colorImage = VK_NULL_HANDLE;
    VkDeviceMemory m_colorImageMemory = VK_NULL_HANDLE;
    VkImageView m_colorImageView = VK_NULL_HANDLE;

    void createDepthResources(const VkCommandPool& commandPool,
                              VkFormat depthFormat,
                              VkExtent2D extent);

    void createColorResources(VkExtent2D extent);

    void createFrameBuffers(const VkRenderPass& renderPass, VkExtent2D extent);

    [[nodiscard]] virtual VkFormat getColorFormat() = 0;

    [[nodiscard]] virtual const std::vector<VkImageView>& getImageViews() = 0;

    [[nodiscard]] virtual VkSampleCountFlagBits getSampleCount() = 0;
  };

} // ge

#endif //PLANEAR_FRAMEBUFFER_H
