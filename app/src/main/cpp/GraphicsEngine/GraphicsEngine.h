#ifndef PLANEAR_GRAPHICSENGINE_H
#define PLANEAR_GRAPHICSENGINE_H

#include <memory>

struct android_app;

namespace ge {

  class Instance;
  class LogicalDevice;
  class PhysicalDevice;
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

    void initializeVulkan();
  };

} // ge

#endif //PLANEAR_GRAPHICSENGINE_H
