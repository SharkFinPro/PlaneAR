#ifndef PLANEAR_QUADPIPELINE_H
#define PLANEAR_QUADPIPELINE_H

#include "../GraphicsPipeline.h"

struct AAssetManager;

namespace ge {

  class CommandBuffer;

  class QuadPipeline final : public GraphicsPipeline
  {
  public:
    QuadPipeline(const std::shared_ptr<LogicalDevice>& logicalDevice,
                 std::shared_ptr<RenderPass> renderPass,
                 AAssetManager* assetManager);

    void render(const std::shared_ptr<CommandBuffer> commandBuffer);
  };

} // ge

#endif //PLANEAR_QUADPIPELINE_H
