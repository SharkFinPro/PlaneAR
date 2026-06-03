#ifndef PLANEAR_PRIMITIVES2D_H
#define PLANEAR_PRIMITIVES2D_H

#include <glm/mat4x4.hpp>
#include <vulkan/vulkan.h>

namespace ge {

  struct Rect {
    glm::vec4 bounds;
    glm::vec4 color;
    glm::mat4 transform;
    float z;
    float radius;

    struct PushConstant {
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
      float radius;
    };

    [[nodiscard]] PushConstant createPushConstant(const VkExtent2D extent) const
    {
      return {
        .transform = transform,
        .screenWidth = static_cast<int>(extent.width),
        .screenHeight = static_cast<int>(extent.height),
        .z = z,
        .x = bounds.x,
        .y = bounds.y,
        .width = bounds.z,
        .height = bounds.w,
        .r = color.r,
        .g = color.g,
        .b = color.b,
        .a = color.a,
        .radius = radius
      };
    }
  };

  struct Triangle {
    glm::vec2 p1;
    glm::vec2 p2;
    glm::vec2 p3;
    glm::vec4 color;
    glm::mat4 transform;
    float z;

    struct PushConstant {
      float r;
      float g;
      float b;
      float a;
      glm::mat4 transform;
      int screenWidth;
      int screenHeight;
      float z;
      float x1;
      float y1;
      float x2;
      float y2;
      float x3;
      float y3;
    };

    [[nodiscard]] PushConstant createPushConstant(const VkExtent2D extent) const
    {
      return {
        .r = color.r,
        .g = color.g,
        .b = color.b,
        .a = color.a,
        .transform = transform,
        .screenWidth = static_cast<int>(extent.width),
        .screenHeight = static_cast<int>(extent.height),
        .z = z,
        .x1 = p1.x,
        .y1 = p1.y,
        .x2 = p2.x,
        .y2 = p2.y,
        .x3 = p3.x,
        .y3 = p3.y
      };
    }
  };

  struct Ellipse {
    glm::vec4 bounds;
    glm::vec4 color;
    glm::mat4 transform;
    float z;

    struct PushConstant {
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

    [[nodiscard]] PushConstant createPushConstant(const VkExtent2D extent) const
    {
      return {
        .transform = transform,
        .screenWidth = static_cast<int>(extent.width),
        .screenHeight = static_cast<int>(extent.height),
        .z = z,
        .x = bounds.x,
        .y = bounds.y,
        .width = bounds.z,
        .height = bounds.w,
        .r = color.r,
        .g = color.g,
        .b = color.b,
        .a = color.a
      };
    }
  };

  struct Glyph {
    glm::vec4 bounds;
    glm::vec4 color;
    glm::mat4 transform;
    glm::vec4 uv;
    float z;

    struct PushConstant {
      glm::mat4 transform;
      int screenWidth;
      int screenHeight;
      float z;
      float x;
      float y;
      float width;
      float height;
      float u0, v0;
      float u1, v1;
      float r;
      float g;
      float b;
      float a;
    };

    [[nodiscard]] PushConstant createPushConstant(const VkExtent2D extent) const
    {
      return {
        .transform = transform,
        .screenWidth = static_cast<int>(extent.width),
        .screenHeight = static_cast<int>(extent.height),
        .z = z,
        .x = bounds.x,
        .y = bounds.y,
        .width = bounds.z,
        .height = bounds.w,
        .u0 = uv.x,
        .v0 = uv.y,
        .u1 = uv.z,
        .v1 = uv.w,
        .r = color.r,
        .g = color.g,
        .b = color.b,
        .a = color.a
      };
    }
  };

  struct Image {
    std::string imageName;
    glm::vec4 bounds;
    glm::mat4 transform;
    float z;

    struct PushConstant {
      glm::mat4 transform;
      int screenWidth;
      int screenHeight;
      float z;
      float x;
      float y;
      float width;
      float height;
    };

    [[nodiscard]] PushConstant createPushConstant(const VkExtent2D extent) const
    {
      return {
        .transform = transform,
        .screenWidth = static_cast<int>(extent.width),
        .screenHeight = static_cast<int>(extent.height),
        .z = z,
        .x = bounds.x,
        .y = bounds.y,
        .width = bounds.z,
        .height = bounds.w
      };
    }
  };

  struct Camera {
    glm::vec4 bounds;
    glm::mat4 transform;
    float z;

    struct PushConstant {
      glm::mat4 transform;
      int screenWidth;
      int screenHeight;
      float z;
      float x;
      float y;
      float width;
      float height;
    };

    [[nodiscard]] PushConstant createPushConstant(const VkExtent2D extent) const
    {
      return {
        .transform = transform,
        .screenWidth = static_cast<int>(extent.width),
        .screenHeight = static_cast<int>(extent.height),
        .z = z,
        .x = bounds.x,
        .y = bounds.y,
        .width = bounds.z,
        .height = bounds.w
      };
    }
  };

  struct PointInstance {
    glm::vec3 worldPos;
    float size;
    glm::vec4 color;
  };
    // Aspect multipliers for the billboard quad.  aspectX > 1 makes the card
    // wider than it is tall.  Default 1.0 keeps square behaviour for any other
    // call sites that don't set these explicitly.
    float aspectX = 1.0f;
    float aspectY = 1.0f;

