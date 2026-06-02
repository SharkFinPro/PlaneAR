#version 450

// Push constant — only the fields after the vertex-stage data are used here.
layout(push_constant) uniform compassPC {
    // vertex fields (consumed by .vert, re-declared here to maintain correct offsets)
    layout(offset = 0)   mat4  mvp;
    layout(offset = 64)  vec3  worldPos;
    layout(offset = 76)  float size;
    layout(offset = 80)  vec3  camRight;
    layout(offset = 92)  float offsetX;
    layout(offset = 96)  vec3  camUp;
    layout(offset = 108) float offsetY;
    // fragment fields
    layout(offset = 112) float headingRad;  // aircraft heading in radians (adjusted for mode)
    layout(offset = 116) float alpha;       // overall opacity (0-1)
} pc;

layout(location = 0) in  vec2 fragUV;   // [-1,+1] in compass face space; +Y = visual north
layout(location = 0) out vec4 outColor;

// ── helpers ──────────────────────────────────────────────────────────────────

float sdCircle(vec2 p, float r) {
    return length(p) - r;
}

// Signed distance to a line segment from a to b, with half-width w.
float sdSegment(vec2 p, vec2 a, vec2 b, float w) {
    vec2 ab = b - a;
    vec2 ap = p - a;
    float t  = clamp(dot(ap, ab) / dot(ab, ab), 0.0, 1.0);
    return length(ap - t * ab) - w;
}

