#include "Renderer2D.h"
#include "MousePicker.h"
#include "../../assets/AssetManager.h"
#include "../../assets/fonts/Font.h"
#include "../../assets/textures/CameraTexture.h"
#include "../../assets/textures/ImageTexture.h"
#include "../../commandBuffer/CommandBuffer.h"
#include "../../descriptorSet/DescriptorSet.h"
#include "../../logicalDevice/LogicalDevice.h"
#include "../../physicalDevice/PhysicalDevice.h"
#include "../../pipelines/GraphicsPipeline.h"
#include "../../pipelines/PipelineManager.h"
#include <algorithm>
#include <glm/gtc/matrix_transform.hpp>

namespace ge {
  Renderer2D::Renderer2D(std::shared_ptr<LogicalDevice> logicalDevice,
                         std::shared_ptr<AssetManager> assetManager)
    : m_logicalDevice(std::move(logicalDevice)), m_assetManager(std::move(assetManager))
  {
    createCommandPool();

    m_mousePicker = std::make_shared<MousePicker>(m_logicalDevice, m_commandPool);

    std::vector<VkDescriptorPoolSize> poolSizes {{
      {VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, m_logicalDevice->getMaxFramesInFlight() * 4}
    }};

    m_cameraUniform = std::make_unique<UniformBuffer>(m_logicalDevice, sizeof(Camera3DUBO));

    const VkDescriptorPoolCreateInfo poolCreateInfo {
      .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO,
      .maxSets = m_logicalDevice->getMaxFramesInFlight() * 3,
      .poolSizeCount = static_cast<uint32_t>(poolSizes.size()),
      .pPoolSizes = poolSizes.data()
    };

    m_descriptorPool = m_logicalDevice->createDescriptorPool(poolCreateInfo);

    m_glyph3DDescriptorSet = std::make_shared<DescriptorSet>(
      m_logicalDevice,
      m_descriptorPool,
      m_assetManager->getGlyph3DDescriptorSetLayout()
    );
    m_glyph3DDescriptorSet->updateDescriptorSets([this](const VkDescriptorSet descriptorSet, const size_t frame)
    {
      std::vector descriptorWrites{{
        m_cameraUniform->getDescriptorSet(0, descriptorSet, frame)
      }};

      return descriptorWrites;
    });

    const uint32_t frames = m_logicalDevice->getMaxFramesInFlight();
    constexpr VkDeviceSize kInitialPointCapacity = sizeof(PointInstance) * 64;
    constexpr VkDeviceSize kInitialGlyph3DCapacity = sizeof(Glyph3DInstance) * 256;

    m_pointInstanceBuffers.resize(frames, VK_NULL_HANDLE);
    m_pointInstanceMemory.resize(frames, VK_NULL_HANDLE);
    ensureInstanceBuffer(m_pointInstanceBuffers, m_pointInstanceMemory,
                         m_pointInstanceBufferCapacity, kInitialPointCapacity);

    m_glyph3DInstanceBuffers.resize(frames, VK_NULL_HANDLE);
    m_glyph3DInstanceMemory.resize(frames, VK_NULL_HANDLE);
    ensureInstanceBuffer(m_glyph3DInstanceBuffers, m_glyph3DInstanceMemory,
                         m_glyph3DInstanceBufferCapacity, kInitialGlyph3DCapacity);
  }

  Renderer2D::~Renderer2D()
  {
    destroyInstanceBuffers(m_pointInstanceBuffers, m_pointInstanceMemory);
    destroyInstanceBuffers(m_glyph3DInstanceBuffers, m_glyph3DInstanceMemory);

    m_logicalDevice->destroyDescriptorPool(m_descriptorPool);
  }

  void Renderer2D::createNewFrame()
  {
    m_currentZ = 0.01f;

    fill(255);

    resetMatrix();

    rectMode(RectMode::CORNER);

    ellipseMode(EllipseMode::CENTER);

    imageMode(ImageMode::CORNER);

    textAlign(TextAlignH::LEFT, TextAlignV::BASELINE);

    m_drawList.clear();

    m_pointInstances.clear();

    m_glyph3DInstances.clear();

    m_mousePicker->clearObjectsToMousePick();
  }

