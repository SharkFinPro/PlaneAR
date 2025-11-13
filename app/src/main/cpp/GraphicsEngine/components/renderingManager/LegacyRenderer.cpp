#include "LegacyRenderer.h"
#include "../commandBuffer/CommandBuffer.h"
#include "../framebuffers/SwapchainFramebuffer.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../physicalDevice/PhysicalDevice.h"
#include "../surface/Swapchain.h"
#include "../renderPass/RenderPass.h"

namespace ge {
  LegacyRenderer::LegacyRenderer(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                 const std::shared_ptr<Swapchain>& swapchain,
                                 VkCommandPool commandPool)
    : Renderer(logicalDevice, commandPool),
      m_renderPass(std::make_shared<RenderPass>(m_logicalDevice,
                                                swapchain->getImageFormat(),
                                                m_logicalDevice->getPhysicalDevice()->getMsaaSamples(),
                                                VK_IMAGE_LAYOUT_PRESENT_SRC_KHR))
  {
    resetSwapchainImageResources(swapchain);
  }

  std::shared_ptr<RenderPass> LegacyRenderer::getRenderPass() const
  {
    return m_renderPass;
  }

  void LegacyRenderer::resetSwapchainImageResources(std::shared_ptr<Swapchain> swapchain)
  {
    m_framebuffer.reset();
    m_framebuffer = std::make_shared<SwapchainFramebuffer>(
      m_logicalDevice,
      swapchain,
      m_commandPool,
      m_renderPass,
      swapchain->getExtent()
    );
  }

  void LegacyRenderer::beginSwapchainRendering(uint32_t imageIndex, VkExtent2D extent,
                                               std::shared_ptr<CommandBuffer> commandBuffer,
                                               std::shared_ptr<Swapchain> swapchain)
  {
    m_renderPass->begin(m_framebuffer->getFramebuffer(imageIndex), extent, commandBuffer);
  }

  void LegacyRenderer::endSwapchainRendering(uint32_t imageIndex,
                                             std::shared_ptr<CommandBuffer> commandBuffer,
                                             std::shared_ptr<Swapchain> swapchain)
  {
    endRendering(commandBuffer);
  }

  void LegacyRenderer::endRendering(const std::shared_ptr<CommandBuffer>& commandBuffer)
  {
    commandBuffer->endRenderPass();
  }
} // ge