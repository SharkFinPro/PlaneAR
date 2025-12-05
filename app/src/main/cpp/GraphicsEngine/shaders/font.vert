#version 450

float s = 0.5;
vec2 vertices[4] = {
    {-s, -s},
    {s, -s},
    {-s, s},
    {s, s}
};

void main()
{
    gl_Position = vec4(vertices[gl_VertexIndex], 0.0, 1.0);
}