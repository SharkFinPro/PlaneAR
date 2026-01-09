#version 450

layout(push_constant) uniform RectPC {
  mat4 transformation;
  int screenWidth;
  int screenHeight;
  float z;
  float x;
  float y;
  float width;
  float height;
} pc;

void main()
{
  vec2 corner = vec2(gl_VertexIndex & 1, gl_VertexIndex >> 1);

  vec2 pos = vec2(pc.x, pc.y) + corner * vec2(pc.width, pc.height);

  pos = (pc.transformation * vec4(pos, 0.0, 1.0)).xy;

  vec2 ndc;
  ndc.x = 2.0 * pos.x / float(pc.screenWidth)  - 1.0;
  ndc.y = 2.0 * pos.y / float(pc.screenHeight) - 1.0;

  gl_Position = vec4(ndc, 0.0, 1.0);
}