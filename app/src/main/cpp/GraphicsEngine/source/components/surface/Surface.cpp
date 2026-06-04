#include "Surface.h"
#include "../instance/Instance.h"
#include <android/native_window.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <utility>

namespace ge {
  Surface::Surface(std::shared_ptr<Instance> instance, android_app* pApp)
    : m_instance(std::move(instance))
  {
    const auto pWindow = pApp->window;

    m_surface = m_instance->createSurface(pWindow);

    m_width = ANativeWindow_getWidth(pWindow);

    m_height = ANativeWindow_getHeight(pWindow);
  }

  Surface::Surface(std::shared_ptr<Instance> instance, ANativeWindow* window)
    : m_instance(std::move(instance))
  {
    m_surface = m_instance->createSurface(window);

    m_width = ANativeWindow_getWidth(window);

    m_height = ANativeWindow_getHeight(window);
  }

  Surface::~Surface()
  {
    m_instance->destroySurface(m_surface);
  }

  VkSurfaceKHR Surface::getSurface() const
  {
    return m_surface;
  }

  int32_t Surface::getWidth() const
  {
    return m_width;
  }

  int32_t Surface::getHeight() const
  {
    return m_height;
  }
} // ge