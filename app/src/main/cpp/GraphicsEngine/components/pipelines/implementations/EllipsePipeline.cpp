#include "EllipsePipeline.h"
#include "common/GraphicsPipelineStates.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../renderingManager/renderer2D/Renderer2D.h"

namespace ge {
  EllipsePipeline::EllipsePipeline(std::shared_ptr<LogicalDevice> logicalDevice,
                                   std::shared_ptr<RenderPass> renderPass,
                                   AAssetManager* assetManager)
    : GraphicsPipeline(std::move(logicalDevice))
  {
    const GraphicsPipelineOptions graphicsPipelineOptions {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/ellipse.vert.spv",
        .fragmentShader = "shaders/ellipse.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendStateTransparent,
        .depthStencilState = gps::depthStencilState,
        .dynamicState = gps::dynamicState,
        .inputAssemblyState = gps::inputAssemblyStateTriangleStrip,
        .multisampleState = gps::getMultsampleState(m_logicalDevice),
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = gps::vertexInputStateRaw,
        .viewportState = gps::viewportState
      },
      .pushConstantRanges {
        {
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
          .offset = 0,
          .size = sizeof(EllipsePushConstant)
        }
      },
      .renderPass = renderPass
    };

    createPipeline(graphicsPipelineOptions);
  }

  void EllipsePipeline::render(const RenderInfo* renderInfo,
                               const std::vector<Ellipse>* ellipses)
  {
    bindPipeline(renderInfo);

    for (const auto& ellipse : *ellipses)
    {
      renderEllipse(renderInfo, ellipse);
    }
  }

  void EllipsePipeline::renderEllipse(const RenderInfo* renderInfo,
                                      const Ellipse& ellipse) const
  {
    const EllipsePushConstant ellipsePC {
      .transform = ellipse.transform,
      .screenWidth = static_cast<int>(renderInfo->extent.width),
      .screenHeight = static_cast<int>(renderInfo->extent.height),
      .z = ellipse.z,
      .x = ellipse.bounds.x,
      .y = ellipse.bounds.y,
      .width = ellipse.bounds.z,
      .height = ellipse.bounds.w,
      .r = ellipse.color.r,
      .g = ellipse.color.g,
      .b = ellipse.color.b,
      .a = ellipse.color.a
    };

    renderInfo->commandBuffer->pushConstants(m_pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                                             0, sizeof(EllipsePushConstant), &ellipsePC);

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }
} // ge