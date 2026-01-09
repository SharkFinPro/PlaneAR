#ifndef PLANEAR_RECTPIPELINE_H
#define PLANEAR_RECTPIPELINE_H

#include "../GraphicsPipeline.h"
#include <vector>
#include <glm/mat4x4.hpp>

struct AAssetManager;

namespace ge {

  struct Rect;

  struct RectPushConstant {
    glm::mat4 transform;
    int screenWidth;
    int screenHeight;
    float z;
    float x;
    float y;
    float width;
    float height;
    float r;
    float g;
    float b;
    float a;
  };

  class RectPipeline final : public GraphicsPipeline
  {
  public:
    RectPipeline(const std::shared_ptr<LogicalDevice>& logicalDevice,
                 std::shared_ptr<RenderPass> renderPass,
                 AAssetManager* assetManager);

    void render(const RenderInfo* renderInfo,
                const std::vector<Rect>* rects);

  private:
    void renderRect(const RenderInfo* renderInfo,
                    Rect rect);
  };

} // ge

#endif //PLANEAR_RECTPIPELINE_H
