#version 450

// Push constant mirrors Compass::PushConstant exactly (see Primitives2D.h).
layout(push_constant) uniform compassPC {
    mat4  mvp;        // projMatrix * viewMatrix
    vec3  worldPos;   // aircraft world-space position
    float size;       // half-size of the compass quad in world units
    vec3  camRight;   // camera right axis (from inverse view)
    float offsetX;    // additional world-unit shift along camRight (top-right anchor)
    vec3  camUp;      // camera up axis (from inverse view)
    float offsetY;    // additional world-unit shift along camUp   (top-right anchor)
} pc;

// UV [-1,+1] in compass face space; +Y is visual north in the fragment shader.
layout(location = 0) out vec2 fragUV;

void main() {
    // Triangle-strip quad, same winding as point.vert.
    //  0 → (-1,-1)   1 → (+1,-1)
    //  2 → (-1,+1)   3 → (+1,+1)
    vec2 corners[4] = vec2[](
        vec2(-1.0, -1.0),
        vec2( 1.0, -1.0),
        vec2(-1.0,  1.0),
        vec2( 1.0,  1.0)
    );

    vec2 uv = corners[gl_VertexIndex];
    fragUV  = uv;

    // Anchor point = aircraft position shifted by the screen-space offset so the
    // compass sits above-right of the aircraft dot.
    vec3 anchor = pc.worldPos
                + pc.camRight * pc.offsetX
                + pc.camUp    * pc.offsetY;

    // Expand the billboard quad around that anchor.
    vec3 worldPos = anchor
                  + pc.camRight * (uv.x * pc.size)
                  + pc.camUp    * (uv.y * pc.size);

    gl_Position = pc.mvp * vec4(worldPos, 1.0);
}
