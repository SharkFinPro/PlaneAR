#ifndef PLANEAR_ELLIPSEPIPELINE_H
#define PLANEAR_ELLIPSEPIPELINE_H

#include "../GraphicsPipeline.h"
#include <glm/mat4x4.hpp>

namespace ge {

  struct Ellipse;

  struct EllipsePushConstant {
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

  class EllipsePipeline final : public GraphicsPipeline
  {
  public:
    EllipsePipeline(std::shared_ptr<LogicalDevice> logicalDevice,
                    std::shared_ptr<RenderPass> renderPass,
                    AAssetManager* assetManager);

    void render(const RenderInfo* renderInfo,
                const std::vector<Ellipse>* ellipses);

  private:
    void renderEllipse(const RenderInfo* renderInfo,
                       const Ellipse& ellipse) const;
  };

} // ge

#endif //PLANEAR_ELLIPSEPIPELINE_H