  struct Glyph3DInstance {
    glm::vec3 worldPos;
    float width;
    float glyphOffsetX;
    float glyphOffsetY;
    float height;
    float _pad;
    glm::vec4 uv;
    glm::vec4 color;
  };

  struct Camera3DUBO {
    glm::mat4 mvp;
    glm::vec3 camRight;
    float _pad0;
    glm::vec3 camUp;
    float _pad1;
  };
    struct PushConstant {
      glm::mat4 mvp;
      glm::vec3 worldPos;
      float     size;
      glm::vec3 camRight;
      float     aspectX;   // was _pad0
      glm::vec3 camUp;
      float     aspectY;   // was _pad1
      float r;
      float g;
      float b;
      float a;
    };

  struct Point {
    float x;
    float y;
    float z;
    float size;
    glm::vec4 color;
    [[nodiscard]] PushConstant createPushConstant(const VkExtent2D extent) const
    {
      const glm::mat4 invView = glm::inverse(viewMatrix);

      const glm::vec3 camRight = glm::vec3(invView[0]);
      const glm::vec3 camUp    = glm::vec3(invView[1]);

      return {
        .mvp      = projMatrix * viewMatrix,
        .worldPos = { x, y, z },
        .size     = size,
        .camRight = camRight,
        .aspectX  = aspectX,
        .camUp    = camUp,
        .aspectY  = aspectY,
        .r = color.r,
        .g = color.g,
        .b = color.b,
        .a = color.a
      };
    }
  };

  struct MousePickingPoint {
    glm::mat4 viewMatrix;
    glm::mat4 projMatrix;
    float x;
    float y;
    float z;
    float size;
    uint32_t id;

    struct PushConstant {
      glm::mat4 mvp;
      glm::vec3 worldPos;
      float size;
      glm::vec3 camRight;
      float _pad0;
      glm::vec3 camUp;
      float _pad1;
      uint32_t id;
    };

    [[nodiscard]] PushConstant createPushConstant(const VkExtent2D extent) const
    {
      const glm::mat4 invView = glm::inverse(viewMatrix);

      const glm::vec3 camRight = glm::vec3(invView[0]);
      const glm::vec3 camUp    = glm::vec3(invView[1]);

      return {
        .mvp      = projMatrix * viewMatrix,
        .worldPos = { x, y, z },
        .size     = size,
        .camRight = camRight,
        ._pad0    = 0.f,
        .camUp    = camUp,
        ._pad1    = 0.f,
        .id = id
      };
    }
  };

  struct Glyph3D {
    float x;
    float y;
    float z;
    glm::vec2 glyphOffset;
    float width;
    float height;
    glm::vec4 uv;
    glm::vec4 color;
  };

  // ── Compass ──────────────────────────────────────────────────────────────────
  // A camera-facing (billboard) compass disc drawn entirely in the fragment
  // shader.  It is anchored to a 3-D world position (the aircraft position) and
  // shifted in camera space by (offsetX, offsetY) world units so it appears in
  // the top-right corner of each aircraft billboard.
  //
  // headingRad: clockwise angle from visual north (+Y in UV space) to the
  //   aircraft heading direction.  For "Always North" mode this equals the raw
  //   aircraft heading.  For "User Relative" mode the user's azimuth has already
  //   been subtracted before this value is stored here.
  struct Compass {
    glm::mat4 viewMatrix;
    glm::mat4 projMatrix;
    float x;
    float y;
    float z;
    float size;       // half-size of the compass quad in world units
    float offsetX;    // world-unit shift along camRight to the anchor point
    float offsetY;    // world-unit shift along camUp    to the anchor point
    float headingRad; // heading in radians (mode-adjusted before storage)
    float alpha;      // overall opacity (0–1)

    // Push constant layout — must match compass.vert / compass.frag exactly.
    struct PushConstant {
      glm::mat4 mvp;        // offset   0
      glm::vec3 worldPos;   // offset  64
      float     size;       // offset  76
      glm::vec3 camRight;   // offset  80
      float     offsetX;    // offset  92
      glm::vec3 camUp;      // offset  96
      float     offsetY;    // offset 108
      float     headingRad; // offset 112
      float     alpha;      // offset 116
      float     _pad0;      // offset 120
      float     _pad1;      // offset 124
    };                      // total  128 bytes

    [[nodiscard]] PushConstant createPushConstant(const VkExtent2D /*extent*/) const
    {
      const glm::mat4 invView  = glm::inverse(viewMatrix);
      const glm::vec3 camRight = glm::vec3(invView[0]);
      const glm::vec3 camUp    = glm::vec3(invView[1]);

      return {
        .mvp        = projMatrix * viewMatrix,
        .worldPos   = { x, y, z },
        .size       = size,
        .camRight   = camRight,
        .offsetX    = offsetX,
        .camUp      = camUp,
        .offsetY    = offsetY,
        .headingRad = headingRad,
        .alpha      = alpha,
        ._pad0      = 0.f,
        ._pad1      = 0.f
      };
    }
  };

}

#endif //PLANEAR_PRIMITIVES2D_H
