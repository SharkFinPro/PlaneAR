package edu.osu.t22.planear.scenes.pages

import android.util.Log
import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.FrameGestureDetector.FlingDirection
import edu.osu.t22.planear.graphicsEngine.EllipseMode
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.achievements.AchievementStore
import edu.osu.t22.planear.scenes.Scene
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher
import edu.osu.t22.planear.adsb.Aircraft

enum class SceneId(val id: Int) {
    AR(2), FlightHistory(3), Settings(4), Favorites(5), Achievements(6)
}

val sceneIdMap     = listOf(SceneId.AR, SceneId.FlightHistory, SceneId.Achievements, SceneId.Settings)
val navLabels      = listOf("AR View", "History", "Achievements", "Settings")
val navEmojiLabels = listOf("📷", "🕒", "🏆", "⚙️")

enum class SheetResult { ANIMATING, OPEN, DISMISSED }

object FlightDetailSheet {

    private const val ANIM_STEP = 0.06f

    var pendingFlight: Aircraft? = null
        private set

    var isOpen: Boolean = false
        private set

    private var animProgress: Float = 0f
    private var closing: Boolean = false

    /** Open the sheet for the given flight. Safe to call from any page. */
    fun open(flight: Aircraft) {
        pendingFlight = flight
        animProgress = 0f
        closing = false
        isOpen = true
    }

    /** Programmatically start the closing animation. */
    fun dismiss() {
        closing = true
    }

    /** Reset all state (called internally after dismiss animation completes). */
    private fun reset() {
        pendingFlight = null
        animProgress = 0f
        closing = false
        isOpen = false
    }

    /**
     * Draw the flight detail bottom sheet on top of the current frame.
     * Call this at the end of any page's render() while [isOpen] is true.
     *
     * @param sceneInfo      Current frame's SceneInfo
     * @param tapConsumed    Pass true if the current frame already handled a tap
     *                       (prevents double-consuming the same gesture).
     * @return [SheetResult] indicating current animation state.
     */
    fun draw(
        sceneInfo: SceneInfo,
        tapConsumed: Boolean = false
    ): SheetResult {
        val flight = pendingFlight ?: return SheetResult.DISMISSED

        val screenW = sceneInfo.screenWidth
        val screenH = sceneInfo.screenHeight
        // Use the same navHeight constant that Page uses so the sheet never overlaps nav
        val navHeight = 225.0f
        val navTop = screenH - navHeight

        val gestures = sceneInfo.gestures
        val c = AppColors.current

        // --- Advance animation ---
        if (closing) {
            animProgress = (animProgress - ANIM_STEP).coerceAtLeast(0f)
            if (animProgress == 0f) {
                reset()
                return SheetResult.DISMISSED
            }
        } else {
            animProgress = (animProgress + ANIM_STEP).coerceAtMost(1f)
        }

        // Ease-out curve: fast start, settles smoothly
        val eased = 1f - (1f - animProgress) * (1f - animProgress)

        // --- Layout ---
        // Sheet covers exactly the bottom half of the screen (above the nav bar)
        val sheetH = screenH * 0.5f
        val sheetY = screenH - navHeight - sheetH // resting Y (fully open)
        val slideOffset = sheetH * (1f - eased)        // 0 when open, sheetH when hidden
        val sheetR = 32f
        val padX = screenW * 0.08f
        val rightEdge = screenW - padX

        // Favourite state
        val flightIndex = Page.flightFavorites.indices.firstOrNull {
            flightData.getOrNull(it)?.callsign == flight.callsign
        } ?: -1
        val isFavorited = flightIndex >= 0 && Page.flightFavorites[flightIndex]

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)

            // ── Backdrop ──────────────────────────────────────────────────────
            fill(c.overlay, (140 * eased).toInt())
            rect(0, 0, screenW, navTop)

            // ── Sheet (translated up from below nav) ──────────────────────────
            pushMatrix()
            translate(0, slideOffset)

