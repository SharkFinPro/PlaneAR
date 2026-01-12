#ifndef PLANEAR_PIPELINEMANAGER_H
#define PLANEAR_PIPELINEMANAGER_H

#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class AssetManager;
  class CommandBuffer;
  class LogicalDevice;
  class Renderer;

  enum class PipelineType;

  class PipelineManager
  {
  public:
    PipelineManager(std::shared_ptr<LogicalDevice> logicalDevice,
                    const std::shared_ptr<Renderer>& renderer,
                    const std::shared_ptr<AssetManager>& assetManager);

    ~PipelineManager();

    void bindGraphicsPipeline(const std::shared_ptr<CommandBuffer>& commandBuffer,
                              PipelineType pipelineType) const;

    void pushGraphicsPipelineConstants(const std::shared_ptr<CommandBuffer>& commandBuffer,
                                       PipelineType pipelineType,
                                       VkShaderStageFlags stageFlags,
                                       uint32_t offset,
                                       uint32_t size,
                                       const void* values) const;

    void bindGraphicsPipelineDescriptorSet(const std::shared_ptr<CommandBuffer>& commandBuffer,
                                           PipelineType pipelineType,
                                           VkDescriptorSet descriptorSet,
                                           uint32_t location) const;
  };

} // ge

#endif //PLANEAR_PIPELINEMANAGER_H
