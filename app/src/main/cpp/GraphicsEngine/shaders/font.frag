#version 450

layout(set = 0, binding = 0) uniform sampler2D glyphAtlas;

layout(location = 0) in vec2 fragUV;

layout(location = 0) out vec4 outColor;

const vec3 COLOR = vec3(1.0, 1.0, 1.0);

void main()
{
  float alpha = texture(glyphAtlas, fragUV).r;

  outColor = vec4(COLOR, alpha);
}