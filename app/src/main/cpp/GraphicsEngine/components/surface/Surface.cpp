#include "Surface.h"
#include "../instance/Instance.h"
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <utility>

namespace ge {
  Surface::Surface(std::shared_ptr<Instance> instance, android_app* pApp)
    : m_instance(std::move(instance))
  {
    m_surface = m_instance->createSurface(pApp->window);
  }

  Surface::~Surface()
  {
    m_instance->destroySurface(m_surface);
  }
} // ge