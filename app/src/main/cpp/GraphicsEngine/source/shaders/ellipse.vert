#version 450

layout(push_constant) uniform EllipsePC {
  mat4 transform;
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
} pc;

layout(location = 0) out vec2 fragPos;

void main()
{
  vec2 corner = vec2(gl_VertexIndex & 1, gl_VertexIndex >> 1);

  vec2 pos = vec2(pc.x, pc.y) + (corner * 2.0 - 1.0) * vec2(pc.width, pc.height) * 0.5;


  pos = (pc.transform * vec4(pos, 0.0, 1.0)).xy;

  fragPos = pos;

  vec2 ndc;
  ndc.x = 2.0 * pos.x / float(pc.screenWidth)  - 1.0;
  ndc.y = 2.0 * pos.y / float(pc.screenHeight) - 1.0;

  gl_Position = vec4(ndc, pc.z, 1.0);
}