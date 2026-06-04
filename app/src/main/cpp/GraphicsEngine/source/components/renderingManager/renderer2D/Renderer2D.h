#ifndef PLANEAR_RENDERER2D_H
#define PLANEAR_RENDERER2D_H

#include "Primitives2D.h"
#include "../../descriptorSet/UniformBuffer.h"
#include <glm/mat4x4.hpp>
#include <glm/vec4.hpp>
#include <vulkan/vulkan.h>
#include <memory>
#include <unordered_map>
#include <string>
#include <variant>
#include <vector>

struct AAssetManager;

namespace ge {

  class AssetManager;
  class CommandBuffer;
  class DescriptorSet;
  class Font;
  class LogicalDevice;
  class MousePicker;
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
    Renderer2D(std::shared_ptr<LogicalDevice> logicalDevice,
               std::shared_ptr<AssetManager> assetManager);

    ~Renderer2D();

    void createNewFrame();

    void render(const std::shared_ptr<PipelineManager>& pipelineManager,
                const RenderInfo* renderInfo);

    [[nodiscard]] std::shared_ptr<MousePicker> getMousePicker();

    void renderMousePicking(const std::shared_ptr<PipelineManager>& pipelineManager,
                            const RenderInfo* renderInfo) const;

    void handleRenderedMousePickingImage(const VkImage image) const;

    void fill(float r,
              float g,
              float b,
              float a = 255.0f);

    void fill(float rgb,
              float a = 255.0f);

    void rotate(float angle);

    void translate(float x,
                   float y);

    void scale(float xy);

    void scale(float x,
               float y);

    void pushMatrix();

    void popMatrix();

    void resetMatrix();

    void rectMode(RectMode mode);

    void ellipseMode(EllipseMode mode);

    void imageMode(ImageMode mode);

    void rect(float x,
              float y,
              float width,
              float height,
              float radius = 0.0f);

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

    [[nodiscard]] float textWidth(const std::vector<uint32_t>& codepoints) const;

    [[nodiscard]] float textAscent(const std::vector<uint32_t>& codepoints) const;

    [[nodiscard]] float textDescent(const std::vector<uint32_t>& codepoints) const;

    void text(const std::string& text,
              float x,
              float y);

    void image(std::string image,
               float x,
               float y,
               float width,
               float height);

    [[nodiscard]] std::shared_ptr<AssetManager> getAssetManager() const;

    void point(float x,
               float y,
               float z,
               float size);

    // Set the aspect-ratio multipliers for the next point() draw.
    // aspectX > 1 makes the billboard wider than it is tall (e.g. 2.2 for a
    // wide info card).  Resets to 1.0/1.0 each frame via createNewFrame().
    void pointAspect(float aspectX,
                     float aspectY);

    void mousePickingPoint(float x,
                           float y,
                           float z,
                           float size,
                           uint32_t id);

    // Set the camera matrices directly from Android's 3x3 rotation matrix
    // (row-major, 9 floats as returned by SensorManager.getRotationMatrixFromVector).
    // The view matrix is built by treating the matrix rows as world-space axes,
    // then constructing glm::lookAt from the extracted forward and up vectors.
    // The projection matrix uses the same FOV and near/far as set3DView.
    void set3DViewMatrix(const float* rotationMatrix9,
                         float screenWidth,
                         float screenHeight);

    void text3D(const std::string& text,
                float x,
                float y,
                float z);

    void camera(float x,
                float y,
                float width,
                float height);

    // Draw a compass billboard anchored to the given 3-D world position.
    //
    // x, y, z      — aircraft world position (same coords used for point()).
    // size         — half-size of the quad in world units (controls how large
    //                the compass appears regardless of depth).
    // offsetX/Y    — shift in world units along camRight / camUp so the compass
    //                sits top-right of the aircraft dot.
    // headingRad   — aircraft heading in radians, already adjusted for the
    //                active compass mode (Always North vs User Relative).
    //                0 = pointing up (+Y / north), increases clockwise.
    // alpha        — opacity 0–1.
    void compass(float x,
                 float y,
                 float z,
                 float size,
                 float offsetX,
                 float offsetY,
                 float headingRad,
                 float alpha);

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    struct GlyphCommand {
      Glyph glyph;
      std::string fontName;
      uint32_t fontSize;
    };

    struct Glyph3DCommand {
      Glyph3DInstance instance;
      std::string fontName;
      uint32_t fontSize;
    };

    // Marks the draw list position of a point instanced batch flush.
    // firstInstance/instanceCount index into m_pointInstances.
    struct PointBatchMarker {
      uint32_t firstInstance;
      uint32_t instanceCount;
    };

    // Marks the draw list position of a glyph3D instanced batch flush.
    // firstInstance/instanceCount index into m_glyph3DInstances.
    struct Glyph3DBatchMarker {
      uint32_t firstInstance;
      uint32_t instanceCount;
      std::string fontName;
      uint32_t fontSize;
    };

    // Marks the draw list position of a compass instanced batch flush.
    // firstInstance/instanceCount index into m_compassInstances.
    struct CompassBatchMarker {
      uint32_t firstInstance;
      uint32_t instanceCount;
    };

