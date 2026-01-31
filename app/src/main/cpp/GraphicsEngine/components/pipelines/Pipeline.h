#ifndef PLANEAR_PIPELINE_H
#define PLANEAR_PIPELINE_H

#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class CommandBuffer;
  class LogicalDevice;

  class Pipeline
  {
  public:
    explicit Pipeline(std::shared_ptr<LogicalDevice> logicalDevice);

    virtual ~Pipeline();

    void pushConstants(const std::shared_ptr<CommandBuffer>& commandBuffer,
                       VkShaderStageFlags stageFlags,
                       uint32_t offset,
                       uint32_t size,
                       const void* values) const;

  protected:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkPipelineLayout m_pipelineLayout = VK_NULL_HANDLE;
    VkPipeline m_pipeline = VK_NULL_HANDLE;
  };

} // ge

#endif //PLANEAR_PIPELINE_H
