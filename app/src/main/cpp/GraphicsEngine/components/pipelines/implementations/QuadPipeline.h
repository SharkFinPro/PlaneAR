#ifndef PLANEAR_QUADPIPELINE_H
#define PLANEAR_QUADPIPELINE_H

#include "../GraphicsPipeline.h"
#include <vector>

struct AAssetManager;

namespace ge {

  class CommandBuffer;
  class Surface;

  struct Rect {
    float x;
    float y;
    float width;
    float height;
    float r;
    float g;
    float b;
  };

  class QuadPipeline final : public GraphicsPipeline
  {
  public:
    QuadPipeline(const std::shared_ptr<LogicalDevice>& logicalDevice,
                 std::shared_ptr<RenderPass> renderPass,
                 AAssetManager* assetManager,
                 std::shared_ptr<Surface> surface);

    void render(const std::shared_ptr<CommandBuffer>& commandBuffer);

    void queueRectToRender(float x,
                           float y,
                           float width,
                           float height,
                           float r,
                           float g,
                           float b);

    void createNewFrame();

  private:
    std::shared_ptr<Surface> m_surface;

    std::vector<Rect> m_rectsToRender;

    void renderRect(const std::shared_ptr<CommandBuffer>& commandBuffer,
                    float x,
                    float y,
                    float width,
                    float height,
                    float r,
                    float g,
                    float b);
  };

} // ge

#endif //PLANEAR_QUADPIPELINE_H