  void Renderer2D::render(const std::shared_ptr<PipelineManager>& pipelineManager,
                          const RenderInfo* renderInfo)
  {
    normalizeZValues();

    const Camera3DUBO cameraUBO {
      .mvp = m_projectionMatrix * m_viewMatrix,
      .camRight = m_camRight,
      ._pad0 = 0.f,
      .camUp = m_camUp,
      ._pad1 = 0.f
    };
    m_cameraUniform->update(renderInfo->currentFrame, &cameraUBO);

    // Upload all instance data for this frame upfront so markers can draw sub-ranges.
    if (!m_pointInstances.empty())
    {
      const VkDeviceSize requiredBytes = sizeof(PointInstance) * m_pointInstances.size();
      ensureInstanceBuffer(m_pointInstanceBuffers, m_pointInstanceMemory,
                           m_pointInstanceBufferCapacity, requiredBytes);

      void* mapped = nullptr;
      m_logicalDevice->mapMemory(m_pointInstanceMemory[renderInfo->currentFrame],
                                 0, requiredBytes, 0, &mapped);
      std::memcpy(mapped, m_pointInstances.data(), requiredBytes);
      m_logicalDevice->unmapMemory(m_pointInstanceMemory[renderInfo->currentFrame]);
    }

    if (!m_glyph3DInstances.empty())
    {
      const VkDeviceSize requiredBytes = sizeof(Glyph3DInstance) * m_glyph3DInstances.size();
      ensureInstanceBuffer(m_glyph3DInstanceBuffers, m_glyph3DInstanceMemory,
                           m_glyph3DInstanceBufferCapacity, requiredBytes);

      void* mapped = nullptr;
      m_logicalDevice->mapMemory(m_glyph3DInstanceMemory[renderInfo->currentFrame],
                                 0, requiredBytes, 0, &mapped);
      std::memcpy(mapped, m_glyph3DInstances.data(), requiredBytes);
      m_logicalDevice->unmapMemory(m_glyph3DInstanceMemory[renderInfo->currentFrame]);
    }

    PipelineType currentPipeline = PipelineType::rect;
    bool firstDraw = true;

    auto clearDepthBuffer = [&]
    {
      constexpr VkClearAttachment clearAttachment{
        .aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT,
        .clearValue = VkClearValue{ {1.0f, 0} }
      };

      const VkClearRect clearRect{
        .rect = {
          .offset = { 0, 0 },
          .extent = renderInfo->extent
        },
        .baseArrayLayer = 0,
        .layerCount = 1
      };

      renderInfo->commandBuffer->clearAttachments({ clearAttachment }, { clearRect });
    };

    auto bindIfNeeded = [&](PipelineType type)
    {
      constexpr static std::array pipelines3D {
        PipelineType::point,
        PipelineType::font3D
      };

      const bool was3D = std::ranges::contains(pipelines3D, currentPipeline);
      const bool now3D = std::ranges::contains(pipelines3D, type);

      if (was3D != now3D)
      {
        clearDepthBuffer();
      }

      if (firstDraw || currentPipeline != type)
      {
        pipelineManager->bindGraphicsPipeline(renderInfo->commandBuffer, type);
        currentPipeline = type;
        firstDraw = false;

        if (type != PipelineType::font3D)
        {
          currentGlyph3DFontSet = VK_NULL_HANDLE;
          currentGlyph3DCameraSet = VK_NULL_HANDLE;
        }

        if (type != PipelineType::point)
        {
          currentPointCameraSet = VK_NULL_HANDLE;
        }

        if (type != PipelineType::font)
        {
          currentGlyphFontSet = VK_NULL_HANDLE;
        }
      }
    };

    for (const auto& entry : m_drawList)
    {
      std::visit([&](const auto& cmd) {
        using T = std::decay_t<decltype(cmd)>;

        if constexpr (std::is_same_v<T, Rect>)
        {
          bindIfNeeded(PipelineType::rect);
          renderRect(pipelineManager, renderInfo, cmd);
        }
        else if constexpr (std::is_same_v<T, Triangle>)
        {
          bindIfNeeded(PipelineType::triangle);
          renderTriangle(pipelineManager, renderInfo, cmd);
        }
        else if constexpr (std::is_same_v<T, Ellipse>)
        {
          bindIfNeeded(PipelineType::ellipse);
          renderEllipse(pipelineManager, renderInfo, cmd);
        }
        else if constexpr (std::is_same_v<T, GlyphCommand>)
        {
          bindIfNeeded(PipelineType::font);
          renderGlyph(pipelineManager, renderInfo, cmd);
        }
        else if constexpr (std::is_same_v<T, Image>)
        {
          bindIfNeeded(PipelineType::image);
          renderImage(pipelineManager, renderInfo, cmd);
        }
        else if constexpr (std::is_same_v<T, PointBatchMarker>)
        {
          bindIfNeeded(PipelineType::point);
          renderPointBatch(pipelineManager, renderInfo, cmd);
        }
        else if constexpr (std::is_same_v<T, Glyph3DBatchMarker>)
        {
          bindIfNeeded(PipelineType::font3D);
          renderGlyph3DBatch(pipelineManager, renderInfo, cmd);
        }
        else if constexpr (std::is_same_v<T, Camera>)
        {
          renderCamera(pipelineManager, renderInfo, cmd);
        }
      }, entry.command);
    }
  }

