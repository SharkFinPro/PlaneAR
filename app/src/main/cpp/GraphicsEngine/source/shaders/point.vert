#version 450

layout(location = 0) in vec3 inWorldPos;
layout(location = 1) in float inSize;
layout(location = 2) in vec4 inColor;

layout(set = 0, binding = 0) uniform Camera {
  mat4 mvp;
  vec3 camRight;
  float _pad0;
  vec3 camUp;
  float _pad1;
} camera;

layout(location = 0) out vec4 fragColor;

void main() {
  vec2 corners[4] = vec2[](
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2(-1.0,  1.0),
    vec2( 1.0,  1.0)
  );

  vec2 offset = corners[gl_VertexIndex] * inSize;

  vec3 worldPos = inWorldPos
    + camera.camRight * offset.x
    + camera.camUp    * offset.y;

  gl_Position = camera.mvp * vec4(worldPos, 1.0);
  fragColor = inColor;
}
