#ifndef PLANEAR_RENDERER2D_H
#define PLANEAR_RENDERER2D_H

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
  class FontPipeline;
  class LogicalDevice;
  class RectPipeline;
  class Renderer;
  struct RenderInfo;
  class TrianglePipeline;

  struct Glyph {
    glm::vec4 bounds;
    glm::vec4 color;
    glm::mat4 transform;
    glm::vec4 uv;
    float z;
  };

  struct Rect {
    glm::vec4 bounds;
    glm::vec4 color;
    glm::mat4 transform;
    float z;
  };

  struct Triangle {
    glm::vec2 p1;
    glm::vec2 p2;
    glm::vec2 p3;
    glm::vec4 color;
    glm::mat4 transform;
    float z;
  };

  struct Ellipse {
    glm::vec4 bounds;
    glm::vec4 color;
    glm::mat4 transform;
    float z;
  };

  class Renderer2D
  {
  public:
    Renderer2D(const std::shared_ptr<LogicalDevice>& logicalDevice,
               const std::shared_ptr<Renderer>& renderer,
               std::shared_ptr<AssetManager> assetManager,
               VkCommandPool commandPool,
               VkDescriptorPool descriptorPool);

    void createNewFrame();

    void render(const RenderInfo* renderInfo);

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

    void text(const std::string& text,
              float x,
              float y);

  private:
    std::shared_ptr<AssetManager> m_assetManager;

    std::shared_ptr<RectPipeline> m_rectPipeline;

    std::shared_ptr<TrianglePipeline> m_trianglePipeline;

    std::shared_ptr<FontPipeline> m_fontPipeline;

    glm::vec4 m_currentFill = glm::vec4(1.0f, 1.0f, 1.0f, 1.0f);

    glm::mat4 m_currentTransform = glm::mat4(1.0f);

    std::vector<glm::mat4> m_transformStack;

    std::unordered_map<std::string, std::unordered_map<uint32_t, std::vector<Glyph>>> m_glyphsToRender;

    std::shared_ptr<Font> m_currentFont;
    std::string m_currentFontName;
    uint32_t m_currentFontSize = 12;

    std::vector<Rect> m_rectsToRender;

    std::vector<Triangle> m_trianglesToRender;

    std::vector<Ellipse> m_ellipsesToRender;

    float m_currentZ = 0.0f;

    void updateCurrentFont();

    void increaseCurrentZ();

    void normalizeZValues();
  };

} // ge

#endif //PLANEAR_RENDERER2D_H
