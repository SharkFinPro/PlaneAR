package edu.osu.t22.planear

typealias Color = Triple<Int, Int, Int>

// Extension properties for Color
val Color.r: Int get() = first
val Color.g: Int get() = second
val Color.b: Int get() = third

/**
 * Central color palette for the app.
 * All pages should call fill() using these values rather than hardcoding RGB Colors.
 * Switching AppSettings.darkMode automatically changes every color across all pages.
 *
 * Usage:
 *   fill(AppColors.background)
 *   fill(AppColors.accent)
 *   fill(AppColors.textPrimary)
 *   // or destructure for RGB:
 *   val (r, g, b) = AppColors.accent;  fill(r, g, b)
 */
object AppColors {

    // Each color is a Color(r, g, b) so it unpacks cleanly into fill(r, g, b)
    data class Palette(
        val background:      Color,  // page / screen background
        val backgroundCard:  Color,  // card / panel background
        val backgroundRow:   Color,  // list row background
        val divider:         Color,  // row dividers, separators
        val accent:          Color,  // primary green accent
        val accentDisabled:  Color,  // grayed-out accent (Back/Next at boundary)
        val textPrimary:     Color,  // main body text
        val textSecondary:   Color,  // secondary / subtitle text
        val textHint:        Color,  // min/max labels, page indicators
        val textOnAccent:    Color,  // text drawn on top of accent color
        val navBackground:   Color,  // nav bar background
        val navActive:       Color,  // active nav tab highlight
        val navInactive:     Color,  // inactive nav icon/label
        val trackBackground: Color,  // slider track unfilled portion
        val danger:          Color,  // destructive actions (remove)
        val overlay:         Color,  // backdrop behind bottom sheets (use with alpha)
        val skyBackground:   Color,  // camera-unavailable AR background
    )

    private val light = Palette(
        background      = Color(245, 248, 250),
        backgroundCard  = Color(255, 255, 255),
        backgroundRow   = Color(230, 235, 240),
        divider         = Color(200, 200, 200),
        accent          = Color(76,  175, 80),
        accentDisabled  = Color(180, 180, 180),
        textPrimary     = Color(30,  30,  30),
        textSecondary   = Color(100, 100, 100),
        textHint        = Color(150, 160, 150),
        textOnAccent    = Color(255, 255, 255),
        navBackground   = Color(255, 255, 255),
        navActive       = Color(76,  217, 100),
        navInactive     = Color(100, 100, 100),
        trackBackground = Color(220, 220, 220),
        danger          = Color(220, 60,  60),
        overlay         = Color(0,   0,   0),
        skyBackground   = Color(135, 195, 235),
    )

    private val dark = Palette(
        background      = Color(18,  18,  18),
        backgroundCard  = Color(30,  30,  30),
        backgroundRow   = Color(40,  44,  48),
        divider         = Color(55,  55,  55),
        accent          = Color(76,  175, 80),
        accentDisabled  = Color(80,  80,  80),
        textPrimary     = Color(230, 230, 230),
        textSecondary   = Color(160, 160, 160),
        textHint        = Color(110, 120, 110),
        textOnAccent    = Color(255, 255, 255),
        navBackground   = Color(24,  24,  24),
        navActive       = Color(56,  142, 60),
        navInactive     = Color(140, 140, 140),
        trackBackground = Color(60,  60,  60),
        danger          = Color(200, 50,  50),
        overlay         = Color(0,   0,   0),
        skyBackground   = Color(20,  35,  55),
    )

    val current: Palette
        get() = if (AppSettings.darkMode) dark else light

    // Convenience destructuring extension so pages can write:
    //   fill(AppColors.accent)  when the renderer accepts a Color
    // or use the individual r/g/b properties directly.
}
