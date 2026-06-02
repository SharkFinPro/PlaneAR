#version 450

layout(push_constant) uniform pointPC {
  vec3 worldPos;
  float size;
  vec3 camRight;
  float _pad0;
  vec3 camUp;
  float _pad1;
} pc;

layout(set = 0, binding = 0) uniform Camera {
  mat4 mvp;
} camera;

void main() {
  vec2 corners[4] = vec2[](
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2(-1.0,  1.0),
    vec2( 1.0,  1.0)
  );

  vec2 offset = corners[gl_VertexIndex] * pc.size;

  vec3 worldPos = pc.worldPos
    + pc.camRight * offset.x
    + pc.camUp    * offset.y;

  gl_Position = camera.mvp * vec4(worldPos, 1.0);
}