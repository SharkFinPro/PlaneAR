#ifndef PLANEAR_RENDERER2D_H
#define PLANEAR_RENDERER2D_H

#include "Primitives2D.h"
#include <glm/mat4x4.hpp>
#include <glm/vec4.hpp>
#include <vulkan/vulkan.h>
#include <memory>
#include <unordered_map>
#include <string>
#include <vector>

struct AAssetManager;

namespace ge {

  class AssetManager;
  class CommandBuffer;
  class Font;
  class LogicalDevice;
  class PipelineManager;
  class Renderer;
  struct RenderInfo;

  enum class RectMode {
    CORNER,
    CORNERS,
    CENTER,
    RADIUS
  };

  enum class EllipseMode {
    CENTER,
    RADIUS,
    CORNER,
    CORNERS
  };

  enum class ImageMode {
    CORNER,
    CORNERS,
    CENTER
  };

  enum class TextAlignH {
    LEFT,
    CENTER,
    RIGHT
  };

  enum class TextAlignV {
    BASELINE,
    TOP,
    CENTER,
    BOTTOM
  };

  class Renderer2D
  {
  public:
    explicit Renderer2D(std::shared_ptr<AssetManager> assetManager);

    void createNewFrame();

    void render(const std::shared_ptr<PipelineManager>& pipelineManager,
                const RenderInfo* renderInfo);

    void fill(float r,
              float g,
              float b,
              float a = 255.0f);

    void rotate(float angle);

    void translate(float x,
                   float y);

    void scale(float xy);

    void scale(float x,
               float y);

    void pushMatrix();

    void popMatrix();

    void rectMode(RectMode mode);

    void ellipseMode(EllipseMode mode);

    void imageMode(ImageMode mode);

    void rect(float x,
              float y,
              float width,
              float height);

    void triangle(float x1,
                  float y1,
                  float x2,
                  float y2,
                  float x3,
                  float y3);

    void ellipse(float x,
                 float y,
                 float width,
                 float height);

    void textFont(const std::string& font);

    void textFont(const std::string& font,
                  uint32_t size);

    void textSize(uint32_t size);

    void textAlign(TextAlignH h,
                   TextAlignV v = TextAlignV::BASELINE);

    [[nodiscard]] float textWidth(const std::string& text) const;

    [[nodiscard]] float textAscent(const std::string& text) const;

    [[nodiscard]] float textDescent(const std::string& text) const;

    void text(const std::string& text,
              float x,
              float y);

    void image(std::string image,
               float x,
               float y,
               float width,
               float height);

  private:
    std::shared_ptr<AssetManager> m_assetManager;

    glm::vec4 m_currentFill = glm::vec4(1.0f, 1.0f, 1.0f, 1.0f);

    glm::mat4 m_currentTransform = glm::mat4(1.0f);

    std::vector<glm::mat4> m_transformStack;

    RectMode m_rectMode = RectMode::CORNER;

    EllipseMode m_ellipseMode = EllipseMode::CENTER;

    ImageMode m_imageMode = ImageMode::CORNER;

    TextAlignH m_textAlignH = TextAlignH::LEFT;

    TextAlignV m_textAlignV = TextAlignV::BASELINE;

    std::vector<Rect> m_rectsToRender;

    std::vector<Triangle> m_trianglesToRender;

    std::vector<Ellipse> m_ellipsesToRender;

    std::unordered_map<std::string, std::unordered_map<uint32_t, std::vector<Glyph>>> m_glyphsToRender;

    std::shared_ptr<Font> m_currentFont;
    std::string m_currentFontName;
    uint32_t m_currentFontSize = 12;

    std::vector<Image> m_imagesToRender;

    float m_currentZ = 0.01f;

    [[nodiscard]] glm::vec4 resolveRectBounds(float a,
                                              float b,
                                              float c,
                                              float d);

    [[nodiscard]] glm::vec4 resolveEllipseBounds(float a,
                                                 float b,
                                                 float c,
                                                 float d);

    [[nodiscard]] glm::vec4 resolveImageBounds(float a,
                                               float b,
                                               float c,
                                               float d);

    void updateCurrentFont();

    void increaseCurrentZ();

    void normalizeZValues();

    void renderRects(const std::shared_ptr<PipelineManager>& pipelineManager,
                     const RenderInfo* renderInfo) const;

    static void renderRect(const std::shared_ptr<PipelineManager>& pipelineManager,
                           const RenderInfo* renderInfo,
                           const Rect& rect);

    void renderTriangles(const std::shared_ptr<PipelineManager>& pipelineManager,
                         const RenderInfo* renderInfo) const;

    static void renderTriangle(const std::shared_ptr<PipelineManager>& pipelineManager,
                               const RenderInfo* renderInfo,
                               const Triangle& triangle);

    void renderEllipses(const std::shared_ptr<PipelineManager>& pipelineManager,
                        const RenderInfo* renderInfo) const;

    static void renderEllipse(const std::shared_ptr<PipelineManager>& pipelineManager,
                              const RenderInfo* renderInfo,
                              const Ellipse& ellipse);

    void renderGlyphs(const std::shared_ptr<PipelineManager>& pipelineManager,
                      const RenderInfo* renderInfo) const;

    static void renderGlyph(const std::shared_ptr<PipelineManager>& pipelineManager,
                            const RenderInfo* renderInfo,
                            const Glyph& glyph);

    void renderImages(const std::shared_ptr<PipelineManager>& pipelineManager,
                      const RenderInfo* renderInfo) const;

    void renderImage(const std::shared_ptr<PipelineManager>& pipelineManager,
                     const RenderInfo* renderInfo,
                     const Image& image) const;
  };

} // ge

#endif //PLANEAR_RENDERER2D_H
