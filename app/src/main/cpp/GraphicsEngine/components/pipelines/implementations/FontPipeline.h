#ifndef PLANEAR_FONTPIPELINE_H
#define PLANEAR_FONTPIPELINE_H

#include "../GraphicsPipeline.h"

struct AAssetManager;

namespace ge {

  class CommandBuffer;

  class FontPipeline final : public GraphicsPipeline
  {
  public:
    FontPipeline(const std::shared_ptr<LogicalDevice>& logicalDevice,
                 std::shared_ptr<RenderPass> renderPass,
                 AAssetManager* assetManager);

    void render(const std::shared_ptr<CommandBuffer>& commandBuffer);
  };

} // ge

#endif //PLANEAR_FONTPIPELINE_H