            // Background
            fill(c.backgroundCard)
            rect(0, sheetY + sheetR, screenW, sheetH - sheetR)
            rect(0, sheetY, screenW, sheetH * 0.4f, sheetR)

            // Drag handle pill
            fill(c.divider)
            ellipseMode(EllipseMode.CENTER)
            ellipse(screenW / 2f, sheetY + 22f, 60f, 10f)

            // Callsign
            fill(c.textPrimary)
            textFont("roboto", 26)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text(flight.label, padX, sheetY + 100f)

            // Accent underline
            fill(c.accent)
            rect(padX, sheetY + 112f, 60f, 4f, 2f)

            // Favourite star
            val starX = rightEdge
            val starY = sheetY + 90f
            textFont("emoji", 32)
            textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
            if (isFavorited) fill(239, 191, 4) else fill(c.divider)
            text("⭐", starX, starY)

            // ── Close button data — anchored inside the sheet near the bottom ──────
            // data placed here so it can be reference by data fields below
            val btnW = screenW * 0.60f
            val btnH = 72f
            val btnX = (screenW - btnW) / 2f
            // Place it with a fixed margin above the nav bar top
            val btnY = screenH - navHeight - btnH - 28f

            // ── Data fields ───────────────────────────────────────────────────
            val fieldStartY = sheetY + 180f
            val fieldEnd    = screenH - navHeight - btnH - 28f - 20f
            val fieldGap    = (fieldEnd - fieldStartY) / 5f

            fun drawField(label: String, value: String, y: Float) {
                fill(c.textHint)
                textFont("roboto", 12)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(label, padX, y)

                fill(c.textPrimary)
                textFont("roboto", 18)
                textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
                text(value, rightEdge, y + 30f)

                fill(c.divider)
                rect(padX, y + 44f, screenW - 2f * padX, 1.5f)
            }

            // draw data fields
            drawField("CALLSIGN",  flight.label,                                fieldStartY)
            drawField("ALTITUDE",  "${"%.0f".format(flight.altitudeSeaLevel)} ft", fieldStartY + fieldGap)
            drawField("SPEED",     "${flight.groundSpeed?.toInt() ?: "N/A"} kts", fieldStartY + fieldGap * 2)
            drawField("TYPE",      flight.type ?: "Unknown",                    fieldStartY + fieldGap * 3)
            drawField("HEADING",   "${flight.headingDegrees?.toInt() ?: "N/A"}°", fieldStartY + fieldGap * 4)


            // draw buttons
            fill(c.accent)
            rect(btnX, btnY, btnW, btnH, btnH / 2f)
            fill(c.textOnAccent)
            textFont("roboto", 16)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Close", screenW / 2f, btnY + btnH / 2f)

            popMatrix()

            // ── Input handling (only when fully open and not already closing) ──
            if (!tapConsumed && !closing && animProgress >= 1f) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    val adjY = ty + slideOffset

                    // Toggle favourite
                    if (flightIndex >= 0 &&
                        tx >= starX - 70f && tx <= starX + 10f &&
                        adjY >= starY - 60f && adjY <= starY + 10f
                    ) {
                        Page.flightFavorites[flightIndex] = !Page.flightFavorites[flightIndex]
                    }

                    // Close taps
                    val hitClose =
                        tx >= btnX && tx <= btnX + btnW && adjY >= btnY && adjY <= btnY + btnH
                    val hitBackdrop = adjY < sheetY
                    if (hitClose || hitBackdrop) closing = true
                }

                // Swipe-down to dismiss
                if (gestures.isScrolling) {
                    val (scrollX, scrollY) = gestures.scrollPosition ?: Pair(0f, 0f)
                    val adjScrollY = scrollY + slideOffset
                    val onSheet = adjScrollY >= sheetY && scrollX >= 0f && scrollX <= screenW
                    val swipingDown = gestures.scrollDelta.second < -30f
                    if (onSheet && swipingDown) closing = true
                }
            }
        }

        return if (animProgress >= 1f && !closing) SheetResult.OPEN else SheetResult.ANIMATING
    }
}

