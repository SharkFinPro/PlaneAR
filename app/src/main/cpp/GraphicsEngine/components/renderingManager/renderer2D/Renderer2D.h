#ifndef PLANEAR_RENDERER2D_H
#define PLANEAR_RENDERER2D_H

#include <glm/mat4x4.hpp>
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

  struct Fill {
    float r;
    float g;
    float b;
  };

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

    void fill(float r,
              float g,
              float b);

    void rotate(float angle);

    void translate(float x,
                   float y);

    void scale(float xy);

    void scale(float x,
               float y);

    void rect(float x,
              float y,
              float width,
              float height);

    void text(std::string message,
              float x,
              float y);

  private:
    std::shared_ptr<QuadPipeline> m_quadPipeline;

    std::shared_ptr<FontPipeline> m_fontPipeline;

    Fill m_currentFill = {1, 1, 1};

    glm::mat4 m_currentTransform = glm::mat4(1.0f);
  };

} // ge

#endif //PLANEAR_RENDERER2D_H
