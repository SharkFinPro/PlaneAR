#ifndef PLANEAR_MOUSEPICKER_H
#define PLANEAR_MOUSEPICKER_H

#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class LogicalDevice;

  class MousePicker
  {
  public:
    MousePicker(std::shared_ptr<LogicalDevice> logicalDevice,
                VkCommandPool commandPool);

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkCommandPool m_commandPool;
  };

} // ge

#endif //PLANEAR_MOUSEPICKER_H