  std::shared_ptr<MousePicker> Renderer2D::getMousePicker()
  {
    return m_mousePicker;
  }

  void Renderer2D::renderMousePicking(const std::shared_ptr<PipelineManager>& pipelineManager,
                                      const RenderInfo* renderInfo) const
  {
    const RenderInfo renderInfoMousePicking {
      .commandBuffer = renderInfo->commandBuffer,
      .currentFrame = renderInfo->currentFrame,
      .extent = renderInfo->extent
    };

    m_mousePicker->render(pipelineManager, &renderInfoMousePicking);
  }

  void Renderer2D::handleRenderedMousePickingImage(const VkImage image) const
  {
    m_mousePicker->handleRenderedMousePickingImage(image);
  }

  void Renderer2D::fill(const float r,
                        const float g,
                        const float b,
                        const float a)
  {
    m_currentFill = glm::vec4(
      r / 255.0f,
      g / 255.0f,
      b / 255.0f,
      a / 255.0f
    );
  }

  void Renderer2D::fill(const float rgb,
                        const float a)
  {
    float rgbScaled = rgb / 255.0f;

    m_currentFill = glm::vec4(
      rgbScaled,
      rgbScaled,
      rgbScaled,
      a / 255.0f
    );
  }

  void Renderer2D::rotate(float angle)
  {
    m_currentTransform *= glm::rotate(glm::mat4(1.0), glm::radians(angle), {0.0f, 0.0f, 1.0f});
  }

  void Renderer2D::translate(float x,
                             float y)
  {
    m_currentTransform *= glm::translate(glm::mat4(1.0), {x, y, 0});
  }

  void Renderer2D::scale(float xy)
  {
    m_currentTransform *= glm::scale(glm::mat4(1.0), glm::vec3(xy));
  }

  void Renderer2D::scale(float x,
                         float y)
  {
    m_currentTransform *= glm::scale(glm::mat4(1.0), {x, y, 1});
  }

  void Renderer2D::pushMatrix()
  {
    m_transformStack.push_back(m_currentTransform);
  }

  void Renderer2D::popMatrix()
  {
    if (m_transformStack.empty())
    {
      return;
    }

    m_currentTransform = m_transformStack.back();
    m_transformStack.pop_back();
  }

  void Renderer2D::resetMatrix()
  {
    m_transformStack.clear();

    m_currentTransform = glm::mat4(1.0f);
  }

  void Renderer2D::rectMode(const RectMode mode)
  {
    m_rectMode = mode;
  }

  void Renderer2D::ellipseMode(const EllipseMode mode)
  {
    m_ellipseMode = mode;
  }

  void Renderer2D::imageMode(const ImageMode mode)
  {
    m_imageMode = mode;
  }

  void Renderer2D::rect(float x,
                        float y,
                        float width,
                        float height,
                        float radius)
  {
    const auto bounds = resolveRectBounds(x, y, width, height);

    m_drawList.push_back({
      Rect{
        .bounds = bounds,
        .color = m_currentFill,
        .transform = m_currentTransform,
        .z = m_currentZ,
        .radius = radius
      },
      m_currentZ
    });

    increaseCurrentZ();
  }

