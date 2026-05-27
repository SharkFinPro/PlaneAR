#ifndef PLANEAR_MOUSEPICKER_H
#define PLANEAR_MOUSEPICKER_H

#include "Primitives2D.h"
#include <vulkan/vulkan.h>
#include <memory>

namespace ge {

  class LogicalDevice;
  class PipelineManager;
  struct RenderInfo;
  class SingleUseCommandBuffer;

  class MousePicker
  {
  public:
    MousePicker(std::shared_ptr<LogicalDevice> logicalDevice,
                VkCommandPool commandPool);

    ~MousePicker();

    void clearObjectsToMousePick();

    void addObjectToMousePick(MousePickingPoint object);

    void render(const std::shared_ptr<PipelineManager>& pipelineManager,
                const RenderInfo* renderInfo) const;

    void handleRenderedMousePickingImage(VkImage image);

    [[nodiscard]] uint32_t getSelectedID() const;

    void doMousePicking(uint32_t mouseX,
                        uint32_t mouseY);

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    VkBuffer m_stagingBuffer = VK_NULL_HANDLE;
    VkDeviceMemory m_stagingBufferMemory = VK_NULL_HANDLE;

    uint32_t m_selectedID = 0;

    std::vector<MousePickingPoint> m_objectsToMousePick;

    [[nodiscard]] uint32_t getIDFromMousePickingImage(VkImage image,
                                                      int32_t mouseX,
                                                      int32_t mouseY) const;

    [[nodiscard]] uint32_t getObjectIDFromBuffer(VkDeviceMemory stagingBufferMemory) const;

    static void transitionImageForReading(const SingleUseCommandBuffer& commandBuffer,
                                          VkImage image);

    static void transitionImageForWriting(const SingleUseCommandBuffer& commandBuffer,
                                          VkImage image);
  };

} // ge

#endif //PLANEAR_MOUSEPICKER_H
