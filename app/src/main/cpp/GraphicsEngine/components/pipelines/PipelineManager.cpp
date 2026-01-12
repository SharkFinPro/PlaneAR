#include "PipelineManager.h"

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

  }

  void PipelineManager::pushGraphicsPipelineConstants(const std::shared_ptr<CommandBuffer>& commandBuffer,
                                                      const PipelineType pipelineType,
                                                      const VkShaderStageFlags stageFlags,
                                                      const uint32_t offset,
                                                      const uint32_t size,
                                                      const void* values) const
  {

  }

  void PipelineManager::bindGraphicsPipelineDescriptorSet(const std::shared_ptr<CommandBuffer>& commandBuffer,
                                                          const PipelineType pipelineType,
                                                          VkDescriptorSet descriptorSet,
                                                          const uint32_t location) const
  {

  }
} // ge