  void Renderer2D::triangle(const float x1,
                            const float y1,
                            const float x2,
                            const float y2,
                            const float x3,
                            const float y3)
  {
    m_drawList.push_back({
      Triangle{
        .p1 = glm::vec2(x1, y1),
        .p2 = glm::vec2(x2, y2),
        .p3 = glm::vec2(x3, y3),
        .color = m_currentFill,
        .transform = m_currentTransform,
        .z = m_currentZ
      },
      m_currentZ
    });

    increaseCurrentZ();
  }

  void Renderer2D::ellipse(const float x,
                           const float y,
                           const float width,
                           const float height)
  {
    const auto bounds = resolveEllipseBounds(x, y, width, height);

    m_drawList.push_back({
      Ellipse{
        .bounds = bounds,
        .color = m_currentFill,
        .transform = m_currentTransform,
        .z = m_currentZ
      },
      m_currentZ
    });

    increaseCurrentZ();
  }

  void Renderer2D::textFont(const std::string &font)
  {
    m_currentFontName = font;

    updateCurrentFont();
  }

  void Renderer2D::textFont(const std::string &font, uint32_t size)
  {
    m_currentFontName = font;
    m_currentFontSize = size;

    updateCurrentFont();
  }

  void Renderer2D::textSize(uint32_t size)
  {
    m_currentFontSize = size;

    updateCurrentFont();
  }

  void Renderer2D::textAlign(const TextAlignH h,
                             const TextAlignV v)
  {
    m_textAlignH = h;
    m_textAlignV = v;
  }

  float Renderer2D::textWidth(const std::vector<uint32_t>& codepoints) const
  {
    if (!m_currentFont)
    {
      return 0.0f;
    }

    float width = 0.0f;
    for (const auto codepoint : codepoints)
    {
      if (const auto glyphInfo = m_currentFont->getGlyphInfo(codepoint))
      {
        width += glyphInfo->advance;
      }
    }

    return width;
  }

  float Renderer2D::textAscent(const std::vector<uint32_t>& codepoints) const
  {
    if (!m_currentFont)
    {
      return 0.0f;
    }

    float maxAscent = 0.0f;
    for (const auto codepoint : codepoints)
    {
      if (const auto glyphInfo = m_currentFont->getGlyphInfo(codepoint))
      {
        maxAscent = std::max(maxAscent, glyphInfo->bearingY);
      }
    }

    return maxAscent;
  }

  float Renderer2D::textDescent(const std::vector<uint32_t>& codepoints) const
  {
    if (!m_currentFont)
    {
      return 0.0f;
    }

    float maxDescent = 0.0f;
    for (const auto codepoint : codepoints)
    {
      if (const auto glyphInfo = m_currentFont->getGlyphInfo(codepoint))
      {
        const float descent = glyphInfo->height - glyphInfo->bearingY;
        maxDescent = std::max(maxDescent, descent);
      }
    }

    return maxDescent;
  }

  void Renderer2D::text(const std::string& text,
                        float x,
                        float y)
  {
    const auto codepoints = decodeUTF8(text);

    float xOffset = 0.0f;
    if (m_textAlignH == TextAlignH::CENTER || m_textAlignH == TextAlignH::RIGHT)
    {
      const float stringWidth = textWidth(codepoints);
      xOffset = (m_textAlignH == TextAlignH::CENTER) ? -stringWidth * 0.5f : -stringWidth;
    }

    float yOffset = 0.0f;
    const float ascent  = textAscent(codepoints);
    const float descent = textDescent(codepoints);
    const float height  = ascent + descent;

    switch (m_textAlignV)
    {
      case TextAlignV::BASELINE:
        yOffset = 0.0f;
        break;

      case TextAlignV::TOP:
        yOffset = ascent;
        break;

      case TextAlignV::CENTER:
        yOffset = ascent - height * 0.5f;
        break;

      case TextAlignV::BOTTOM:
        yOffset = -descent;
        break;
    }

    float currentX = x + xOffset;
    const float adjustedY = y + yOffset;

    // All glyphs in a text() call share the same Z so they sort together
    const float textZ = m_currentZ;

    for (const auto codepoint : decodeUTF8(text))
    {
      if (const auto glyphInfo = m_currentFont->getGlyphInfo(codepoint))
      {
        m_drawList.push_back({
          GlyphCommand{
            .glyph = {
              .bounds = glm::vec4(
                currentX + glyphInfo->bearingX,
                adjustedY - glyphInfo->bearingY,
                glyphInfo->width,
                glyphInfo->height
              ),
              .color = m_currentFill,
              .transform = m_currentTransform,
              .uv = glm::vec4(
                glyphInfo->u0,
                glyphInfo->v0,
                glyphInfo->u1,
                glyphInfo->v1
              ),
              .z = textZ
            },
            .fontName = m_currentFontName,
            .fontSize = m_currentFontSize
          },
          textZ
        });

        currentX += glyphInfo->advance;
      }
    }

    increaseCurrentZ();
  }

