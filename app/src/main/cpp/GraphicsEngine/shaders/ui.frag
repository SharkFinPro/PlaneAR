#version 450

layout(push_constant) uniform QuadPC {
  layout(offset = 88)
  float r;
  float g;
  float b;
} pc;

layout(location = 0) out vec4 outColor;

void main()
{
  outColor = vec4(pc.r, pc.g, pc.b, 1.0);
}