    using DrawCommand = std::variant<Rect, Triangle, Ellipse, GlyphCommand, Image, PointBatchMarker, Glyph3DBatchMarker, Camera, CompassBatchMarker>;

    struct DrawEntry {
      DrawCommand command;
      float z;
    };

    std::shared_ptr<AssetManager> m_assetManager;

    glm::vec4 m_currentFill = glm::vec4(1.0f, 1.0f, 1.0f, 1.0f);

    glm::mat4 m_currentTransform = glm::mat4(1.0f);

    std::vector<glm::mat4> m_transformStack;

    RectMode m_rectMode = RectMode::CORNER;

    EllipseMode m_ellipseMode = EllipseMode::CENTER;

    ImageMode m_imageMode = ImageMode::CORNER;

    TextAlignH m_textAlignH = TextAlignH::LEFT;

    TextAlignV m_textAlignV = TextAlignV::BASELINE;

    std::vector<DrawEntry> m_drawList;

    // Flat instance data arrays filled each frame; markers in m_drawList index into these.
    std::vector<PointInstance> m_pointInstances;
    std::vector<Glyph3DInstance> m_glyph3DInstances;
    std::vector<CompassInstance> m_compassInstances;

    std::vector<VkBuffer> m_pointInstanceBuffers;
    std::vector<VkDeviceMemory> m_pointInstanceMemory;
    VkDeviceSize m_pointInstanceBufferCapacity = 0;

    std::vector<VkBuffer> m_glyph3DInstanceBuffers;
    std::vector<VkDeviceMemory> m_glyph3DInstanceMemory;
    VkDeviceSize m_glyph3DInstanceBufferCapacity = 0;

    std::vector<VkBuffer> m_compassInstanceBuffers;
    std::vector<VkDeviceMemory> m_compassInstanceMemory;
    VkDeviceSize m_compassInstanceBufferCapacity = 0;

    glm::mat4 m_viewMatrix = glm::mat4(1.0f);
    glm::mat4 m_projectionMatrix = glm::mat4(1.0f);

    std::shared_ptr<Font> m_currentFont;
    std::string m_currentFontName;
    uint32_t m_currentFontSize = 12;

    float m_currentZ = 0.01f;

    // Per-draw aspect-ratio multipliers for point() billboard quads.
    // Reset to 1.0 each frame; set via pointAspect() before a point() call.
    float m_pointAspectX = 1.0f;
    float m_pointAspectY = 1.0f;

    std::shared_ptr<MousePicker> m_mousePicker;

    std::shared_ptr<DescriptorSet> m_glyph3DDescriptorSet;

    std::unique_ptr<UniformBuffer> m_cameraUniform;

    VkDescriptorPool m_descriptorPool = VK_NULL_HANDLE;

    VkDescriptorSet currentGlyph3DCameraSet = VK_NULL_HANDLE;
    VkDescriptorSet currentGlyph3DFontSet = VK_NULL_HANDLE;

    VkDescriptorSet currentGlyphFontSet = VK_NULL_HANDLE;

    VkDescriptorSet currentPointCameraSet = VK_NULL_HANDLE;

    VkDescriptorSet currentCompassCameraSet = VK_NULL_HANDLE;

    glm::vec3 m_camRight = glm::vec3(0.0f);
    glm::vec3 m_camUp = glm::vec3(0.0f);

    void createCommandPool();

    void ensureInstanceBuffer(std::vector<VkBuffer>& buffers,
                              std::vector<VkDeviceMemory>& memory,
                              VkDeviceSize& capacity,
                              VkDeviceSize requiredBytes);

    void destroyInstanceBuffers(std::vector<VkBuffer>& buffers,
                                std::vector<VkDeviceMemory>& memory);

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

    static void renderRect(const std::shared_ptr<PipelineManager>& pipelineManager,
                           const RenderInfo* renderInfo,
                           const Rect& rect);

    static void renderTriangle(const std::shared_ptr<PipelineManager>& pipelineManager,
                               const RenderInfo* renderInfo,
                               const Triangle& triangle);

    static void renderEllipse(const std::shared_ptr<PipelineManager>& pipelineManager,
                              const RenderInfo* renderInfo,
                              const Ellipse& ellipse);

    void renderGlyph(const std::shared_ptr<PipelineManager>& pipelineManager,
                     const RenderInfo* renderInfo,
                     const GlyphCommand& glyphCmd);

    void renderImage(const std::shared_ptr<PipelineManager>& pipelineManager,
                     const RenderInfo* renderInfo,
                     const Image& image) const;

    void renderPointBatch(const std::shared_ptr<PipelineManager>& pipelineManager,
                          const RenderInfo* renderInfo,
                          const PointBatchMarker& marker);

    void renderGlyph3DBatch(const std::shared_ptr<PipelineManager>& pipelineManager,
                            const RenderInfo* renderInfo,
                            const Glyph3DBatchMarker& marker);

    void renderCamera(const std::shared_ptr<PipelineManager>& pipelineManager,
                      const RenderInfo* renderInfo,
                      const Camera& camera) const;

    void renderCompassBatch(const std::shared_ptr<PipelineManager>& pipelineManager,
                            const RenderInfo* renderInfo,
                            const CompassBatchMarker& marker);
  };

} // ge

#endif //PLANEAR_RENDERER2D_H
