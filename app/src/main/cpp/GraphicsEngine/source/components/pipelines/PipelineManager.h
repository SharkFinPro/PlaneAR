#ifndef PLANEAR_PIPELINEMANAGER_H
#define PLANEAR_PIPELINEMANAGER_H

#include <vulkan/vulkan.h>
#include <memory>
#include <unordered_map>

namespace ge {

  class AssetManager;
  class CommandBuffer;
  class GraphicsPipeline;
  struct GraphicsPipelineOptions;
  class LogicalDevice;
  class Renderer;

  enum class PipelineType {
    rect,
    triangle,
    ellipse,
    font,
    image,
    point,
    font3D
  };

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

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    VkDescriptorPool m_descriptorPool = VK_NULL_HANDLE;

    std::unordered_map<PipelineType, std::unique_ptr<GraphicsPipeline>> m_graphicsPipelines;

    void createCommandPool();

    void createDescriptorPool();

    void createPipelines(const std::shared_ptr<Renderer>& renderer,
                         const std::shared_ptr<AssetManager>& assetManager);

    void createGraphicsPipeline(PipelineType pipelineType,
                                const GraphicsPipelineOptions& graphicsPipelineOptions);

    [[nodiscard]] const GraphicsPipeline& getGraphicsPipeline(PipelineType pipelineType) const;
  };

} // ge

#endif //PLANEAR_PIPELINEMANAGER_H