interface Page : Scene {
    companion object {
        val flightFavorites: MutableList<Boolean> = MutableList(flightData.size) { false }
    }

    val sceneId: SceneId
    val navHeight: Float get() = 225.0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {}

    fun postRender(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        // Update AR page flag — achievements only track on the AR tab.
        // ArPage.render() sets this to true; all other pages leave it untouched,
        // so we correct it here for non-AR pages.
        if (sceneId != SceneId.AR) {
            AchievementStore.isOnArPage = false
        }

        if (FlightDetailSheet.isOpen) {
            FlightDetailSheet.draw(sceneInfo)
        }

        drawNavButtons(sceneInfo, sceneSwitcher)
    }


    private fun drawNavButtons(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenWidth    = sceneInfo.screenWidth
        val screenHeight   = sceneInfo.screenHeight
        val buttonWidth    = screenWidth / navLabels.size.toFloat()
        val buttonTop      = screenHeight - navHeight
        val activeNavIndex = sceneIdMap.indexOf(sceneId).let { if (it < 0) -1 else it }
        val tapPos         = sceneInfo.gestures.singleTapUpPosition
        val gestures       = sceneInfo.gestures
        val c              = AppColors.current

        // Swipe left/right on the nav bar to switch tabs
        if (gestures.flung && !FlightDetailSheet.isOpen) {
            val startY = gestures.flingStartPosition?.second ?: Float.MAX_VALUE
            if (startY >= buttonTop) {
                val newIndex = when (gestures.flingDirection) {
                    FlingDirection.LEFT  -> (activeNavIndex + 1).coerceAtMost(sceneIdMap.lastIndex)
                    FlingDirection.RIGHT -> (activeNavIndex - 1).coerceAtLeast(0)
                    else -> activeNavIndex
                }
                if (newIndex != activeNavIndex && newIndex >= 0) {
                    // Only navigate if the scene is registered (Achievements placeholder is not yet)
                    val targetId = sceneIdMap[newIndex].id
                    try { sceneSwitcher.setCurrentScene(targetId) } catch (_: Exception) {}
                }
            }
        }

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            // Navigation Background
            fill(c.navBackground)
            rect(0, buttonTop, screenWidth, navHeight)

            for (i in navLabels.indices) {
                val offsetX = i.toFloat() * buttonWidth

                if (i == activeNavIndex) {
                    fill(c.navActive)
                    rect(offsetX, buttonTop, buttonWidth, navHeight)
                }

                // Button Text
                val (tr, tg, tb) = if (i == activeNavIndex) c.textOnAccent else c.navInactive
                fill(tr, tg, tb)

                val yOffset = 30.0f
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                textFont("roboto", 13)
                text(navLabels[i], offsetX + buttonWidth / 2.0f, screenHeight - navHeight / 2.0f + yOffset)
                textFont("emoji", 22)
                text(navEmojiLabels[i], offsetX + buttonWidth / 2.0f, screenHeight - navHeight / 2.0f - yOffset)

                // Check for and handle button press
                tapPos?.let { (tx, ty) ->
                    if (tx > offsetX && tx < offsetX + buttonWidth &&
                        ty > buttonTop && ty < buttonTop + navHeight) {
                        val targetId = sceneIdMap[i].id
                        try {
                            sceneSwitcher.setCurrentScene(targetId)
                            Log.i("Page", "Nav $i tapped - switching to scene $targetId")
                        } catch (_: Exception) {
                            Log.i("Page", "Nav $i tapped - scene $targetId not yet registered")
                        }
                    }
                }
            }

            // Small bar above buttons
            fill(c.divider)
            rect(0, screenHeight - navHeight, screenWidth, 1)
        }
    }
}