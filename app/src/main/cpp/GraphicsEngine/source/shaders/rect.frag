#version 450

layout(push_constant) uniform RectPC {
  layout(offset = 84)
  float width;
  float height;
  float r;
  float g;
  float b;
  float a;
  float cornerRadius;
} pc;

layout(location = 0) in vec2 fragLocalPos;

layout(location = 0) out vec4 outColor;

float roundedRectSDF(vec2 pos, vec2 halfSize, float radius)
{
  vec2 d = abs(pos) - halfSize + vec2(radius);
  return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - radius;
}

void main()
{
  vec2 halfSize = vec2(pc.width, pc.height) * 0.5;
  float dist = roundedRectSDF(fragLocalPos - halfSize, halfSize, pc.cornerRadius);
  
  float alpha = 1.0 - smoothstep(-1.0, 1.0, dist);

  outColor = vec4(pc.r, pc.g, pc.b, pc.a * alpha);
}