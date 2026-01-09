#include "Pipeline.h"
#include "../logicalDevice/LogicalDevice.h"

namespace ge {
  Pipeline::Pipeline(std::shared_ptr<LogicalDevice> logicalDevice)
    : m_logicalDevice(std::move(logicalDevice))
  {}

  Pipeline::~Pipeline()
  {
    m_logicalDevice->destroyPipeline(m_pipeline);

    m_logicalDevice->destroyPipelineLayout(m_pipelineLayout);
  }
} // ge