  void Renderer2D::image(std::string image,
                         float x,
                         float y,
                         float width,
                         float height)
  {
    const auto bounds = resolveImageBounds(x, y, width, height);

    m_drawList.push_back({
      Image{
        .imageName = std::move(image),
        .bounds = bounds,
        .transform = m_currentTransform,
        .z = m_currentZ
      },
      m_currentZ
    });

    increaseCurrentZ();
  }

  std::shared_ptr<AssetManager> Renderer2D::getAssetManager() const
  {
    return m_assetManager;
  }

  void Renderer2D::camera(float x,
                          float y,
                          float width,
                          float height)
  {
    const auto bounds = resolveImageBounds(x, y, width, height);

    m_drawList.push_back({
      Camera{
        .bounds = bounds,
        .transform = m_currentTransform,
        .z = m_currentZ
      },
      m_currentZ
    });

    increaseCurrentZ();
  }

  void Renderer2D::point(float x,
                         float y,
                         float z,
                         float size)
  {
    const auto firstInstance = static_cast<uint32_t>(m_pointInstances.size());

    m_pointInstances.push_back({
      .worldPos = { x, y, z },
      .size = size,
      .color = m_currentFill
    });

    if (firstInstance != 0)
    {
      return;
    }

    m_drawList.push_back({
      PointBatchMarker{
        .firstInstance = firstInstance,
        .instanceCount = 1
      },
      m_currentZ
    });

    increaseCurrentZ();
  }

  void Renderer2D::mousePickingPoint(float x,
                                     float y,
                                     float z,
                                     float size,
                                     uint32_t id)
  {
    m_mousePicker->addObjectToMousePick(MousePickingPoint{
      .viewMatrix = m_viewMatrix,
      .projMatrix = m_projectionMatrix,
      .x = x,
      .y = y,
      .z = z,
      .size = size,
      .id = id
    });
  }

  void Renderer2D::text3D(const std::string& text,
                          float x,
                          float y,
                          float z)
  {
    const auto codepoints = decodeUTF8(text);

    float xOffset = 0.0f;
    if (m_textAlignH == TextAlignH::CENTER || m_textAlignH == TextAlignH::RIGHT)
    {
      const float stringWidth = textWidth(codepoints);
      xOffset = (m_textAlignH == TextAlignH::CENTER) ? -stringWidth * 0.5f : -stringWidth;
    }

    float yOffset = 0.0f;
    const float ascent  = textAscent(codepoints);
    const float descent = textDescent(codepoints);
    const float height  = ascent + descent;

    switch (m_textAlignV)
    {
      case TextAlignV::BASELINE:
        yOffset = 0.0f;
        break;

      case TextAlignV::TOP:
        yOffset = ascent;
        break;

      case TextAlignV::CENTER:
        yOffset = ascent - height * 0.5f;
        break;

      case TextAlignV::BOTTOM:
        yOffset = -descent;
        break;
    }

    float currentX = x + xOffset;
    const float adjustedY = y + yOffset;

    // All glyphs in a text3D() call share the same Z so they sort together
    const float textZ = m_currentZ;

    const auto firstInstance = static_cast<uint32_t>(m_glyph3DInstances.size());
    uint32_t instanceCount = 0;

    for (const auto codepoint : decodeUTF8(text))
    {
      if (const auto glyphInfo = m_currentFont->getGlyphInfo(codepoint))
      {
        float gx = currentX + glyphInfo->bearingX;
        float gy = adjustedY - glyphInfo->bearingY;

        m_glyph3DInstances.push_back({
          .worldPos = { x, y, z },
          .width = glyphInfo->width,
          .glyphOffsetX = gx - x + glyphInfo->width * 0.5f,
          .glyphOffsetY = gy - y + glyphInfo->height * 0.5f,
          .height = glyphInfo->height,
          ._pad = 0.f,
          .uv = { glyphInfo->u0, glyphInfo->v0, glyphInfo->u1, glyphInfo->v1 },
          .color = m_currentFill
        });

        ++instanceCount;
        currentX += glyphInfo->advance;
      }
    }

    if (instanceCount > 0 && firstInstance == 0)
    {
      m_drawList.push_back({
        Glyph3DBatchMarker{
          .firstInstance = firstInstance,
          .instanceCount = instanceCount,
          .fontName = m_currentFontName,
          .fontSize = m_currentFontSize
        },
        textZ
      });
    }

    increaseCurrentZ();
  }

