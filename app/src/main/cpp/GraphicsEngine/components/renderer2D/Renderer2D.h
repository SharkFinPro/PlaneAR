#ifndef PLANEAR_RENDERER2D_H
#define PLANEAR_RENDERER2D_H

#include <vulkan/vulkan.h>
#include <memory>
#include <string>

struct AAssetManager;

namespace ge {

  class CommandBuffer;
  class FontPipeline;
  class LogicalDevice;
  class QuadPipeline;
  class Renderer;
  class Surface;

  class Renderer2D
  {
  public:
    Renderer2D(const std::shared_ptr<LogicalDevice>& logicalDevice,
               const std::shared_ptr<Surface>& surface,
               const std::shared_ptr<Renderer>& renderer,
               VkCommandPool commandPool,
               AAssetManager* assetManager,
               VkDescriptorPool descriptorPool);

    void createNewFrame();

    void render(const std::shared_ptr<CommandBuffer>& commandBuffer,
                uint32_t currentFrame);

    void renderRect(float x,
                    float y,
                    float width,
                    float height,
                    float r,
                    float g,
                    float b);

    void renderText(std::string message,
                    float x,
                    float y,
                    float r,
                    float g,
                    float b);

  private:
    std::shared_ptr<QuadPipeline> m_quadPipeline;

    std::shared_ptr<FontPipeline> m_fontPipeline;
  };

} // ge

#endif //PLANEAR_RENDERER2D_H
