#include "QuadPipeline.h"
#include "common/GraphicsPipelineStates.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../surface/Surface.h"
#include <glm/gtc/matrix_transform.hpp>
#include <utility>

namespace ge {
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
          .size = sizeof(QuadPushConstant)
        }
      },
      .renderPass = renderPass
    };

    createPipeline(graphicsPipelineOptions);
  }

  void QuadPipeline::render(const std::shared_ptr<CommandBuffer>& commandBuffer)
  {
    commandBuffer->bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, m_pipeline);

    for (const auto& rect : m_rectsToRender)
    {
      renderRect(commandBuffer, rect);
    }
  }

  void QuadPipeline::renderRect(const std::shared_ptr<CommandBuffer>& commandBuffer,
                                Rect rect)
  {
    QuadPushConstant quadPC {
      .transformation = rect.transformation,
      .screenWidth = m_surface->getWidth(),
      .screenHeight = m_surface->getHeight(),
      .x = rect.x,
      .y = rect.y,
      .width = rect.width,
      .height = rect.height,
      .r = rect.r,
      .g = rect.g,
      .b = rect.b
    };

    commandBuffer->pushConstants(m_pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                                 0, sizeof(QuadPushConstant), &quadPC);

    commandBuffer->draw(4, 1, 0, 0);
  }

  void QuadPipeline::queueRectToRender(float x,
                                       float y,
                                       float width,
                                       float height,
                                       float r,
                                       float g,
                                       float b,
                                       glm::mat4 transformation)
  {
    m_rectsToRender.push_back({
      x,
      y,
      width,
      height,
      r,
      g,
      b,
      transformation
    });
  }

  void QuadPipeline::createNewFrame()
  {
    m_rectsToRender.clear();
  }
} // ge