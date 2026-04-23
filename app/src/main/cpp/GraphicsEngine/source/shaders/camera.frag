#version 450

layout(set = 0, binding = 0) uniform sampler2D image;

layout(location = 0) in vec2 fragUV;

layout(location = 0) out vec4 outColor;

void main()
{
  vec2 rotatedUV = vec2(fragUV.y, 1.0 - fragUV.x);

  outColor = texture(image, rotatedUV);
}