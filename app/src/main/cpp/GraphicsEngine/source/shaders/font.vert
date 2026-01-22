#version 450

layout(push_constant) uniform FontPC {
  mat4 transformation;
  int screenWidth;
  int screenHeight;
  float z;
  float x;
  float y;
  float width;
  float height;
  float u0;
  float v0;
  float u1;
  float v1;
} pc;

layout(location = 0) out vec2 fragUV;

void main()
{
  vec2 corner = vec2(gl_VertexIndex & 1, gl_VertexIndex >> 1);

  vec2 pos = vec2(pc.x, pc.y) + corner * vec2(pc.width, pc.height);

  pos = (pc.transformation * vec4(pos, 0.0, 1.0)).xy;

  vec2 ndc;
  ndc.x = 2.0 * pos.x / float(pc.screenWidth)  - 1.0;
  ndc.y = 2.0 * pos.y / float(pc.screenHeight) - 1.0;

  gl_Position = vec4(ndc, pc.z, 1.0);

  fragUV = mix(
    vec2(pc.u0, pc.v0),
    vec2(pc.u1, pc.v1),
    corner
  );
}