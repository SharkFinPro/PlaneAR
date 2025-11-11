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

    [[nodiscard]] int32_t getWidth() const;

    [[nodiscard]] int32_t getHeight() const;

  private:
    std::shared_ptr<Instance> m_instance;

    VkSurfaceKHR m_surface = VK_NULL_HANDLE;

    int32_t m_width;
    int32_t m_height;
  };

} // ge

#endif //PLANEAR_SURFACE_H
