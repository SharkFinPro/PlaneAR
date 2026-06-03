#ifndef PLANEAR_PIPELINECONFIG_H
#define PLANEAR_PIPELINECONFIG_H

#include "GraphicsPipeline.h"
#include "GraphicsPipelineStates.h"
#include "../renderingManager/renderer2D/Primitives2D.h"

namespace ge::PipelineConfig {

  inline GraphicsPipelineOptions createRectPipelineOptions(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                                           const std::shared_ptr<RenderPass>& renderPass,
                                                           AAssetManager* assetManager)
  {
    return {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/rect.vert.spv",
        .fragmentShader = "shaders/rect.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendStateTransparent,
        .depthStencilState = gps::depthStencilState,
        .dynamicState = gps::dynamicState,
        .inputAssemblyState = gps::inputAssemblyStateTriangleStrip,
        .multisampleState = gps::getMultsampleState(logicalDevice),
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = gps::vertexInputStateRaw,
        .viewportState = gps::viewportState
      },
      .pushConstantRanges {
        {
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
          .offset = 0,
          .size = sizeof(Rect::PushConstant)
        }
      },
      .renderPass = renderPass
    };
  }

  inline GraphicsPipelineOptions createTrianglePipelineOptions(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                                               const std::shared_ptr<RenderPass>& renderPass,
                                                               AAssetManager* assetManager)
  {
    return {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/triangle.vert.spv",
        .fragmentShader = "shaders/triangle.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendStateTransparent,
        .depthStencilState = gps::depthStencilState,
        .dynamicState = gps::dynamicState,
        .inputAssemblyState = gps::inputAssemblyStateTriangleList,
        .multisampleState = gps::getMultsampleState(logicalDevice),
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = gps::vertexInputStateRaw,
        .viewportState = gps::viewportState
      },
      .pushConstantRanges {
        {
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
          .offset = 0,
          .size = sizeof(Triangle::PushConstant)
        }
      },
      .renderPass = renderPass
    };
  }

  inline GraphicsPipelineOptions createEllipsePipelineOptions(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                                              const std::shared_ptr<RenderPass>& renderPass,
                                                              AAssetManager* assetManager)
  {
    return {
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
        .multisampleState = gps::getMultsampleState(logicalDevice),
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = gps::vertexInputStateRaw,
        .viewportState = gps::viewportState
      },
      .pushConstantRanges {
        {
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
          .offset = 0,
          .size = sizeof(Ellipse::PushConstant)
        }
      },
      .renderPass = renderPass
    };
  }

  inline GraphicsPipelineOptions createFontPipelineOptions(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                                           const std::shared_ptr<RenderPass>& renderPass,
                                                           AAssetManager* assetManager,
                                                           VkDescriptorSetLayout fontDescriptorSetLayout)
  {
    return {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/font.vert.spv",
        .fragmentShader = "shaders/font.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendStateTransparent,
        .depthStencilState = gps::depthStencilState,
        .dynamicState = gps::dynamicState,
        .inputAssemblyState = gps::inputAssemblyStateTriangleStrip,
        .multisampleState = gps::getMultsampleState(logicalDevice),
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = gps::vertexInputStateRaw,
        .viewportState = gps::viewportState
      },
      .pushConstantRanges {
        {
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
          .offset = 0,
          .size = sizeof(Glyph::PushConstant)
        }
      },
      .descriptorSetLayouts {
        fontDescriptorSetLayout
      },
      .renderPass = renderPass
    };
  }

  inline GraphicsPipelineOptions createImagePipelineOptions(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                                            const std::shared_ptr<RenderPass>& renderPass,
                                                            AAssetManager* assetManager,
                                                            VkDescriptorSetLayout imageDescriptorSetLayout)
  {
    return {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/image.vert.spv",
        .fragmentShader = "shaders/image.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendStateTransparent,
        .depthStencilState = gps::depthStencilState,
        .dynamicState = gps::dynamicState,
        .inputAssemblyState = gps::inputAssemblyStateTriangleStrip,
        .multisampleState = gps::getMultsampleState(logicalDevice),
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = gps::vertexInputStateRaw,
        .viewportState = gps::viewportState
      },
      .pushConstantRanges {
        {
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
          .offset = 0,
          .size = sizeof(Image::PushConstant)
        }
      },
      .descriptorSetLayouts {
        imageDescriptorSetLayout
      },
      .renderPass = renderPass
    };
  }

  inline GraphicsPipelineOptions createPointPipelineOptions(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                                            const std::shared_ptr<RenderPass>& renderPass,
                                                            AAssetManager* assetManager,
                                                            VkDescriptorSetLayout glyph3DDescriptorSetLayout)
  {
    static std::vector<VkVertexInputBindingDescription> sBindings {{
      .binding = 0,
      .stride = sizeof(PointInstance),
      .inputRate = VK_VERTEX_INPUT_RATE_INSTANCE
    }};

    static std::vector<VkVertexInputAttributeDescription> sAttributes {
      { .location = 0, .binding = 0, .format = VK_FORMAT_R32G32B32_SFLOAT,    .offset = offsetof(PointInstance, worldPos) },
      { .location = 1, .binding = 0, .format = VK_FORMAT_R32_SFLOAT,          .offset = offsetof(PointInstance, size)     },
      { .location = 2, .binding = 0, .format = VK_FORMAT_R32G32B32A32_SFLOAT, .offset = offsetof(PointInstance, color)    }
    };

    static const VkPipelineVertexInputStateCreateInfo sVertexInputState {
      .sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO,
      .vertexBindingDescriptionCount = static_cast<uint32_t>(sBindings.size()),
      .pVertexBindingDescriptions = sBindings.data(),
      .vertexAttributeDescriptionCount = static_cast<uint32_t>(sAttributes.size()),
      .pVertexAttributeDescriptions = sAttributes.data()
    };

    return {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/point.vert.spv",
        .fragmentShader = "shaders/point.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendStateTransparent,
        .depthStencilState = gps::depthStencilState,
        .dynamicState = gps::dynamicState,
        .inputAssemblyState = gps::inputAssemblyStateTriangleStrip,
        .multisampleState = gps::getMultsampleState(logicalDevice),
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = sVertexInputState,
        .viewportState = gps::viewportState
      },
      .descriptorSetLayouts {
        glyph3DDescriptorSetLayout
      },
      .renderPass = renderPass
    };
  }

  inline GraphicsPipelineOptions createFont3DPipelineOptions(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                                             const std::shared_ptr<RenderPass>& renderPass,
                                                             AAssetManager* assetManager,
                                                             VkDescriptorSetLayout fontDescriptorSetLayout,
                                                             VkDescriptorSetLayout glyph3DDescriptorSetLayout)
  {
    static std::vector<VkVertexInputBindingDescription> sBindings {{
      .binding = 0,
      .stride = sizeof(Glyph3DInstance),
      .inputRate = VK_VERTEX_INPUT_RATE_INSTANCE
    }};

    static std::vector<VkVertexInputAttributeDescription> sAttributes {
      { .location = 0,  .binding = 0, .format = VK_FORMAT_R32G32B32_SFLOAT,    .offset = offsetof(Glyph3DInstance, worldPos)     },
      { .location = 1,  .binding = 0, .format = VK_FORMAT_R32_SFLOAT,          .offset = offsetof(Glyph3DInstance, width)        },
      { .location = 2,  .binding = 0, .format = VK_FORMAT_R32_SFLOAT,          .offset = offsetof(Glyph3DInstance, glyphOffsetX) },
      { .location = 3,  .binding = 0, .format = VK_FORMAT_R32_SFLOAT,          .offset = offsetof(Glyph3DInstance, glyphOffsetY) },
      { .location = 4,  .binding = 0, .format = VK_FORMAT_R32_SFLOAT,          .offset = offsetof(Glyph3DInstance, height)       },
      { .location = 5,  .binding = 0, .format = VK_FORMAT_R32_SFLOAT,          .offset = offsetof(Glyph3DInstance, _pad)         },
      { .location = 6,  .binding = 0, .format = VK_FORMAT_R32_SFLOAT,          .offset = offsetof(Glyph3DInstance, uv) + 0       },
      { .location = 7,  .binding = 0, .format = VK_FORMAT_R32_SFLOAT,          .offset = offsetof(Glyph3DInstance, uv) + 4       },
      { .location = 8,  .binding = 0, .format = VK_FORMAT_R32_SFLOAT,          .offset = offsetof(Glyph3DInstance, uv) + 8       },
      { .location = 9,  .binding = 0, .format = VK_FORMAT_R32_SFLOAT,          .offset = offsetof(Glyph3DInstance, uv) + 12      },
      { .location = 10, .binding = 0, .format = VK_FORMAT_R32G32B32A32_SFLOAT, .offset = offsetof(Glyph3DInstance, color)        }
    };

    static const VkPipelineVertexInputStateCreateInfo sVertexInputState {
      .sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO,
      .vertexBindingDescriptionCount = static_cast<uint32_t>(sBindings.size()),
      .pVertexBindingDescriptions = sBindings.data(),
      .vertexAttributeDescriptionCount = static_cast<uint32_t>(sAttributes.size()),
      .pVertexAttributeDescriptions = sAttributes.data()
    };

    return {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/font3D.vert.spv",
        .fragmentShader = "shaders/font3D.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendStateTransparent,
        .depthStencilState = gps::depthStencilState,
        .dynamicState = gps::dynamicState,
        .inputAssemblyState = gps::inputAssemblyStateTriangleStrip,
        .multisampleState = gps::getMultsampleState(logicalDevice),
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = sVertexInputState,
        .viewportState = gps::viewportState
      },
      .descriptorSetLayouts {
        fontDescriptorSetLayout,
        glyph3DDescriptorSetLayout
      },
      .renderPass = renderPass
    };
  }

  inline GraphicsPipelineOptions createCameraPipelineOptions(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                                             const std::shared_ptr<RenderPass>& renderPass,
                                                             AAssetManager* assetManager,
                                                             VkDescriptorSetLayout imageDescriptorSetLayout)
  {
    auto cameraPipelineOptions = createImagePipelineOptions(logicalDevice, renderPass, assetManager, imageDescriptorSetLayout);

    cameraPipelineOptions.shaders.fragmentShader = "shaders/camera.frag.spv";

    return cameraPipelineOptions;
  }

  inline GraphicsPipelineOptions createMousePickingPipelineOptions(const std::shared_ptr<RenderPass>& renderPass,
                                                                   AAssetManager* assetManager)
  {
    return {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/mousePicking.vert.spv",
        .fragmentShader = "shaders/mousePicking.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendState,
        .depthStencilState = gps::depthStencilState,
        .dynamicState = gps::dynamicState,
        .inputAssemblyState = gps::inputAssemblyStateTriangleStrip,
        .multisampleState = gps::multisampleStateNone,
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = gps::vertexInputStateRaw,
        .viewportState = gps::viewportState
      },
      .pushConstantRanges {
        {
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
          .offset = 0,
          .size = sizeof(MousePickingPoint::PushConstant)
        }
      },
      .renderPass = renderPass,
      .colorFormat = VK_FORMAT_R8G8B8A8_UINT
    };
  }

  // ── Compass ──────────────────────────────────────────────────────────────────
  // Shares the same pipeline state configuration as the point pipeline
  // (billboard triangle-strip, transparent blending, depth test) but uses its
  // own shaders so the fragment stage can draw the compass face procedurally.
  inline GraphicsPipelineOptions createCompassPipelineOptions(const std::shared_ptr<LogicalDevice>& logicalDevice,
                                                              const std::shared_ptr<RenderPass>& renderPass,
                                                              AAssetManager* assetManager)
  {
    return {
      .shaders {
        .assetManager = assetManager,
        .vertexShader = "shaders/compass.vert.spv",
        .fragmentShader = "shaders/compass.frag.spv"
      },
      .states {
        .colorBlendState = gps::colorBlendStateTransparent,
        .depthStencilState = gps::depthStencilState,
        .dynamicState = gps::dynamicState,
        .inputAssemblyState = gps::inputAssemblyStateTriangleStrip,
        .multisampleState = gps::getMultsampleState(logicalDevice),
        .rasterizationState = gps::rasterizationStateNoCull,
        .vertexInputState = gps::vertexInputStateRaw,
        .viewportState = gps::viewportState
      },
      .pushConstantRanges {
        {
          .stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
          .offset = 0,
          .size = sizeof(Compass::PushConstant)
        }
      },
      .renderPass = renderPass
    };
  }

}

#endif //PLANEAR_PIPELINECONFIG_H