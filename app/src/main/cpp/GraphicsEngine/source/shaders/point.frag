#version 450

layout(location = 0) in vec4 fragColor;
layout(location = 1) in vec2 fragUV;   // [-1,+1] in billboard face space
layout(location = 2) in vec2 fragAspect;

layout(location = 0) out vec4 outColor;

// ── SDF helpers ───────────────────────────────────────────────────────────────

// Signed distance to an axis-aligned rounded rectangle centred at origin.
// b = half-extents, r = corner radius.
float sdRoundedBox(vec2 p, vec2 b, float r) {
  vec2 q = abs(p) - b + r;
  return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

// Smooth AA step: 1 inside, 0 outside.
float fillAA(float sdf, float fw) {
  return 1.0 - smoothstep(-fw, fw, sdf);
}

// ── Main ──────────────────────────────────────────────────────────────────────

void main() {
  // fragUV is [-1,+1] in the *stretched* quad space.  We need SDF distances
  // that are consistent with the actual card proportions, so remap UV into
  // an aspect-correct space where the card half-extents equal (aspectX, aspectY).
  vec2 p = fragUV * fragAspect;

  vec2  halfExt    = fragAspect;
  float cornerR    = 0.18;   // corner radius in the same units as halfExt
  float borderW    = 0.055;  // border thickness

  // Derivative-based antialiasing width (world-unit fwidth approximation).
  float fw = fwidth(p.x) * 1.5;

  float distCard   = sdRoundedBox(p, halfExt, cornerR);
  float distInner  = sdRoundedBox(p, halfExt - borderW, cornerR - borderW * 0.5);

  // Discard outside the card boundary (with a tiny AA fringe).
  if (distCard > fw) discard;

  float cardMask   = fillAA(distCard,  fw);
  float innerMask  = fillAA(distInner, fw);
  float borderMask = cardMask * (1.0 - innerMask);

  // ── Fill colour — the color passed from Kotlin is the accent/selection tint ──
  vec3  accentCol  = fragColor.rgb;

  // ── Background — frosted dark panel ──────────────────────────────────────────
  // Subtle vertical gradient: slightly lighter at the top.
  float gradT    = (fragUV.y + 1.0) * 0.5;   // 0 = bottom, 1 = top
  vec3  bgTop    = vec3(0.14, 0.15, 0.18);
  vec3  bgBot    = vec3(0.08, 0.09, 0.11);
  vec3  bgCol    = mix(bgBot, bgTop, gradT);

  // Thin horizontal separator line 40 % down from the top — visually divides
  // the callsign area from the distance/info area.
  float sepY     = fragAspect.y * 0.30;   // separator Y in card space
  float sepDist  = abs(p.y - sepY) - 0.018;
  float sepMask  = fillAA(sepDist, fw) * innerMask;
  vec3  sepCol   = vec3(0.30, 0.32, 0.38);

  // Left accent stripe — narrow vertical band on the far-left edge of the fill.
  float stripeX  = -fragAspect.x + borderW + 0.12;
  float stripeDist = abs(p.x - stripeX) - 0.04;
  float stripeMask = fillAA(stripeDist, fw) * innerMask;

  // ── Border — uses the accent colour when selected, neutral white when not ───
  // accentCol encodes the Kotlin fill: white (245,245,245) = unselected,
  // green (150,245,150) = selected.  We detect selection by checking the
  // green channel dominance.
  float isSelected = step(0.05, accentCol.g - accentCol.r - 0.05);

  vec3  borderCol  = mix(vec3(0.75, 0.77, 0.82),   // neutral light-grey border
                         accentCol,                  // selection accent colour
                         isSelected);

  // Selection glow — soft halo just inside the border when selected.
  float glowDist = sdRoundedBox(p, halfExt - borderW * 2.0, cornerR - borderW);
  float glowMask = fillAA(glowDist + 0.12, fw * 6.0) *
                   (1.0 - fillAA(glowDist, fw)) *
                   innerMask * isSelected;
  vec3  glowCol  = accentCol * 0.55;

  // ── Composite ────────────────────────────────────────────────────────────────
  vec3 col = bgCol;
  col = mix(col, sepCol,    sepMask);
  col = mix(col, accentCol * 0.45, stripeMask);  // muted stripe
  col = mix(col, glowCol,   glowMask);
  col = mix(col, borderCol, borderMask);

  // Alpha: full opacity inside, AA fade at edges.
  float alpha = mix(0.82, 1.0, borderMask) * cardMask;

  outColor = vec4(col, alpha);
}
