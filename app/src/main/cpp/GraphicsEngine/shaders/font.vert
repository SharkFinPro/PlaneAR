#version 450

layout(location = 0) out vec2 fragTexCoord;

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

    fragTexCoord = vec2(coord.x + s, coord.y + s);
}