#version 450

layout(push_constant) uniform EllipsePC {
  layout(offset = 76)
  float x;
  float y;
  float width;
  float height;
  float r;
  float g;
  float b;
  float a;
} pc;

layout(location = 0) in vec2 fragPos;

layout(location = 0) out vec4 outColor;

void main()
{
  vec2 offsetFromCenter = fragPos - vec2(pc.x, pc.y);

  vec2 ellipseScale = vec2(2.0 / pc.width, 2.0 / pc.height);

  vec2 normalizedPos = offsetFromCenter * ellipseScale;

  float isInside = step(dot(normalizedPos, normalizedPos), 1.0);

  outColor = vec4(pc.r, pc.g, pc.b, pc.a * isInside);
}