#version 450

layout(location = 0) in vec2  fragUV;
layout(location = 1) in float fragHeadingRad;
layout(location = 2) in float fragAlpha;

layout(location = 0) out vec4 outColor;

// ── helpers ──────────────────────────────────────────────────────────────────

float sdCircle(vec2 p, float r) {
    return length(p) - r;
}

float sdSegment(vec2 p, vec2 a, vec2 b, float w) {
    vec2 ab = b - a;
    vec2 ap = p - a;
    float t  = clamp(dot(ap, ab) / dot(ab, ab), 0.0, 1.0);
    return length(ap - t * ab) - w;
}

vec2 rot(vec2 v, float a) {
    float c = cos(a), s = sin(a);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

float fill(float sdf) {
    return 1.0 - smoothstep(-0.015, 0.015, sdf);
}

// ── main ─────────────────────────────────────────────────────────────────────

void main() {
    vec2 uv = fragUV;

    float distFromCentre = length(uv);
    if (distFromCentre > 1.0) {
        discard;
    }

    float bgMask = fill(sdCircle(uv, 0.92));
    vec3  bgCol  = vec3(0.08, 0.08, 0.12);
    float bgA    = bgMask * 0.72;

    float ringOuter = fill(sdCircle(uv,  0.92));
    float ringInner = fill(sdCircle(uv,  0.82));
    float ringMask  = ringOuter * (1.0 - ringInner);
    vec3  ringCol   = vec3(0.85, 0.85, 0.90);

    float ticks = 0.0;
    for (int i = 0; i < 4; i++) {
        float angle = float(i) * 1.5707963;
        vec2 tp = rot(uv, -angle);
        ticks = max(ticks, fill(sdSegment(tp, vec2(0.0, 0.84), vec2(0.0, 0.93), 0.025)));
    }
    float northTick = fill(sdSegment(uv, vec2(0.0, 0.78), vec2(0.0, 0.93), 0.038));
    ticks = max(ticks, northTick);

    float letters = 0.0;
    float lScale  = 0.10;

    vec2 dirs[4] = vec2[](
    vec2( 0.0,  0.62),
    vec2( 0.62, 0.0),
    vec2( 0.0, -0.62),
    vec2(-0.62, 0.0)
    );

    // N
    {
        vec2 lp = (uv - dirs[0]) / lScale;
        lp.y *= -1;
        float s = 0.0;
        s = max(s, fill(sdSegment(lp, vec2(-0.6,-1.0), vec2(-0.6, 1.0), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2( 0.6,-1.0), vec2( 0.6, 1.0), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2(-0.6,-1.0), vec2( 0.6, 1.0), 0.18)));
        letters = max(letters, s);
    }

    // E
    {
        vec2 lp = (uv - dirs[1]) / lScale;
        lp.y *= -1;
        float s = 0.0;
        s = max(s, fill(sdSegment(lp, vec2(-0.6,-1.0), vec2(-0.6, 1.0), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2(-0.6,-1.0), vec2( 0.6,-1.0), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2(-0.6, 0.0), vec2( 0.4, 0.0), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2(-0.6, 1.0), vec2( 0.6, 1.0), 0.18)));
        letters = max(letters, s);
    }

    // S
    {
        vec2 lp = (uv - dirs[2]) / lScale;
        lp.y *= -1;
        float s = 0.0;
        s = max(s, fill(sdSegment(lp, vec2( 0.6,-1.0), vec2(-0.6,-1.0), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2(-0.6,-1.0), vec2(-0.6, 0.0), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2(-0.6, 0.0), vec2( 0.6, 0.0), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2( 0.6, 0.0), vec2( 0.6, 1.0), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2( 0.6, 1.0), vec2(-0.6, 1.0), 0.18)));
        letters = max(letters, s);
    }

    // W
    {
        vec2 lp = (uv - dirs[3]) / lScale;
        lp.y *= -1;
        float s = 0.0;
        s = max(s, fill(sdSegment(lp, vec2(-0.9,-1.0), vec2(-0.5, 1.0), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2(-0.5, 1.0), vec2( 0.0,-0.1), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2( 0.0,-0.1), vec2( 0.5, 1.0), 0.18)));
        s = max(s, fill(sdSegment(lp, vec2( 0.5, 1.0), vec2( 0.9,-1.0), 0.18)));
        letters = max(letters, s);
    }

    vec2 arrowUV = rot(uv, -fragHeadingRad);
    arrowUV /= 0.85;

    float arrow = 0.0;
    arrow = max(arrow, fill(sdSegment(arrowUV, vec2( 0.0,  0.55), vec2(-0.14, 0.22), 0.055)));
    arrow = max(arrow, fill(sdSegment(arrowUV, vec2( 0.0,  0.55), vec2( 0.14, 0.22), 0.055)));
    arrow = max(arrow, fill(sdSegment(arrowUV, vec2(-0.14, 0.22), vec2( 0.14, 0.22), 0.055)));
    arrow = max(arrow, fill(sdSegment(arrowUV, vec2(0.0, 0.22), vec2(0.0, -0.35), 0.055)));
    arrow = max(arrow, fill(sdSegment(arrowUV, vec2(-0.12,-0.35), vec2(0.12,-0.35), 0.055)));

    vec3 arrowCol = vec3(1.0, 0.80, 0.15);

    vec3 col = bgCol;
    float a  = bgA;

    col  = mix(col, ringCol,              ringMask);
    a    = max(a, ringMask * fragAlpha);

    col  = mix(col, vec3(1.0),            ticks);
    a    = max(a, ticks * fragAlpha);

    col  = mix(col, vec3(0.95, 0.95, 1.0), letters);
    a    = max(a, letters * fragAlpha);

    col  = mix(col, arrowCol, arrow);
    a    = max(a, arrow * fragAlpha);

    a   *= fragAlpha;
    outColor = vec4(col, a);
}
