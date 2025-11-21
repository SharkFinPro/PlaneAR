#version 450

layout(push_constant) uniform QuadPC {
    int screenWidth;
    int screenHeight;
    vec2 p1;
    vec2 p2;
    vec2 p3;
} pc;

void main()
{
    vec2 pos = vec2(0, 0);
    if (gl_VertexIndex == 0)
    {
        pos = pc.p1;
    }
    else if (gl_VertexIndex == 1)
    {
        pos = pc.p2;
    }
    else
    {
        pos = pc.p3;
    }

    vec2 ndc;
    ndc.x = 2.0 * pos.x / float(pc.screenWidth)  - 1.0;
    ndc.y = 2.0 * pos.y / float(pc.screenHeight) - 1.0;

    gl_Position = vec4(ndc, 0.0, 1.0);
}