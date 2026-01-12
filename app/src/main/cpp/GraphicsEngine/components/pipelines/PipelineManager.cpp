#include "PipelineManager.h"
#include "GraphicsPipeline.h"

namespace ge {
  PipelineManager::PipelineManager(std::shared_ptr<LogicalDevice> logicalDevice,
                                   const std::shared_ptr<Renderer>& renderer,
                                   const std::shared_ptr<AssetManager>& assetManager)
  {

  }

  PipelineManager::~PipelineManager()
  {

  }

  void PipelineManager::bindGraphicsPipeline(const std::shared_ptr<CommandBuffer>& commandBuffer,
                                             const PipelineType pipelineType) const
  {
    const auto& graphicsPipeline = getGraphicsPipeline(pipelineType);

    graphicsPipeline.bind(commandBuffer);
  }

  void PipelineManager::pushGraphicsPipelineConstants(const std::shared_ptr<CommandBuffer>& commandBuffer,
                                                      const PipelineType pipelineType,
                                                      const VkShaderStageFlags stageFlags,
                                                      const uint32_t offset,
                                                      const uint32_t size,
                                                      const void* values) const
  {
    const auto& graphicsPipeline = getGraphicsPipeline(pipelineType);

    graphicsPipeline.pushConstants(commandBuffer, stageFlags, offset, size, values);
  }

  void PipelineManager::bindGraphicsPipelineDescriptorSet(const std::shared_ptr<CommandBuffer>& commandBuffer,
                                                          const PipelineType pipelineType,
                                                          VkDescriptorSet descriptorSet,
                                                          const uint32_t location) const
  {
    const auto& graphicsPipeline = getGraphicsPipeline(pipelineType);

    graphicsPipeline.bindDescriptorSet(commandBuffer, descriptorSet, location);
  }

  const GraphicsPipeline &PipelineManager::getGraphicsPipeline(PipelineType pipelineType) const
  {
    const auto it = m_graphicsPipelines.find(pipelineType);
    if (it == m_graphicsPipelines.end())
    {
      throw std::runtime_error("Pipeline for the given type does not exist");
    }

    return *it->second;
  }
} // ge