  void Renderer2D::set3DView(float x,
                             float y,
                             float z,
                             float pitch,
                             float yaw,
                             float roll,
                             float screenWidth,
                             float screenHeight)
  {
    const glm::vec3 position { x, y, z };

    glm::vec3 direction = normalize(glm::vec3(
      std::cos(glm::radians(yaw)) * std::cos(glm::radians(pitch)),
      std::sin(glm::radians(pitch)),
      std::sin(glm::radians(yaw)) * std::cos(glm::radians(pitch))
    ));

    constexpr auto UP = glm::vec3(0.0f, 1.0f, 0.0f);

    m_viewMatrix = glm::lookAt(
      position,
      position + direction,
      UP
    );

    m_projectionMatrix = glm::perspective(
      glm::radians(50.0f),
      screenWidth / screenHeight,
      500.0f,
      20000.0f
    );

    m_projectionMatrix[1][1] *= -1;

    const glm::mat4 invView = glm::inverse(m_viewMatrix);

    m_camRight = glm::vec3(invView[0]);
    m_camUp = glm::vec3(invView[1]);
  }

  void Renderer2D::createCommandPool()
  {
    const VkCommandPoolCreateInfo poolInfo {
      .sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO,
      .queueFamilyIndex = m_logicalDevice->getPhysicalDevice()->getQueueFamilies().graphicsFamily.value()
    };

    m_commandPool = m_logicalDevice->createCommandPool(poolInfo);
  }

  void Renderer2D::ensureInstanceBuffer(std::vector<VkBuffer>& buffers,
                                        std::vector<VkDeviceMemory>& memory,
                                        VkDeviceSize& capacity,
                                        VkDeviceSize requiredBytes)
  {
    if (requiredBytes <= capacity)
    {
      return;
    }

    destroyInstanceBuffers(buffers, memory);

    VkDeviceSize newCapacity = capacity == 0 ? 64 : capacity;
    while (newCapacity < requiredBytes)
    {
      newCapacity *= 2;
    }

    const uint32_t frames = static_cast<uint32_t>(buffers.size());

    for (uint32_t i = 0; i < frames; ++i)
    {
      const VkBufferCreateInfo bufferInfo {
        .sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO,
        .size = newCapacity,
        .usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
        .sharingMode = VK_SHARING_MODE_EXCLUSIVE
      };

      buffers[i] = m_logicalDevice->createBuffer(bufferInfo);

      const VkMemoryRequirements memReqs = m_logicalDevice->getBufferMemoryRequirements(buffers[i]);

      const VkMemoryAllocateInfo allocInfo {
        .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
        .allocationSize = memReqs.size,
        .memoryTypeIndex = m_logicalDevice->getPhysicalDevice()->findMemoryType(
          memReqs.memoryTypeBits,
          VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        )
      };

      m_logicalDevice->allocateMemory(allocInfo, memory[i]);

      m_logicalDevice->bindBufferMemory(buffers[i], memory[i], 0);
    }

    capacity = newCapacity;
  }

  void Renderer2D::destroyInstanceBuffers(std::vector<VkBuffer>& buffers,
                                          std::vector<VkDeviceMemory>& memory)
  {
    for (size_t i = 0; i < buffers.size(); ++i)
    {
      if (buffers[i] != VK_NULL_HANDLE)
      {
        m_logicalDevice->destroyBuffer(buffers[i]);
        buffers[i] = VK_NULL_HANDLE;
      }

      if (memory[i] != VK_NULL_HANDLE)
      {
        m_logicalDevice->freeMemory(memory[i]);
        memory[i] = VK_NULL_HANDLE;
      }
    }
  }

