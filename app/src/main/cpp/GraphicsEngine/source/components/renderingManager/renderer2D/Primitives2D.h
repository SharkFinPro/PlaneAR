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

  struct Point {
    glm::mat4 viewMatrix;
    glm::mat4 projMatrix;
    float x;
    float y;
    float z;
    float size;

    struct PushConstant {
      glm::mat4 mvp;
      glm::vec3 worldPos;
      float size;
      glm::vec3 camRight;
      float _pad0;
      glm::vec3 camUp;
      float _pad1;
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
      };
    }
  };

  struct Glyph3D {
    glm::mat4 viewMatrix;
    glm::mat4 projMatrix;
    float x;
    float y;
    float z;
    glm::vec2 glyphOffset;
    float width;
    float height;
    glm::vec4 uv;
    glm::vec4 color;

    struct PushConstant {
      glm::mat4 mvp;
      glm::vec3 worldPos;
      float width;
      glm::vec3 camRight;
      float glyphOffsetX;
      glm::vec3 camUp;
      float glyphOffsetY;
      float height;
      float u0, v0;
      float u1, v1;
      float r, g, b, a;
    };

    [[nodiscard]] PushConstant createPushConstant(const VkExtent2D extent) const
    {
      const glm::mat4 invView = glm::inverse(viewMatrix);

      const glm::vec3 camRight = glm::vec3(invView[0]);
      const glm::vec3 camUp = glm::vec3(invView[1]);

      return {
        .mvp = projMatrix * viewMatrix,
        .worldPos = { x, y, z },
        .width = width,
        .camRight = camRight,
        .glyphOffsetX = glyphOffset.x,
        .camUp = camUp,
        .glyphOffsetY = glyphOffset.y,
        .height = height,
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

}

#endif //PLANEAR_PRIMITIVES2D_H
