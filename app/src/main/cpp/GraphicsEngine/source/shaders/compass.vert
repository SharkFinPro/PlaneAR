#version 450

layout(location = 0) in vec3  inWorldPos;
layout(location = 1) in float inSize;
layout(location = 2) in vec3  _padVec;
layout(location = 3) in float inOffsetX;
layout(location = 4) in float inOffsetY;
layout(location = 5) in float inHeadingRad;
layout(location = 6) in float inAlpha;
layout(location = 7) in float _pad;

layout(set = 0, binding = 0) uniform Camera {
    mat4 mvp;
    vec3 camRight;
    float _pad0;
    vec3 camUp;
    float _pad1;
} camera;

layout(location = 0) out vec2  fragUV;
layout(location = 1) out float fragHeadingRad;
layout(location = 2) out float fragAlpha;

void main() {
    vec2 corners[4] = vec2[](
        vec2(-1.0, -1.0),
        vec2( 1.0, -1.0),
        vec2(-1.0,  1.0),
        vec2( 1.0,  1.0)
    );

    vec2 uv = corners[gl_VertexIndex];
    fragUV         = uv;
    fragHeadingRad = inHeadingRad;
    fragAlpha      = inAlpha;

    vec3 anchor = inWorldPos
    + camera.camRight * inOffsetX
    + camera.camUp    * inOffsetY;

    vec3 worldPos = anchor
    + camera.camRight * (uv.x * inSize)
    + camera.camUp    * (uv.y * inSize);

    gl_Position = camera.mvp * vec4(worldPos, 1.0);
}