  glm::vec4 Renderer2D::resolveRectBounds(float a,
                                          float b,
                                          float c,
                                          float d)
  {
    switch(m_rectMode)
    {
      case RectMode::CORNER:
        return { a, b, c, d };

      case RectMode::CORNERS:
        return { a, b, c - a, d - b };

      case RectMode::CENTER:
        return { a - c * 0.5f, b - d * 0.5f, c, d };

      case RectMode::RADIUS:
        return { a - c, b - d, c * 2.0f, d * 2.0f };
    }

    return { a, b, c, d };
  }

  glm::vec4 Renderer2D::resolveEllipseBounds(float a,
                                             float b,
                                             float c,
                                             float d)
  {
    switch(m_ellipseMode)
    {
      case EllipseMode::CENTER:
        return { a, b, c, d };

      case EllipseMode::RADIUS:
        return { a, b, c * 2.0f, d * 2.0f };

      case EllipseMode::CORNER:
        return { a + c * 0.5f, b + d * 0.5f, c, d };

      case EllipseMode::CORNERS:
        const float w = c - a;
        const float h = d - b;
        return { a + w * 0.5f, b + h * 0.5f, w, h };
    }

    return { a, b, c, d };
  }

  glm::vec4 Renderer2D::resolveImageBounds(float a,
                                           float b,
                                           float c,
                                           float d)
  {
    switch(m_imageMode)
    {
      case ImageMode::CORNER:
        return { a, b, c, d };

      case ImageMode::CORNERS:
        return { a, b, c - a, d - b };

      case ImageMode::CENTER:
        return { a - c * 0.5f, b - d * 0.5f, c, d };
    }

    return { a, b, c, d };
  }

  void Renderer2D::updateCurrentFont()
  {
    m_currentFont = m_assetManager->getFont(m_currentFontName, m_currentFontSize);
  }

  void Renderer2D::increaseCurrentZ()
  {
    m_currentZ += 1.0f;
  }

  void Renderer2D::normalizeZValues()
  {
    for (auto& entry : m_drawList)
    {
      float normalized = entry.z / m_currentZ;
      float flipped = 1.0f - normalized;

      entry.z = flipped;

      std::visit([flipped](auto& cmd) {
        using T = std::decay_t<decltype(cmd)>;

        if constexpr (std::is_same_v<T, GlyphCommand>)
        {
          cmd.glyph.z = flipped;
        }
        else if constexpr (!std::is_same_v<T, PointBatchMarker> &&
                           !std::is_same_v<T, Glyph3DBatchMarker>)
        {
          cmd.z = flipped;
        }
      }, entry.command);
    }
  }

