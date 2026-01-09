#include "TrianglePipeline.h"
#include "common/GraphicsPipelineStates.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../renderingManager/renderer2D/Renderer2D.h"

namespace ge {
  TrianglePipeline::TrianglePipeline(std::shared_ptr<LogicalDevice> logicalDevice,
                                     std::shared_ptr<RenderPass> renderPass)
    : GraphicsPipeline(std::move(logicalDevice))
  {
    const GraphicsPipelineOptions graphicsPipelineOptions {
      .shaders {
        .vertexShader = "shaders/triangle.vert.spv",
        .fragmentShader = "shaders/triangle.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendStateTransparent,
        .depthStencilState = gps::depthStencilStateNone,
        .dynamicState = gps::dynamicState,
        .inputAssemblyState = gps::inputAssemblyStateTriangleList,
        .multisampleState = gps::getMultsampleState(m_logicalDevice),
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = gps::vertexInputStateRaw,
        .viewportState = gps::viewportState
      },
      .pushConstantRanges {
        {
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
          .offset = 0,
          .size = sizeof(TrianglePushConstant)
        }
      },
      .renderPass = renderPass
    };

    createPipeline(graphicsPipelineOptions);
  }

  void TrianglePipeline::render(const RenderInfo* renderInfo,
                                const std::vector<Triangle>* triangles)
  {
    renderInfo->commandBuffer->bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, m_pipeline);

    for (const auto& triangle : *triangles)
    {
      renderTriangle(renderInfo, triangle);
    }
  }

  void TrianglePipeline::renderTriangle(const RenderInfo* renderInfo,
                                        const Triangle& triangle) const
  {
    const TrianglePushConstant trianglePC {
      .r = triangle.color.r,
      .g = triangle.color.g,
      .b = triangle.color.b,
      .a = triangle.color.a,
      .transform = triangle.transform,
      .screenWidth = static_cast<int>(renderInfo->extent.width),
      .screenHeight = static_cast<int>(renderInfo->extent.height),
      .z = triangle.z,
      .x1 = triangle.p1.x,
      .y1 = triangle.p1.y,
      .x2 = triangle.p2.x,
      .y2 = triangle.p2.y,
      .x3 = triangle.p3.x,
      .y3 = triangle.p3.y
    };

    renderInfo->commandBuffer->pushConstants(m_pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                                             0, sizeof(TrianglePushConstant), &trianglePC);

    renderInfo->commandBuffer->draw(3, 1, 0, 0);
  }
} // ge