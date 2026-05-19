#version 450

layout(push_constant) uniform pointPC {
  layout(offset = 112)
  uint objectID;
} pc;

layout(location = 0) out vec4 outColor;

void main() {
  float r = (pc.objectID >> 16) & 0xFF;
  float g = (pc.objectID >> 8) & 0xFF;
  float b = (pc.objectID >> 0) & 0xFF;

  outColor = uvec4(r, g, b, 255);
}
