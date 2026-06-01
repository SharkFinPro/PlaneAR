#version 450

layout(push_constant) uniform Glyph3DPC {
  vec3 worldPos;
  float width;
  vec3 camRight;
  float glyphOffsetX;
  vec3 camUp;
  float glyphOffsetY;
  float height;
  float u0;
  float v0;
  float u1;
  float v1;
  float r, g, b, a;
} pc;

layout(set = 1, binding = 0) uniform Camera {
  mat4 mvp;
} camera;

layout(location = 0) out vec2 fragUV;
layout(location = 1) out vec4 fragColor;

void main() {
  vec2 corners[4] = vec2[](
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2(-1.0,  1.0),
    vec2( 1.0,  1.0)
  );

  vec2 corner = corners[gl_VertexIndex];

  // Glyph offset (layout position relative to text anchor) + quad corner expansion
  // corner is [-1,1], so multiply by half width/height to get offset from glyph origin
  vec2 totalOffset = vec2(pc.glyphOffsetX, pc.glyphOffsetY)
                   + vec2(corner.x * pc.width * 0.5, corner.y * pc.height * 0.5);

  // Billboard: position in world space using camera plane vectors
  vec3 finalWorldPos = pc.worldPos
    + pc.camRight * totalOffset.x
    - pc.camUp    * totalOffset.y;

  gl_Position = camera.mvp * vec4(finalWorldPos, 1.0);

  vec2 uvCorner = vec2(gl_VertexIndex & 1, gl_VertexIndex >> 1);
  fragUV = mix(
    vec2(pc.u0, pc.v0),
    vec2(pc.u1, pc.v1),
    uvCorner
  );

  fragColor = vec4(pc.r, pc.g, pc.b, pc.a);
}
