#ifndef PLANEAR_SURFACE_H
#define PLANEAR_SURFACE_H

#include <vulkan/vulkan.h>
#include <memory>

struct android_app;

namespace ge {

  class Instance;

  class Surface
  {
  public:
    Surface(std::shared_ptr<Instance> instance, android_app* pApp);
    ~Surface();

    [[nodiscard]] VkSurfaceKHR getSurface() const;

  private:
    std::shared_ptr<Instance> m_instance;

    VkSurfaceKHR m_surface = VK_NULL_HANDLE;
  };

} // ge

#endif //PLANEAR_SURFACE_H
