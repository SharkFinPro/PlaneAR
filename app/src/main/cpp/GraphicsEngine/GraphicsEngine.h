#ifndef PLANEAR_GRAPHICSENGINE_H
#define PLANEAR_GRAPHICSENGINE_H

#include <memory>

struct android_app;

namespace ge {

  class Instance;

  class GraphicsEngine
  {
  public:
    explicit GraphicsEngine(android_app* pApp);

    ~GraphicsEngine();

  private:
    android_app* m_app;

    std::shared_ptr<Instance> m_instance;
  };

} // ge

#endif //PLANEAR_GRAPHICSENGINE_H