// Rotate a 2D vector.
vec2 rot(vec2 v, float a) {
    float c = cos(a), s = sin(a);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

// AA step: 1 inside shape, 0 outside, smooth at the boundary.
float fill(float sdf) {
    return 1.0 - smoothstep(-0.015, 0.015, sdf);
}

// ── main ─────────────────────────────────────────────────────────────────────

void main() {
    vec2 uv = fragUV; // [-1,+1], +Y up

    // Discard corners so the compass is a circle, not a square.
    float distFromCentre = length(uv);
    if (distFromCentre > 1.0) {
        discard;
    }

    // ── Background disc ──────────────────────────────────────────────────────
    // Semi-transparent dark fill.
    float bgMask = fill(sdCircle(uv, 0.92));
    vec3  bgCol  = vec3(0.08, 0.08, 0.12);
    float bgA    = bgMask * 0.72;

    // ── Outer ring ───────────────────────────────────────────────────────────
    float ringOuter = fill(sdCircle(uv,  0.92));
    float ringInner = fill(sdCircle(uv,  0.82));
    float ringMask  = ringOuter * (1.0 - ringInner);
    vec3  ringCol   = vec3(0.85, 0.85, 0.90);

    // ── Cardinal tick marks ──────────────────────────────────────────────────
    // Four ticks at N / E / S / W, thin rectangles along each axis.
    float ticks = 0.0;
    for (int i = 0; i < 4; i++) {
        float angle = float(i) * 1.5707963; // 0, 90, 180, 270 degrees
        vec2 tp = rot(uv, -angle);
        // Tick: a short thick segment pointing inward from the ring.
        ticks = max(ticks, fill(sdSegment(tp, vec2(0.0, 0.84), vec2(0.0, 0.93), 0.025)));
    }
    // Slightly thicker tick for North so it is distinguishable.
    float northTick = fill(sdSegment(uv, vec2(0.0, 0.78), vec2(0.0, 0.93), 0.038));
    ticks = max(ticks, northTick);

    // ── Cardinal letter dots (N, E, S, W) ────────────────────────────────────
    // Drawn as minimal SDF glyph approximations at radius ~0.62.
    // Each letter is authored in a small local coordinate system, then placed.
    float letters = 0.0;
    float lScale  = 0.10; // size of each letter cell

    // Helper: draw one pixel-art 3×5 letter as a set of filled segments.
    // We use a simple approach: encode each letter as a list of dot positions
    // in a 3-wide × 5-tall grid (col 0-2, row 0-4, row 0 = top).

    // Place and draw each cardinal letter.
    vec2 dirs[4] = vec2[](
        vec2( 0.0,  0.62),  // N — top
        vec2( 0.62, 0.0),   // E — right
        vec2( 0.0, -0.62),  // S — bottom
        vec2(-0.62, 0.0)    // W — left
    );

    // N
    {
        vec2 lp = (uv - dirs[0]) / lScale; // local [-1,1] ish coords
        lp.y *= -1;
        // Two vertical strokes + diagonal
        float s = 0.0;
        s = max(s, fill(sdSegment(lp, vec2(-0.6,-1.0), vec2(-0.6, 1.0), 0.18))); // left
        s = max(s, fill(sdSegment(lp, vec2( 0.6,-1.0), vec2( 0.6, 1.0), 0.18))); // right
        s = max(s, fill(sdSegment(lp, vec2(-0.6,-1.0), vec2( 0.6, 1.0), 0.18))); // diagonal
        letters = max(letters, s);
    }

    // E
    {
        vec2 lp = (uv - dirs[1]) / lScale;
        lp.y *= -1;
        float s = 0.0;
        s = max(s, fill(sdSegment(lp, vec2(-0.6,-1.0), vec2(-0.6, 1.0), 0.18))); // spine
        s = max(s, fill(sdSegment(lp, vec2(-0.6,-1.0), vec2( 0.6,-1.0), 0.18))); // top
        s = max(s, fill(sdSegment(lp, vec2(-0.6, 0.0), vec2( 0.4, 0.0), 0.18))); // mid
        s = max(s, fill(sdSegment(lp, vec2(-0.6, 1.0), vec2( 0.6, 1.0), 0.18))); // bot
        letters = max(letters, s);
    }

    // S
    {
        vec2 lp = (uv - dirs[2]) / lScale;
        lp.y *= -1;
        float s = 0.0;
        s = max(s, fill(sdSegment(lp, vec2( 0.6,-1.0), vec2(-0.6,-1.0), 0.18))); // top
        s = max(s, fill(sdSegment(lp, vec2(-0.6,-1.0), vec2(-0.6, 0.0), 0.18))); // top-left
        s = max(s, fill(sdSegment(lp, vec2(-0.6, 0.0), vec2( 0.6, 0.0), 0.18))); // mid
        s = max(s, fill(sdSegment(lp, vec2( 0.6, 0.0), vec2( 0.6, 1.0), 0.18))); // bot-right
        s = max(s, fill(sdSegment(lp, vec2( 0.6, 1.0), vec2(-0.6, 1.0), 0.18))); // bot
        letters = max(letters, s);
    }

    // W
    {
        vec2 lp = (uv - dirs[3]) / lScale;
        lp.y *= -1;
        float s = 0.0;
        s = max(s, fill(sdSegment(lp, vec2(-0.9,-1.0), vec2(-0.5, 1.0), 0.18))); // left stroke
        s = max(s, fill(sdSegment(lp, vec2(-0.5, 1.0), vec2( 0.0,-0.1), 0.18))); // inner-left
        s = max(s, fill(sdSegment(lp, vec2( 0.0,-0.1), vec2( 0.5, 1.0), 0.18))); // inner-right
        s = max(s, fill(sdSegment(lp, vec2( 0.5, 1.0), vec2( 0.9,-1.0), 0.18))); // right stroke
        letters = max(letters, s);
    }

    // ── Heading arrow ─────────────────────────────────────────────────────────
    // The arrow points in the direction of the aircraft heading.
    // headingRad = 0 → arrow points toward +Y (north/up).
    // Rotate the UV so the arrow geometry is authored pointing up (+Y).
    vec2 arrowUV = rot(uv, -pc.headingRad);
    arrowUV /= 0.85;

    float arrow = 0.0;
    // Arrowhead: a triangle approximated by two diagonal segments + a flat base.
    arrow = max(arrow, fill(sdSegment(arrowUV, vec2( 0.0,  0.55), vec2(-0.14, 0.22), 0.055)));
    arrow = max(arrow, fill(sdSegment(arrowUV, vec2( 0.0,  0.55), vec2( 0.14, 0.22), 0.055)));
    arrow = max(arrow, fill(sdSegment(arrowUV, vec2(-0.14, 0.22), vec2( 0.14, 0.22), 0.055)));
    // Shaft
    arrow = max(arrow, fill(sdSegment(arrowUV, vec2(0.0, 0.22), vec2(0.0, -0.35), 0.055)));
    // Tail flare
    arrow = max(arrow, fill(sdSegment(arrowUV, vec2(-0.12,-0.35), vec2(0.12,-0.35), 0.055)));

    // Arrow colour: warm yellow-orange for visibility against any background.
    vec3 arrowCol = vec3(1.0, 0.80, 0.15);

    // ── Composite ─────────────────────────────────────────────────────────────
    vec3 col = bgCol;
    float a  = bgA;

    // Ring (white/light grey) — additive blend over background
    col  = mix(col, ringCol,         ringMask);
    a    = max(a, ringMask * pc.alpha);

    // Ticks (white)
    col  = mix(col, vec3(1.0),       ticks);
    a    = max(a, ticks * pc.alpha);

    // Letters (light grey)
    col  = mix(col, vec3(0.95, 0.95, 1.0), letters);
    a    = max(a, letters * pc.alpha);

    // Arrow (yellow)
    col  = mix(col, arrowCol, arrow);
    a    = max(a, arrow * pc.alpha);

    a   *= pc.alpha;
    outColor = vec4(col, a);
}
