#include "QuadPipeline.h"
#include "common/GraphicsPipelineStates.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../renderingManager/renderer2D/Renderer2D.h"
#include "../../surface/Surface.h"
#include <glm/gtc/matrix_transform.hpp>
#include <utility>

namespace ge {
  QuadPipeline::QuadPipeline(const std::shared_ptr<LogicalDevice>& logicalDevice,
                             std::shared_ptr<RenderPass> renderPass,
                             AAssetManager* assetManager)
    : GraphicsPipeline(logicalDevice)
  {
    const GraphicsPipelineOptions graphicsPipelineOptions {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/ui.vert.spv",
        .fragmentShader = "shaders/ui.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendStateTransparent,
        .depthStencilState = gps::depthStencilStateNone,
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
          .size = sizeof(QuadPushConstant)
        }
      },
      .renderPass = renderPass
    };

    createPipeline(graphicsPipelineOptions);
  }

  void QuadPipeline::render(const RenderInfo* renderInfo,
                            const std::vector<Rect>* rects)
  {
    renderInfo->commandBuffer->bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, m_pipeline);

    for (const auto& rect : *rects)
    {
      renderRect(renderInfo, rect);
    }
  }

  void QuadPipeline::renderRect(const RenderInfo* renderInfo,
                                Rect rect)
  {
    QuadPushConstant quadPC {
      .transform = rect.transform,
      .screenWidth = static_cast<int>(renderInfo->extent.width),
      .screenHeight = static_cast<int>(renderInfo->extent.height),
      .z = rect.z,
      .x = rect.bounds.x,
      .y = rect.bounds.y,
      .width = rect.bounds.z,
      .height = rect.bounds.w,
      .r = rect.color.r,
      .g = rect.color.g,
      .b = rect.color.b,
      .a = rect.color.a
    };

    renderInfo->commandBuffer->pushConstants(m_pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                                             0, sizeof(QuadPushConstant), &quadPC);

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }
} // ge