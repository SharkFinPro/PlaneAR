#include "GraphicsEngine.h"
#include "Logger.h"
#include "components/instance/Instance.h"
#include "components/logicalDevice/LogicalDevice.h"
#include "components/physicalDevice/PhysicalDevice.h"
#include "components/surface/Surface.h"

namespace ge {

  GraphicsEngine::GraphicsEngine(android_app* pApp)
    : m_app(pApp)
  {
    LOGI("Creating Graphics Engine!");

    initializeVulkan();
  }

  GraphicsEngine::~GraphicsEngine()
  {
    LOGI("Destroying Graphics Engine!");
  }

  void GraphicsEngine::initializeVulkan()
  {
    m_instance = std::make_shared<Instance>();

    m_surface = std::make_shared<Surface>(m_instance, m_app);

    m_physicalDevice = std::make_shared<PhysicalDevice>(m_instance, m_surface);

    m_logicalDevice = std::make_shared<LogicalDevice>(m_physicalDevice);
  }

} // ge