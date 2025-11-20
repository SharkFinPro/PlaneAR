#ifndef PLANEAR_GRAPHICSPIPELINE_H
#define PLANEAR_GRAPHICSPIPELINE_H

#include "Pipeline.h"
#include "../shaderModule/ShaderModule.h"
#include <string>
#include <vector>

namespace ge {

  class RenderPass;

  struct GraphicsPipelineOptions {
    struct {
      AAssetManager* assetManager;

      std::string vertexShader;
      std::string geometryShader;
      std::string tesselationControlShader;
      std::string tesselationEvaluationShader;
      std::string fragmentShader;

      [[nodiscard]] std::vector<ShaderModule> getShaderModules(const std::shared_ptr<LogicalDevice>& logicalDevice) const
      {
        std::vector<ShaderModule> shaderModules;
        if (!vertexShader.empty())
        {
          shaderModules.emplace_back(logicalDevice, assetManager, vertexShader.c_str(), VK_SHADER_STAGE_VERTEX_BIT);
        }

        if (!fragmentShader.empty())
        {
          shaderModules.emplace_back(logicalDevice, assetManager, fragmentShader.c_str(), VK_SHADER_STAGE_FRAGMENT_BIT);
        }

        if (!geometryShader.empty())
        {
          shaderModules.emplace_back(logicalDevice, assetManager, geometryShader.c_str(), VK_SHADER_STAGE_GEOMETRY_BIT);
        }

        if (!tesselationControlShader.empty())
        {
          shaderModules.emplace_back(logicalDevice, assetManager, tesselationControlShader.c_str(), VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT);
        }

        if (!tesselationEvaluationShader.empty())
        {
          shaderModules.emplace_back(logicalDevice, assetManager, tesselationEvaluationShader.c_str(), VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT);
        }

        return std::move(shaderModules);
      }

      static std::vector<VkPipelineShaderStageCreateInfo> getShaderStages(const std::vector<ShaderModule>& shaderModules)
      {
        std::vector<VkPipelineShaderStageCreateInfo> pipelineShaderStageCreateInfos;

        for (const auto& shaderModule : shaderModules)
        {
          pipelineShaderStageCreateInfos.push_back(shaderModule.getShaderStageCreateInfo());
        }

        return std::move(pipelineShaderStageCreateInfos);
      }
    } shaders;

    struct {
      VkPipelineColorBlendStateCreateInfo colorBlendState{};
      VkPipelineDepthStencilStateCreateInfo depthStencilState{};
      VkPipelineDynamicStateCreateInfo dynamicState{};
      VkPipelineInputAssemblyStateCreateInfo inputAssemblyState{};
      VkPipelineMultisampleStateCreateInfo multisampleState{};
      VkPipelineRasterizationStateCreateInfo rasterizationState{};
      VkPipelineTessellationStateCreateInfo tessellationState{};
      VkPipelineVertexInputStateCreateInfo vertexInputState{};
      VkPipelineViewportStateCreateInfo viewportState{};
    } states;

    std::vector<VkPushConstantRange> pushConstantRanges;

    std::vector<VkDescriptorSetLayout> descriptorSetLayouts;

    std::shared_ptr<RenderPass>& renderPass;
  };

  class GraphicsPipeline : public Pipeline
  {
  public:
    explicit GraphicsPipeline(const std::shared_ptr<LogicalDevice>& logicalDevice);

//  protected:
    void createPipelineLayout(const GraphicsPipelineOptions& graphicsPipelineOptions);

    void createPipeline(const GraphicsPipelineOptions& graphicsPipelineOptions);
  };

} // ge

#endif //PLANEAR_GRAPHICSPIPELINE_H
