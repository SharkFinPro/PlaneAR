#version 450

layout(push_constant) uniform GlyphData {
  float posX;
  float posY;
  float u0;
  float v0;
  float u1;
  float v1;
  float width;
  float height;
} glyph;

layout(location = 0) out vec2 fragUV;

float s = 0.5;
vec2 vertices[4] = {
  {-s, -s},
  {s, -s},
  {-s, s},
  {s, s}
};

void main()
{
  vec2 coord = vertices[gl_VertexIndex];

  gl_Position = vec4(coord, 0.0, 1.0);

  fragUV = mix(
    vec2(glyph.u0, glyph.v0),
    vec2(glyph.u1, glyph.v1),
    vec2(coord.x + s, coord.y + s)
  );
}