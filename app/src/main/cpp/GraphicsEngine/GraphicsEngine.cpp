#include "GraphicsEngine.h"
#include "Logger.h"
#include "components/assets/AssetManager.h"
#include "components/instance/Instance.h"
#include "components/logicalDevice/LogicalDevice.h"
#include "components/physicalDevice/PhysicalDevice.h"
#include "components/renderingManager/RenderingManager.h"
#include "components/surface/Surface.h"
#include <game-activity/native_app_glue/android_native_app_glue.h>

namespace ge {

  GraphicsEngine::GraphicsEngine(android_app* pApp)
    : m_app(pApp)
  {
    LOGI("Creating Graphics Engine!");

    initializeVulkan();

    createPools();

    createComponents();
  }

  GraphicsEngine::~GraphicsEngine()
  {
    LOGI("Destroying Graphics Engine!");

    m_logicalDevice->waitIdle();

    m_logicalDevice->destroyDescriptorPool(m_descriptorPool);

    m_logicalDevice->destroyCommandPool(m_commandPool);
  }

  void GraphicsEngine::render()
  {
    m_renderingManager->doRendering(m_currentFrame);

    createNewFrame();
  }

  std::shared_ptr<AssetManager> GraphicsEngine::getAssetManager() const
  {
    return m_assetManager;
  }

  std::shared_ptr<RenderingManager> GraphicsEngine::getRenderingManager() const
  {
    return m_renderingManager;
  }

  void GraphicsEngine::initializeVulkan()
  {
    m_instance = std::make_shared<Instance>();

    m_surface = std::make_shared<Surface>(m_instance, m_app);

    m_physicalDevice = std::make_shared<PhysicalDevice>(m_instance, m_surface);

    m_logicalDevice = std::make_shared<LogicalDevice>(m_physicalDevice);
  }

  void GraphicsEngine::createPools()
  {
    createCommandPool();

    createDescriptorPool();
  }

  void GraphicsEngine::createCommandPool()
  {
    const auto queueFamilyIndices = m_physicalDevice->getQueueFamilies();

    const VkCommandPoolCreateInfo poolInfo {
      .sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO,
      .flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT,
      .queueFamilyIndex = queueFamilyIndices.graphicsFamily.value()
    };

    m_commandPool = m_logicalDevice->createCommandPool(poolInfo);
  }

  void GraphicsEngine::createDescriptorPool()
  {
    const std::array<VkDescriptorPoolSize, 3> poolSizes {{
       {VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, m_logicalDevice->getMaxFramesInFlight() * 30},
       {VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, m_logicalDevice->getMaxFramesInFlight() * 50},
       {VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, m_logicalDevice->getMaxFramesInFlight() * 10}
     }};

    const VkDescriptorPoolCreateInfo poolCreateInfo {
      .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO,
      .maxSets = m_logicalDevice->getMaxFramesInFlight() * 30,
      .poolSizeCount = static_cast<uint32_t>(poolSizes.size()),
      .pPoolSizes = poolSizes.data()
    };

    m_descriptorPool = m_logicalDevice->createDescriptorPool(poolCreateInfo);
  }

  void GraphicsEngine::createComponents()
  {
    m_assetManager = std::make_shared<AssetManager>(m_logicalDevice, m_app->activity->assetManager);

    m_renderingManager = std::make_shared<RenderingManager>(
      m_logicalDevice,
      m_surface,
      m_assetManager,
      m_commandPool,
      m_descriptorPool
    );
  }

  void GraphicsEngine::createNewFrame()
  {
    m_currentFrame = (m_currentFrame + 1) % m_logicalDevice->getMaxFramesInFlight();

    m_renderingManager->createNewFrame();
  }

} // ge