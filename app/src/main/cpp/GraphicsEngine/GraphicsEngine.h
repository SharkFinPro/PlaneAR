#ifndef PLANEAR_GRAPHICSENGINE_H
#define PLANEAR_GRAPHICSENGINE_H

#include <vulkan/vulkan.h>
#include <memory>

struct android_app;

namespace ge {

  class Instance;
  class LogicalDevice;
  class PhysicalDevice;
  class RenderingManager;
  class Surface;

  class GraphicsEngine
  {
  public:
    explicit GraphicsEngine(android_app* pApp);

    ~GraphicsEngine();

  private:
    android_app* m_app;

    std::shared_ptr<Instance> m_instance;
    std::shared_ptr<Surface> m_surface;
    std::shared_ptr<PhysicalDevice> m_physicalDevice;
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    VkDescriptorPool m_descriptorPool = VK_NULL_HANDLE;

    std::shared_ptr<RenderingManager> m_renderingManager;

    void initializeVulkan();

    void createPools();

    void createCommandPool();

    void createDescriptorPool();

    void createComponents();
  };

} // ge

#endif //PLANEAR_GRAPHICSENGINE_H
