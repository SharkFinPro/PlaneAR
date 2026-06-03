#version 450

layout(push_constant) uniform pointPC {
  mat4 mvp;
  vec3 worldPos;
  float size;
  vec3 camRight;
  float aspectX;
  vec3 camUp;
  float aspectY;
} pc;

void main() {
  vec2 corners[4] = vec2[](
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2(-1.0,  1.0),
    vec2( 1.0,  1.0)
  );

  vec2 uv = corners[gl_VertexIndex];

  vec3 worldPos = pc.worldPos
    + pc.camRight * (uv.x * pc.size * pc.aspectX)
    + pc.camUp    * (uv.y * pc.size * pc.aspectY);

  gl_Position = pc.mvp * vec4(worldPos, 1.0);
}