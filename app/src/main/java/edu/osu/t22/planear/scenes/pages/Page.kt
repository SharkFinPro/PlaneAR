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

enum class SceneId(val id: Int) {
    AR(2), FlightHistory(3), Settings(4), Favorites(5), Achievements(6)
}

val sceneIdMap     = listOf(SceneId.AR, SceneId.FlightHistory, SceneId.Achievements, SceneId.Settings)
val navLabels      = listOf("AR View", "History", "Achievements", "Settings")
val navEmojiLabels = listOf("📷", "🕒", "🏆", "⚙️")

enum class SheetResult { ANIMATING, OPEN, DISMISSED }

interface Page : Scene {
    companion object {
        val flightFavorites: MutableList<Boolean> = MutableList(flightData.size) { false }
        var sheetAnimProgress: Float = 0.0f
        var sheetClosing: Boolean    = false
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

        drawNavButtons(sceneInfo, sceneSwitcher)
    }

    fun drawFlightDetailWidget(
        sceneInfo: SceneInfo,
        flight: FlightEntry,
        tapAlreadyConsumed: Boolean
    ): SheetResult {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight
        val navTop   = screenH - navHeight
        val step     = 0.06f
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        // Advance progress in whichever direction we're animating
        if (sheetClosing) {
            sheetAnimProgress = (sheetAnimProgress - step).coerceAtLeast(0.0f)
            if (sheetAnimProgress == 0.0f) return SheetResult.DISMISSED
        } else {
            sheetAnimProgress = (sheetAnimProgress + step).coerceAtMost(1.0f)
        }

        // Ease out: fast start, settles smoothly
        val eased       = 1.0f - (1.0f - sheetAnimProgress) * (1.0f - sheetAnimProgress)

        // Fixed sheet dimensions - always drawn as if fully open
        val sheetH      = navTop * 0.62f
        val sheetY      = navTop - sheetH // resting position (fully open, y=0 origin)

        // Slide offset: 0 when open, sheetH when fully hidden below navTop
        val slideOffset = sheetH * (1.0f - eased)

        // Padding and edges
        val padX        = screenW * 0.08f
        val rightEdge   = screenW - padX
        val sheetR      = 32.0f

        // Look up the flight index for favorite state
        val flightIndex = flightData.indexOf(flight)
        val isFavorited = flightIndex >= 0 && flightIndex < flightFavorites.size && flightFavorites[flightIndex]

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)

            // Backdrop - no transform needed
            fill(c.overlay, (140 * eased).toInt())
            rect(0, 0, screenW, navTop)

            // Translate down by slideOffset so the sheet slides up from navTop
            pushMatrix()
            translate(0, slideOffset)

            // Sheet background
            fill(c.backgroundCard)
            rect(0, sheetY + sheetR, screenW, sheetH - sheetR)
            rect(0, sheetY, screenW, sheetH * 0.4f, sheetR)

            // Drag handle pill
            fill(c.divider)
            ellipseMode(EllipseMode.CENTER)
            ellipse(screenW / 2.0f, sheetY + 22.0f, 60.0f, 10.0f)

            // Callsign
            fill(c.textPrimary)
            textFont("roboto", 26)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text(flight.callsign, padX, sheetY + 100.0f)

            // Accent line under callsign
            fill(c.accent)
            rect(padX, sheetY + 112.0f, 60.0f, 4.0f, 2.0f)

            // Favorite star - top right of sheet header
            val starX = rightEdge
            val starY = sheetY + 90.0f
            textFont("emoji", 32)
            textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
            if (isFavorited) fill(239, 191, 4) else fill(c.divider)
            text("⭐", starX, starY)

            // Field rows
            val fieldStartY = sheetY + 200.0f
            val fieldGap    = 100.0f

            fun drawField(label: String, value: String, y: Float) {
                fill(c.textHint)
                textFont("roboto", 12)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(label, padX, y)
                fill(c.textPrimary)
                textFont("roboto", 18)
                textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
                text(value, rightEdge, y + 38.0f)
                fill(c.divider)
                rect(padX, y + 56.0f, screenW - 2.0f * padX, 1.5f)
            }

            drawField("TAKEOFF",       flight.takeoffTime,       fieldStartY)
            drawField("LANDING",       flight.landingTime,       fieldStartY + fieldGap)
            drawField("AIRCRAFT TYPE", flight.planeType,         fieldStartY + fieldGap * 2)
            drawField("AIRSPEED",      "${flight.airspeed} kts", fieldStartY + fieldGap * 3)

            // Close button
            val btnW = screenW * 0.60f
            val btnH = 84.0f
            val btnX = (screenW - btnW) / 2.0f
            val btnY = navTop - btnH - 36.0f
            fill(c.accent)
            rect(btnX, btnY, btnW, btnH, btnH / 2.0f)
            fill(c.textOnAccent)
            textFont("roboto", 16)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Close", screenW / 2.0f, btnY + btnH / 2.0f)

            popMatrix()

            // Input checks use unadjusted coords - offset by slideOffset to match translated positions
            if (!tapAlreadyConsumed && !sheetClosing && sheetAnimProgress >= 1.0f) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    val adjustedY = ty + slideOffset

                    // Favorite star tap
                    if (flightIndex >= 0 &&
                        tx >= starX - 70.0f && tx <= starX + 10.0f &&
                        adjustedY >= starY - 60.0f && adjustedY <= starY + 10.0f) {
                        flightFavorites[flightIndex] = !flightFavorites[flightIndex]
                    }

                    val tappedClose    = tx >= btnX && tx <= btnX + btnW && adjustedY >= btnY && adjustedY <= btnY + btnH
                    val tappedBackdrop = adjustedY < sheetY
                    if (tappedClose || tappedBackdrop) sheetClosing = true
                }

                // Swipe down to dismiss
                if (gestures.isScrolling) {
                    val (scrollX, scrollY) = gestures.scrollPosition ?: Pair(0f, 0f)
                    val adjustedScrollY    = scrollY + slideOffset
                    val onSheet            = adjustedScrollY >= sheetY && scrollX >= 0f && scrollX <= screenW
                    val swipingDown        = gestures.scrollDelta.second < -30f
                    if (onSheet && swipingDown) sheetClosing = true
                }
            }
        }

        return if (sheetAnimProgress >= 1.0f && !sheetClosing) SheetResult.OPEN else SheetResult.ANIMATING
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
        if (gestures.flung && !sheetClosing && sheetAnimProgress == 0f) {
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