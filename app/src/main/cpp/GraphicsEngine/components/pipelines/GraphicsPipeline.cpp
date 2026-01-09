#include "GraphicsPipeline.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../physicalDevice/PhysicalDevice.h"
#include "../renderPass/RenderPass.h"

namespace ge {
  GraphicsPipeline::GraphicsPipeline(std::shared_ptr<LogicalDevice> logicalDevice)
    : Pipeline(std::move(logicalDevice))
  {}

  void GraphicsPipeline::createPipelineLayout(const GraphicsPipelineOptions& graphicsPipelineOptions)
  {
    const VkPipelineLayoutCreateInfo pipelineLayoutInfo {
      .sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO,
      .setLayoutCount = static_cast<uint32_t>(graphicsPipelineOptions.descriptorSetLayouts.size()),
      .pSetLayouts = graphicsPipelineOptions.descriptorSetLayouts.data(),
      .pushConstantRangeCount = static_cast<uint32_t>(graphicsPipelineOptions.pushConstantRanges.size()),
      .pPushConstantRanges = graphicsPipelineOptions.pushConstantRanges.empty() ? nullptr : graphicsPipelineOptions.pushConstantRanges.data()
    };

    m_pipelineLayout = m_logicalDevice->createPipelineLayout(pipelineLayoutInfo);
  }

  void GraphicsPipeline::createPipeline(const GraphicsPipelineOptions& graphicsPipelineOptions)
  {
    createPipelineLayout(graphicsPipelineOptions);

    const auto shaderModules = graphicsPipelineOptions.shaders.getShaderModules(m_logicalDevice);
    const auto shaderStages = graphicsPipelineOptions.shaders.getShaderStages(shaderModules);

    constexpr VkFormat colorFormat = VK_FORMAT_B8G8R8A8_UNORM;

    const VkPipelineRenderingCreateInfo pipelineRenderingCreateInfo = {
      .sType = VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO,
      .colorAttachmentCount = 1,
      .pColorAttachmentFormats = &colorFormat,
      .depthAttachmentFormat = m_logicalDevice->getPhysicalDevice()->findDepthFormat()
    };

    const VkGraphicsPipelineCreateInfo pipelineInfo {
      .sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO,
      .pNext = graphicsPipelineOptions.renderPass ? nullptr : &pipelineRenderingCreateInfo,
      .stageCount = static_cast<uint32_t>(shaderStages.size()),
      .pStages = shaderStages.data(),
      .pVertexInputState = &graphicsPipelineOptions.states.vertexInputState,
      .pInputAssemblyState = &graphicsPipelineOptions.states.inputAssemblyState,
      .pTessellationState = &graphicsPipelineOptions.states.tessellationState,
      .pViewportState = &graphicsPipelineOptions.states.viewportState,
      .pRasterizationState = &graphicsPipelineOptions.states.rasterizationState,
      .pMultisampleState = &graphicsPipelineOptions.states.multisampleState,
      .pDepthStencilState = &graphicsPipelineOptions.states.depthStencilState,
      .pColorBlendState = &graphicsPipelineOptions.states.colorBlendState,
      .pDynamicState = &graphicsPipelineOptions.states.dynamicState,
      .layout = m_pipelineLayout,
      .renderPass = graphicsPipelineOptions.renderPass ? graphicsPipelineOptions.renderPass->getRenderPass() : VK_NULL_HANDLE,
      .subpass = 0,
      .basePipelineHandle = VK_NULL_HANDLE,
      .basePipelineIndex = -1
    };

    m_pipeline = m_logicalDevice->createPipeline(pipelineInfo);
  }
} // ge