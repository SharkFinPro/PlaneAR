#version 450

layout(push_constant) uniform pointPC {
  mat4  mvp;
  vec3  worldPos;
  float size;
  vec3  camRight;
  float aspectX;   // half-width  multiplier (default 1.0 = square)
  vec3  camUp;
  float aspectY;   // half-height multiplier (default 1.0 = square)
} pc;

// [-1,+1] in billboard face space, passed to frag for SDF work.
layout(location = 0) out vec2 fragUV;

void main() {
  // Standard quad, same winding as before.
  vec2 corners[4] = vec2[](
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2(-1.0,  1.0),
    vec2( 1.0,  1.0)
  );

  vec2 uv    = corners[gl_VertexIndex];
  fragUV     = uv;

  // Scale the billboard quad by the per-axis aspect multipliers so the card
  // can be rectangular (aspectX > 1 for a wide card) while the fragment
  // shader still receives a clean [-1,+1] UV for its SDF.
  vec3 worldPos = pc.worldPos
    + pc.camRight * (uv.x * pc.size * pc.aspectX)
    + pc.camUp    * (uv.y * pc.size * pc.aspectY);

  gl_Position = pc.mvp * vec4(worldPos, 1.0);
}
