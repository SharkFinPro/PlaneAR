#include "QuadPipeline.h"
#include "common/GraphicsPipelineStates.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../surface/Surface.h"
#include <utility>

namespace ge {

  struct QuadPC {
    int screenWidth;
    int screenHeight;
    float x1;
    float y1;
    float x2;
    float y2;
    float x3;
    float y3;
    float r;
    float g;
    float b;
  };

  QuadPipeline::QuadPipeline(const std::shared_ptr<LogicalDevice>& logicalDevice,
                             std::shared_ptr<RenderPass> renderPass,
                             AAssetManager* assetManager,
                             std::shared_ptr<Surface> surface)
    : GraphicsPipeline(logicalDevice), m_surface(std::move(surface))
  {
    const GraphicsPipelineOptions graphicsPipelineOptions {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/ui.vert.spv",
        .fragmentShader = "shaders/ui.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendState,
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
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT,
          .offset = 0,
          .size = sizeof(QuadPC)
        }
      },
      .renderPass = renderPass
    };

    createPipeline(graphicsPipelineOptions);
  }

  void QuadPipeline::render(const std::shared_ptr<CommandBuffer>& commandBuffer)
  {
    commandBuffer->bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, m_pipeline);

    static float x = 100;
    static float y = 100;
    static float w = 200;
    static float h = 100;

    QuadPC quadPC {
      .screenWidth = m_surface->getWidth(),
      .screenHeight = m_surface->getHeight(),
      .x1 = x,
      .y1 = y,
      .x2 = x + w,
      .y2 = y,
      .x3 = x + w,
      .y3 = y + h,
      .r = 0,
      .g = 0,
      .b = 1
    };

    commandBuffer->pushConstants(m_pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
                                 0, sizeof(QuadPC), &quadPC);

    commandBuffer->draw(3, 1, 0, 0);

    quadPC = {
      .screenWidth = m_surface->getWidth(),
      .screenHeight = m_surface->getHeight(),
      .x1 = x,
      .y1 = y,
      .x2 = x,
      .y2 = y + h,
      .x3 = x + w,
      .y3 = y + h,
      .r = 1,
      .g = 0,
      .b = 0
    };

    commandBuffer->pushConstants(m_pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
                                 0, sizeof(QuadPC), &quadPC);

    commandBuffer->draw(3, 1, 0, 0);
  }
} // ge