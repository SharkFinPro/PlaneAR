package edu.osu.t22.planear.scenes.pages

/**
 * Shared transition state for the History / Favorites / Overview tab bar.
 *
 * When a tab switch is triggered, the **outgoing** page sets [direction] and
 * resets [progress] to 0.  The **incoming** page advances [progress] each frame
 * until it reaches 1, then clears [active].
 *
 * The incoming page applies a horizontal translate of
 *   `screenW * (1 - easedProgress) * direction`
 * to its content (below the fixed header/tabs), giving a smooth slide-in effect.
 */
object TabTransition {
    private const val ANIM_SPEED = 0.08f   // frames to ~1 → ≈12 frames ≈ 200 ms @ 60 fps

    /** +1 = slide from right, -1 = slide from left */
    var direction: Int = 1
        private set

    /** 0 → just started, 1 → fully arrived */
    var progress: Float = 1f
        private set

    /** True while the slide-in animation is still playing */
    val active: Boolean get() = progress < 1f

    /** Call once on the outgoing page right before the scene switch */
    fun start(slideFromRight: Boolean) {
        direction = if (slideFromRight) 1 else -1
        progress = 0f
    }

    /**
     * Call at the start of every render frame on the incoming page.
     * Returns the current eased offset multiplier (0 = off-screen, 1 = settled).
     */
    fun advance(): Float {
        if (progress < 1f) {
            progress = (progress + ANIM_SPEED).coerceAtMost(1f)
        }
        // Ease-out: fast start, slow settle
        val eased = 1f - (1f - progress) * (1f - progress)
        return eased
    }
}
