#version 450

void main()
{
    float s = 0.5;
    vec2 pos = vec2(-s, -s);

    if (gl_VertexIndex == 1)
    {
        pos = vec2(s, -s);
    }
    else if (gl_VertexIndex == 2)
    {
        pos = vec2(-s, s);
    }
    else if (gl_VertexIndex == 3)
    {
        pos = vec2(s, s);
    }

    gl_Position = vec4(pos, 0.0, 1.0);
}