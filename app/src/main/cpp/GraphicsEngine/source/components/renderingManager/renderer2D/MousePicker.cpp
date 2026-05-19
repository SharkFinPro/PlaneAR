#include "MousePicker.h"

namespace ge {
  MousePicker::MousePicker(std::shared_ptr<LogicalDevice> logicalDevice,
                           VkCommandPool commandPool)
    : m_logicalDevice(std::move(logicalDevice)), m_commandPool(commandPool)
  {

  }
} // ge