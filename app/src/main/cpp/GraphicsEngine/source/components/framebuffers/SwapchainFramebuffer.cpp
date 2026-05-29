#include "SwapchainFramebuffer.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../physicalDevice/PhysicalDevice.h"
#include "../surface/Swapchain.h"

namespace ge {
  SwapchainFramebuffer::SwapchainFramebuffer(std::shared_ptr<LogicalDevice> logicalDevice,
                                             const std::shared_ptr<Swapchain>& swapchain,
                                             const VkCommandPool& commandPool,
                                             const std::shared_ptr<RenderPass>& renderPass,
                                             VkExtent2D extent)
    : Framebuffer(std::move(logicalDevice)), m_swapchain(swapchain)
  {
    initializeFramebuffer(commandPool, renderPass, extent);

    m_swapchain.reset();
  }

  VkFormat SwapchainFramebuffer::getColorFormat()
  {
    return m_swapchain->getImageFormat();
  }

  const std::vector<VkImageView>& SwapchainFramebuffer::getImageViews()
  {
    return m_swapchain->getImageViews();
  }

  VkSampleCountFlagBits SwapchainFramebuffer::getSampleCount()
  {
    return m_logicalDevice->getPhysicalDevice()->getMsaaSamples();
  }
} // ge