  void Renderer2D::renderRect(const std::shared_ptr<PipelineManager>& pipelineManager,
                              const RenderInfo* renderInfo,
                              const Rect& rect)
  {
    const auto rectPC = rect.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::rect,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(rectPC),
      &rectPC
    );

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }

  void Renderer2D::renderTriangle(const std::shared_ptr<PipelineManager>& pipelineManager,
                                  const RenderInfo* renderInfo,
                                  const Triangle& triangle)
  {
    const auto trianglePC = triangle.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::triangle,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(trianglePC),
      &trianglePC
    );

    renderInfo->commandBuffer->draw(3, 1, 0, 0);
  }

  void Renderer2D::renderEllipse(const std::shared_ptr<PipelineManager>& pipelineManager,
                                 const RenderInfo* renderInfo,
                                 const Ellipse& ellipse)
  {
    const auto ellipsePC = ellipse.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::ellipse,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(ellipsePC),
      &ellipsePC
    );

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }

  void Renderer2D::renderGlyph(const std::shared_ptr<PipelineManager>& pipelineManager,
                               const RenderInfo* renderInfo,
                               const GlyphCommand& glyphCmd)
  {
    const auto fontSet = m_assetManager->getFont(glyphCmd.fontName, glyphCmd.fontSize)->getDescriptorSet(renderInfo->currentFrame);

    if (fontSet != currentGlyphFontSet)
    {
      pipelineManager->bindGraphicsPipelineDescriptorSet(
        renderInfo->commandBuffer,
        PipelineType::font,
        fontSet,
        0
      );

      currentGlyphFontSet = fontSet;
    }

    const auto glyphPC = glyphCmd.glyph.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::font,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(glyphPC),
      &glyphPC
    );

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }

  void Renderer2D::renderImage(const std::shared_ptr<PipelineManager>& pipelineManager,
                               const RenderInfo* renderInfo,
                               const Image& image) const
  {
    auto imageTexture = m_assetManager->getImage(image.imageName);

    pipelineManager->bindGraphicsPipelineDescriptorSet(
      renderInfo->commandBuffer,
      PipelineType::image,
      imageTexture->getDescriptorSet(renderInfo->currentFrame),
      0
    );

    const auto imagePC = image.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::image,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(imagePC),
      &imagePC
    );

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }

  void Renderer2D::renderPointBatch(const std::shared_ptr<PipelineManager>& pipelineManager,
                                    const RenderInfo* renderInfo,
                                    const PointBatchMarker& marker)
  {
    const VkDescriptorSet cameraSet =
      m_glyph3DDescriptorSet->getDescriptorSet(renderInfo->currentFrame);

    if (cameraSet != currentPointCameraSet)
    {
      pipelineManager->bindGraphicsPipelineDescriptorSet(
        renderInfo->commandBuffer,
        PipelineType::point,
        cameraSet,
        0
      );

      currentPointCameraSet = cameraSet;
    }

    const VkBuffer buf = m_pointInstanceBuffers[renderInfo->currentFrame];
    const VkDeviceSize offset = 0;
    renderInfo->commandBuffer->bindVertexBuffers(0, 1, &buf, &offset);

    renderInfo->commandBuffer->draw(4, m_pointInstances.size(), 0, 0);
  }

  void Renderer2D::renderGlyph3DBatch(const std::shared_ptr<PipelineManager>& pipelineManager,
                                      const RenderInfo* renderInfo,
                                      const Glyph3DBatchMarker& marker)
  {
    const VkDescriptorSet cameraSet =
      m_glyph3DDescriptorSet->getDescriptorSet(renderInfo->currentFrame);

    if (cameraSet != currentGlyph3DCameraSet)
    {
      pipelineManager->bindGraphicsPipelineDescriptorSet(
        renderInfo->commandBuffer,
        PipelineType::font3D,
        cameraSet,
        1
      );

      currentGlyph3DCameraSet = cameraSet;
    }

    const VkDescriptorSet fontSet =
      m_assetManager
        ->getFont(marker.fontName, marker.fontSize)
        ->getDescriptorSet(renderInfo->currentFrame);

    if (fontSet != currentGlyph3DFontSet)
    {
      pipelineManager->bindGraphicsPipelineDescriptorSet(
        renderInfo->commandBuffer,
        PipelineType::font3D,
        fontSet,
        0
      );

      currentGlyph3DFontSet = fontSet;
    }

    const VkBuffer buf = m_glyph3DInstanceBuffers[renderInfo->currentFrame];
    const VkDeviceSize offset = 0;
    renderInfo->commandBuffer->bindVertexBuffers(0, 1, &buf, &offset);

    renderInfo->commandBuffer->draw(4, m_glyph3DInstances.size(), 0, 0);
  }

  void Renderer2D::renderCamera(const std::shared_ptr<PipelineManager>& pipelineManager,
                                const RenderInfo* renderInfo,
                                const Camera& camera) const
  {
    const auto cameraTexture = m_assetManager->getCameraTexture();

    if (!cameraTexture ||
        cameraTexture->getDescriptorSet(renderInfo->currentFrame) == VK_NULL_HANDLE)
    {
      return;
    }

    cameraTexture->flushDescriptorUpdate(renderInfo->currentFrame);

    if (!pipelineManager->hasCameraPipeline())
    {
      pipelineManager->createCameraPipeline(cameraTexture->getDescriptorSetLayout());
    }

    pipelineManager->bindGraphicsPipeline(renderInfo->commandBuffer, PipelineType::camera);

    pipelineManager->bindGraphicsPipelineDescriptorSet(
      renderInfo->commandBuffer,
      PipelineType::camera,
      cameraTexture->getDescriptorSet(renderInfo->currentFrame),
      0
    );

    const auto cameraPC = camera.createPushConstant(renderInfo->extent);

    pipelineManager->pushGraphicsPipelineConstants(
      renderInfo->commandBuffer,
      PipelineType::camera,
      VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
      0,
      sizeof(cameraPC),
      &cameraPC
    );

    renderInfo->commandBuffer->draw(4, 1, 0, 0);
  }

} // ge