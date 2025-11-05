#ifndef PLANEAR_GRAPHICSENGINE_H
#define PLANEAR_GRAPHICSENGINE_H

#include <memory>

namespace ge {

  class Instance;

  class GraphicsEngine
  {
  public:
    GraphicsEngine();

    ~GraphicsEngine();

  private:
    std::shared_ptr<Instance> m_instance;
  };

} // ge

#endif //PLANEAR_GRAPHICSENGINE_H
