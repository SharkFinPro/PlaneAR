#version 450

layout(location = 0) in vec3  inWorldPos;
layout(location = 1) in float inWidth;
layout(location = 2) in float inGlyphOffsetX;
layout(location = 3) in float inGlyphOffsetY;
layout(location = 4) in float inHeight;
layout(location = 5) in float _pad;
layout(location = 6) in float inU0;
layout(location = 7) in float inV0;
layout(location = 8) in float inU1;
layout(location = 9) in float inV1;
layout(location = 10) in vec4 inColor;

layout(set = 1, binding = 0) uniform Camera {
  mat4 mvp;
  vec3 camRight;
  float _pad0;
  vec3 camUp;
  float _pad1;
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

  vec2 totalOffset = vec2(inGlyphOffsetX, inGlyphOffsetY)
                   + vec2(corner.x * inWidth * 0.5, corner.y * inHeight * 0.5);

  vec3 finalWorldPos = inWorldPos
    + camera.camRight * totalOffset.x
    - camera.camUp    * totalOffset.y;

  gl_Position = camera.mvp * vec4(finalWorldPos, 1.0);

  vec2 uvCorner = vec2(gl_VertexIndex & 1, gl_VertexIndex >> 1);
  fragUV = mix(
    vec2(inU0, inV0),
    vec2(inU1, inV1),
    uvCorner
  );

  fragColor = inColor;
}
