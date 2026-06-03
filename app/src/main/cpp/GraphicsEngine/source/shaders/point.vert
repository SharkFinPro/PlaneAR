#version 450

layout(location = 0) in vec3 inWorldPos;
layout(location = 1) in float inSize;
layout(location = 2) in vec4 inColor;
layout(location = 3) in vec2 inAspect;

layout(set = 0, binding = 0) uniform Camera {
  mat4 mvp;
  vec3 camRight;
  float _pad0;
  vec3 camUp;
  float _pad1;
} camera;

layout(location = 0) out vec4 fragColor;

// [-1,+1] in billboard face space, passed to frag for SDF work.
layout(location = 1) out vec2 fragUV;

layout(location = 2) out vec2 fragAspect;

void main() {
  vec2 corners[4] = vec2[](
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2(-1.0,  1.0),
    vec2( 1.0,  1.0)
  );

  vec2 offset = corners[gl_VertexIndex] * inSize;
  vec2 uv     = corners[gl_VertexIndex];
  fragUV      = uv;

  // Scale the billboard quad by the per-axis aspect multipliers so the card
  // can be rectangular (aspectX > 1 for a wide card) while the fragment
  // shader still receives a clean [-1,+1] UV for its SDF.
  vec3 worldPos = inWorldPos
    + camera.camRight * (uv.x * inSize * inAspect.x)
    + camera.camUp    * (uv.y * inSize * inAspect.y);

  gl_Position = camera.mvp * vec4(worldPos, 1.0);
  fragColor = inColor;
}
