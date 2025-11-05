#ifndef PLANEAR_INSTANCE_H
#define PLANEAR_INSTANCE_H

#include <vulkan/vulkan.h>

namespace ge {

  class Instance
  {
  public:
    Instance();
    ~Instance();

  private:
    VkInstance m_instance = VK_NULL_HANDLE;
  };

} // ge

#endif //PLANEAR_INSTANCE_H
