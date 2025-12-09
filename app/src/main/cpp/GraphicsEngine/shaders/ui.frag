#version 450

layout(push_constant) uniform QuadPC {
  layout(offset = 32) vec3 color;
} pc;

layout(location = 0) out vec4 outColor;

void main()
{
  outColor = vec4(pc.color, 1.0);
}