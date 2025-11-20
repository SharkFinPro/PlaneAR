#include "RenderingManager.h"
#include "LegacyRenderer.h"
#include "../commandBuffer/CommandBuffer.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../surface/Swapchain.h"

#include "../pipelines/GraphicsPipeline.h"
#include "../physicalDevice/PhysicalDevice.h"

namespace ge {

  inline VkPipelineColorBlendAttachmentState colorBlendAttachment {
    .blendEnable = VK_FALSE,
    .colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                      VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT
  };

  inline VkPipelineColorBlendStateCreateInfo colorBlendState {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO,
    .logicOpEnable = VK_FALSE,
    .logicOp = VK_LOGIC_OP_COPY,
    .attachmentCount = 1,
    .pAttachments = &colorBlendAttachment,
    .blendConstants = {0.0f, 0.0f, 0.0f, 0.0f}
  };

  inline VkPipelineDepthStencilStateCreateInfo depthStencilStateNone {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO,
    .depthTestEnable = VK_FALSE,
    .depthWriteEnable = VK_FALSE
  };

  inline std::array dynamicStates {
    VK_DYNAMIC_STATE_VIEWPORT,
    VK_DYNAMIC_STATE_SCISSOR
  };

  inline VkPipelineDynamicStateCreateInfo dynamicState {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO,
    .dynamicStateCount = static_cast<uint32_t>(dynamicStates.size()),
    .pDynamicStates = dynamicStates.data()
  };

  inline VkPipelineInputAssemblyStateCreateInfo inputAssemblyStateTriangleList {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO,
    .topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
    .primitiveRestartEnable = VK_FALSE
  };

  inline VkPipelineMultisampleStateCreateInfo getMultsampleState(const std::shared_ptr<LogicalDevice>& logicalDevice)
  {
    return {
      .sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO,
      .rasterizationSamples = logicalDevice->getPhysicalDevice()->getMsaaSamples(),
      .sampleShadingEnable = VK_FALSE,
      .minSampleShading = 1.0f,
      .pSampleMask = nullptr,
      .alphaToCoverageEnable = VK_FALSE,
      .alphaToOneEnable = VK_FALSE
    };
  }

  inline VkPipelineRasterizationStateCreateInfo rasterizationStateNoCull {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO,
    .depthClampEnable = VK_FALSE,
    .rasterizerDiscardEnable = VK_FALSE,
    .polygonMode = VK_POLYGON_MODE_FILL,
    .cullMode = VK_CULL_MODE_NONE,
    .frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE,
    .depthBiasEnable = VK_FALSE,
    .lineWidth = 1.0f
  };

  inline VkPipelineVertexInputStateCreateInfo vertexInputStateRaw {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO,
    .vertexBindingDescriptionCount = 0,
    .pVertexBindingDescriptions = nullptr,
    .vertexAttributeDescriptionCount = 0,
    .pVertexAttributeDescriptions = nullptr
  };

  inline VkPipelineViewportStateCreateInfo viewportState {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO,
    .viewportCount = 1,
    .scissorCount = 1
  };

  RenderingManager::RenderingManager(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                     const std::shared_ptr<Surface>& surface,
                                     VkCommandPool commandPool,
                                     AAssetManager* assetManager)
    : m_logicalDevice(logicalDevice), m_surface(surface), m_commandPool(commandPool)
  {
    m_swapchain = std::make_shared<Swapchain>(m_logicalDevice, m_surface);
    m_logicalDevice->createSyncObjects(m_swapchain);

    m_swapchainCommandBuffer = std::make_shared<CommandBuffer>(m_logicalDevice, m_commandPool);

    m_renderer = std::make_shared<LegacyRenderer>(m_logicalDevice, m_swapchain, m_commandPool);

    auto renderPass = m_renderer->getRenderPass();

    const GraphicsPipelineOptions graphicsPipelineOptions {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/ui.vert.spv",
        .fragmentShader = "shaders/ui.frag.spv"
      },
      .states {
        .colorBlendState = colorBlendState,
        .depthStencilState = depthStencilStateNone,
        .dynamicState = dynamicState,
        .inputAssemblyState = inputAssemblyStateTriangleList,
        .multisampleState = getMultsampleState(m_logicalDevice),
        .rasterizationState = rasterizationStateNoCull,
        .vertexInputState = vertexInputStateRaw,
        .viewportState = viewportState
      },
      .renderPass = renderPass
    };

    m_graphicsPipeline = std::make_shared<GraphicsPipeline>(m_logicalDevice);

    m_graphicsPipeline->createPipeline(graphicsPipelineOptions);
  }

  void RenderingManager::doRendering(uint32_t currentFrame)
  {
    m_logicalDevice->waitForGraphicsFences(currentFrame);

    uint32_t imageIndex;
    auto result = m_logicalDevice->acquireNextImage(currentFrame, m_swapchain, &imageIndex);

    if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR)
    {
      throw std::runtime_error("failed to acquire swap chain image!");
    }

    m_logicalDevice->resetGraphicsFences(currentFrame);

    m_swapchainCommandBuffer->setCurrentFrame(currentFrame);
    m_swapchainCommandBuffer->resetCommandBuffer();
    recordSwapchainCommandBuffer(imageIndex);
    m_logicalDevice->submitGraphicsQueue(currentFrame, imageIndex, m_swapchainCommandBuffer);

    result = m_logicalDevice->queuePresent(imageIndex, m_swapchain);

    if (result != VK_SUCCESS)
    {
      throw std::runtime_error("failed to present swap chain image!");
    }
  }

  void RenderingManager::recordSwapchainCommandBuffer(uint32_t imageIndex) const
  {
    m_swapchainCommandBuffer->record([this, imageIndex]()
    {
      const auto extent = m_swapchain->getExtent();
      const auto commandBuffer = m_swapchainCommandBuffer;

      m_renderer->beginSwapchainRendering(imageIndex, extent, commandBuffer, m_swapchain);

      const VkViewport viewport = {
        .x = 0.0f,
        .y = 0.0f,
        .width = static_cast<float>(extent.width),
        .height = static_cast<float>(extent.height),
        .minDepth = 0.0f,
        .maxDepth = 1.0f
      };
      commandBuffer->setViewport(viewport);

      const VkRect2D scissor = {
        .offset = {0, 0},
        .extent = extent
      };
      commandBuffer->setScissor(scissor);

      // render graphics pipeline

      commandBuffer->bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, m_graphicsPipeline->m_pipeline);

      commandBuffer->draw(3, 1, 0, 0);

      m_renderer->endSwapchainRendering(imageIndex, commandBuffer, m_swapchain);
    });
  }
} // ge