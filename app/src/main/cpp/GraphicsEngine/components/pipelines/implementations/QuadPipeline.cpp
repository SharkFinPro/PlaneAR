#include "QuadPipeline.h"
#include "common/GraphicsPipelineStates.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../surface/Surface.h"
#include <utility>

namespace ge {

  struct QuadPC {
    int screenWidth;
    int screenHeight;
    float x;
    float y;
    float width;
    float height;
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

    for (const auto& [x, y, width, height, r, g, b] : m_rectsToRender)
    {
      renderRect(commandBuffer, x, y, width, height, r, g, b);
    }
  }

  void QuadPipeline::renderRect(const std::shared_ptr<CommandBuffer>& commandBuffer,
                                float x,
                                float y,
                                float width,
                                float height,
                                float r,
                                float g,
                                float b)
  {
    QuadPC quadPC {
      .screenWidth = m_surface->getWidth(),
      .screenHeight = m_surface->getHeight(),
      .x = x,
      .y = y,
      .width = width,
      .height = height,
      .r = r,
      .g = g,
      .b = b
    };

    commandBuffer->pushConstants(m_pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                                 0, sizeof(QuadPC), &quadPC);

    commandBuffer->draw(4, 1, 0, 0);
  }

  void QuadPipeline::queueRectToRender(float x,
                                       float y,
                                       float width,
                                       float height,
                                       float r,
                                       float g,
                                       float b)
  {
    m_rectsToRender.push_back({x, y, width, height, r, g, b});
  }

  void QuadPipeline::createNewFrame()
  {
    m_rectsToRender.clear();
  }
} // ge