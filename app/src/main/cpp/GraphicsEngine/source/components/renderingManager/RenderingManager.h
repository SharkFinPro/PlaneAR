#ifndef PLANEAR_RENDERINGMANAGER_H
#define PLANEAR_RENDERINGMANAGER_H

#include <vulkan/vulkan.h>
#include <memory>

struct ANativeWindow;
struct AAssetManager;

namespace ge {

  class AssetManager;
  class CommandBuffer;
  class Instance;
  class LogicalDevice;
  class PipelineManager;
  class Renderer;
  class Renderer2D;
  class Surface;
  class Swapchain;

  class RenderingManager
  {
  public:
    RenderingManager(const std::shared_ptr<LogicalDevice>& logicalDevice,
                     const std::shared_ptr<Instance>& instance,
                     const std::shared_ptr<Surface>& surface,
                     std::shared_ptr<AssetManager> assetManager,
                     VkCommandPool commandPool);

    void doRendering(const std::shared_ptr<PipelineManager>& pipelineManager,
                     uint32_t currentFrame);

    void suspend();

    void resume(ANativeWindow* window);

    void createNewFrame();

    [[nodiscard]] std::shared_ptr<Renderer> getRenderer() const;

    [[nodiscard]] std::shared_ptr<Renderer2D> getRenderer2D();

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    std::shared_ptr<Instance> m_instance;

    std::shared_ptr<Surface> m_surface;

    std::shared_ptr<Renderer> m_renderer;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    std::shared_ptr<Swapchain> m_swapchain;

    std::shared_ptr<CommandBuffer> m_swapchainCommandBuffer;

    std::shared_ptr<CommandBuffer> m_mousePickingCommandBuffer;

    std::shared_ptr<Renderer2D> m_renderer2D;

    void recordSwapchainCommandBuffer(const std::shared_ptr<PipelineManager>& pipelineManager,
                                      uint32_t currentFrame,
                                      uint32_t imageIndex) const;

    void recordMousePickingCommandBuffer(const std::shared_ptr<PipelineManager>& pipelineManager,
                                         uint32_t currentFrame) const;

    void doMousePicking(const std::shared_ptr<PipelineManager>& pipelineManager,
                        uint32_t currentFrame) const;
  };

} // ge

#endif //PLANEAR_RENDERINGMANAGER_H
