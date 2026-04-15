#include "PipelineManager.h"
#include "GraphicsPipeline.h"
#include "PipelineConfig.h"
#include "../assets/AssetManager.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../physicalDevice/PhysicalDevice.h"
#include "PipelineManager.h"
#include "../renderingManager/Renderer.h"

namespace ge {
  PipelineManager::PipelineManager(std::shared_ptr<LogicalDevice> logicalDevice,
                                   const std::shared_ptr<Renderer>& renderer,
                                   const std::shared_ptr<AssetManager>& assetManager)
    : m_logicalDevice(std::move(logicalDevice)), m_renderer(renderer), m_assetManager(assetManager)
  {
    createCommandPool();

    createDescriptorPool();

    createPipelines();
  }

  PipelineManager::~PipelineManager()
  {
    m_logicalDevice->destroyDescriptorPool(m_descriptorPool);

    m_logicalDevice->destroyCommandPool(m_commandPool);
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

  void PipelineManager::createCameraPipeline(VkDescriptorSetLayout cameraLayout)
  {
    const auto renderPass = m_renderer->getRenderPass();

    createGraphicsPipeline(PipelineType::camera,
      PipelineConfig::createImagePipelineOptions(
        m_logicalDevice,
        renderPass,
        m_assetManager->getAAssetManager(),
        cameraLayout
      )
    );
  }

  bool PipelineManager::hasCameraPipeline() const
  {
    return m_graphicsPipelines.contains(PipelineType::camera);
  }

  void PipelineManager::createCommandPool()
  {
    const VkCommandPoolCreateInfo poolInfo {
      .sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO,
      .queueFamilyIndex = m_logicalDevice->getPhysicalDevice()->getQueueFamilies().graphicsFamily.value()
    };

    m_commandPool = m_logicalDevice->createCommandPool(poolInfo);
  }

  void PipelineManager::createDescriptorPool()
  {
    const std::array<VkDescriptorPoolSize, 1> poolSizes {{
      {VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, m_logicalDevice->getMaxFramesInFlight() * 30}
    }};

    const VkDescriptorPoolCreateInfo poolCreateInfo {
      .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO,
      .maxSets = m_logicalDevice->getMaxFramesInFlight() * 30,
      .poolSizeCount = static_cast<uint32_t>(poolSizes.size()),
      .pPoolSizes = poolSizes.data()
    };

    m_descriptorPool = m_logicalDevice->createDescriptorPool(poolCreateInfo);
  }

  const GraphicsPipeline& PipelineManager::getGraphicsPipeline(PipelineType pipelineType) const
  {
    const auto it = m_graphicsPipelines.find(pipelineType);
    if (it == m_graphicsPipelines.end())
    {
      throw std::runtime_error("Pipeline for the given type does not exist");
    }

    return *it->second;
  }

  void PipelineManager::createPipelines()
  {
    const auto renderPass = m_renderer->getRenderPass();
    const auto AAssetManager = m_assetManager->getAAssetManager();

    createGraphicsPipeline(PipelineType::rect,
                           PipelineConfig::createRectPipelineOptions(m_logicalDevice,
                           renderPass, AAssetManager));

    createGraphicsPipeline(PipelineType::triangle,
                           PipelineConfig::createTrianglePipelineOptions(m_logicalDevice,
                           renderPass, AAssetManager));

    createGraphicsPipeline(PipelineType::ellipse,
                           PipelineConfig::createEllipsePipelineOptions(m_logicalDevice,
                           renderPass, AAssetManager));

    createGraphicsPipeline(PipelineType::font,
                           PipelineConfig::createFontPipelineOptions(m_logicalDevice, renderPass,
                                               AAssetManager, m_assetManager->getFontDescriptorSetLayout()));

    createGraphicsPipeline(PipelineType::image,
                           PipelineConfig::createImagePipelineOptions(m_logicalDevice, renderPass,
                                               AAssetManager, m_assetManager->getImageDescriptorSetLayout()));
  }

  void PipelineManager::createGraphicsPipeline(PipelineType pipelineType,
                                               const GraphicsPipelineOptions& graphicsPipelineOptions)
  {
    m_graphicsPipelines[pipelineType] = std::make_unique<GraphicsPipeline>(m_logicalDevice, graphicsPipelineOptions);
  }
} // ge