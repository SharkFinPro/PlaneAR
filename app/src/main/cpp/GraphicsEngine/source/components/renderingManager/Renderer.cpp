#include "Renderer.h"
#include "../commandBuffer/CommandBuffer.h"
#include "../framebuffers/MousePickingFramebuffer.h"
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
                                                VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)),
      m_mousePickingRenderPass(std::make_shared<RenderPass>(m_logicalDevice,
                                                            VK_FORMAT_R8G8B8A8_UINT,
                                                            VK_SAMPLE_COUNT_1_BIT,
                                                            VK_IMAGE_LAYOUT_UNDEFINED))
  {
    resetSwapchainImageResources(swapchain);

    resetMousePickingImageResources(swapchain->getExtent());
  }

  std::shared_ptr<RenderPass> Renderer::getRenderPass() const
  {
    return m_renderPass;
  }

  std::shared_ptr<RenderPass> Renderer::getMousePickingRenderPass() const
  {
    return m_mousePickingRenderPass;
  }

  void Renderer::resetSwapchainImageResources(const std::shared_ptr<Swapchain>& swapchain)
  {
    m_framebuffer.reset();

    if (!swapchain)
    {
      return;
    }

    m_framebuffer = std::make_shared<SwapchainFramebuffer>(
      m_logicalDevice,
      swapchain,
      m_commandPool,
      m_renderPass,
      swapchain->getExtent()
    );
  }

  void Renderer::resetMousePickingImageResources(VkExtent2D mousePickingExtent)
  {
    m_mousePickingFramebuffer.reset();

    if (mousePickingExtent.width == 0 && mousePickingExtent.height == 0)
    {
      return;
    }

    m_mousePickingFramebuffer = std::make_shared<MousePickingFramebuffer>(
      m_logicalDevice,
      m_commandPool,
      m_mousePickingRenderPass,
      mousePickingExtent
    );
  }

  void Renderer::beginSwapchainRendering(uint32_t imageIndex, VkExtent2D extent,
                                         const std::shared_ptr<CommandBuffer>& commandBuffer,
                                         const std::shared_ptr<Swapchain>& swapchain)
  {
    m_renderPass->begin(m_framebuffer->getFramebuffer(imageIndex), extent, commandBuffer);
  }

  void Renderer::endRendering(const std::shared_ptr<CommandBuffer>& commandBuffer)
  {
    commandBuffer->endRenderPass();
  }

  void Renderer::beginMousePickingRendering(uint32_t imageIndex,
                                            VkExtent2D extent,
                                            const std::shared_ptr<CommandBuffer>& commandBuffer)
  {
    m_mousePickingRenderPass->begin(
      m_mousePickingFramebuffer->getFramebuffer(imageIndex),
      extent,
      commandBuffer
    );
  }

  VkImage Renderer::getMousePickingColorImage() const
  {
    return m_mousePickingFramebuffer->getColorImage();
  }

} // ge