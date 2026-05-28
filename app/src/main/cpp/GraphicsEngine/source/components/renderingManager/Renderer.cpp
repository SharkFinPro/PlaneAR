#include "Renderer.h"
#include "../commandBuffer/CommandBuffer.h"
#include "../framebuffers/SwapchainFramebuffer.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../physicalDevice/PhysicalDevice.h"
#include "../surface/Swapchain.h"
#include "../renderPass/RenderPass.h"

namespace ge {

  Renderer::Renderer(std::shared_ptr<LogicalDevice> logicalDevice,
                     const std::shared_ptr<Swapchain>& swapchain,
                     VkCommandPool commandPool)
    : m_logicalDevice(std::move(logicalDevice)),
      m_commandPool(commandPool),
      m_renderPass(std::make_shared<RenderPass>(m_logicalDevice,
                                                swapchain->getImageFormat(),
                                                m_logicalDevice->getPhysicalDevice()->getMsaaSamples(),
                                                VK_IMAGE_LAYOUT_PRESENT_SRC_KHR))
  {
    resetSwapchainImageResources(swapchain);
  }

  std::shared_ptr<RenderPass> Renderer::getRenderPass() const
  {
    return m_renderPass;
  }

  void Renderer::resetSwapchainImageResources(const std::shared_ptr<Swapchain>& swapchain)
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

  void Renderer::beginSwapchainRendering(uint32_t imageIndex, VkExtent2D extent,
                                         const std::shared_ptr<CommandBuffer>& commandBuffer,
                                         const std::shared_ptr<Swapchain>& swapchain)
  {
    m_renderPass->begin(m_framebuffer->getFramebuffer(imageIndex), extent, commandBuffer);
  }

  void Renderer::endSwapchainRendering(uint32_t imageIndex,
                                       const std::shared_ptr<CommandBuffer>& commandBuffer,
                                       const std::shared_ptr<Swapchain>& swapchain)
  {
    endRendering(commandBuffer);
  }

  void Renderer::endRendering(const std::shared_ptr<CommandBuffer> &commandBuffer)
  {
    commandBuffer->endRenderPass();
  }

} // ge