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

val sceneIdMap     = listOf(SceneId.AR, SceneId.FlightHistory, SceneId.Achievements)
val navLabels      = listOf("AR View", "History", "Achievements")
val navEmojiLabels = listOf("📷", "🕒", "🏆")

enum class SheetResult { ANIMATING, OPEN, DISMISSED }

interface Page : Scene {
    companion object {
        val flightFavorites: MutableList<Boolean> = MutableList(flightData.size) { false }
        var sheetAnimProgress: Float = 0.0f
        var sheetClosing: Boolean    = false

        // Settings overlay state
        var settingsOverlayOpen: Boolean    = false
        var settingsAnimProgress: Float     = 0.0f
        var settingsClosing: Boolean        = false
        var radiusSliderDragging: Boolean   = false

        /** True when the settings overlay is visible and should block page input. */
        val isInputBlocked: Boolean
            get() = settingsOverlayOpen || settingsAnimProgress > 0f
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
        drawSettingsGearIcon(sceneInfo)
        if (settingsOverlayOpen || settingsAnimProgress > 0f) {
            drawSettingsOverlay(sceneInfo)
        }
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

    private fun drawSettingsGearIcon(sceneInfo: SceneInfo) {
        val screenW  = sceneInfo.screenWidth
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        // Don't draw the gear icon if the settings overlay is fully open (it's redundant)
        if (settingsOverlayOpen && settingsAnimProgress >= 1.0f) return

        val iconSize = 60.0f
        val iconX    = screenW - iconSize - 30.0f
        val iconY    = 65.0f

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            // Draw a circular background behind the gear icon
            rectMode(RectMode.CORNER)
            fill(c.overlay, 80)
            val bgSize = iconSize + 20.0f
            rect(iconX - 10.0f, iconY - 10.0f, bgSize, bgSize, bgSize / 2.0f)

            // Draw the gear emoji
            textFont("emoji", 28)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            fill(255, 255, 255)
            text("⚙️", iconX + iconSize / 2.0f, iconY + iconSize / 2.0f)
        }

        // Handle tap on the gear icon
        gestures.singleTapUpPosition?.let { (tx, ty) ->
            val hitPad = 15.0f
            if (tx >= iconX - hitPad && tx <= iconX + iconSize + hitPad &&
                ty >= iconY - hitPad && ty <= iconY + iconSize + hitPad) {
                settingsOverlayOpen    = true
                settingsAnimProgress   = 0.0f
                settingsClosing        = false
                radiusSliderDragging   = false
            }
        }
    }

    private fun drawSettingsOverlay(sceneInfo: SceneInfo) {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current
        val step     = 0.06f

        // Advance animation
        if (settingsClosing) {
            settingsAnimProgress = (settingsAnimProgress - step).coerceAtLeast(0.0f)
            if (settingsAnimProgress == 0.0f) {
                settingsOverlayOpen  = false
                settingsClosing      = false
                radiusSliderDragging = false
                return
            }
        } else {
            settingsAnimProgress = (settingsAnimProgress + step).coerceAtMost(1.0f)
        }

        val eased = 1.0f - (1.0f - settingsAnimProgress) * (1.0f - settingsAnimProgress)

        // Panel dimensions
        val panelW = screenW * 0.88f
        val panelH = screenH * 0.55f
        val panelX = (screenW - panelW) / 2.0f
        val panelY = (screenH - panelH) / 2.0f
        val panelR = 32.0f

        // Slide offset: panel slides up from below
        val slideOffset = panelH * 0.3f * (1.0f - eased)

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)

            // Full-screen backdrop
            fill(c.overlay, (160 * eased).toInt())
            rect(0, 0, screenW, screenH)

            pushMatrix()
            translate(0, slideOffset)

            // Panel background
            fill(c.backgroundCard)
            rect(panelX, panelY, panelW, panelH, panelR)

            // Title
            fill(c.textPrimary)
            textFont("roboto", 20)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Settings", screenW / 2f, panelY + 70f)

            // X close button (top-right of panel)
            val closeBtnSize = 50.0f
            val closeBtnX    = panelX + panelW - closeBtnSize - 20.0f
            val closeBtnY    = panelY + 20.0f
            fill(c.divider)
            rect(closeBtnX, closeBtnY, closeBtnSize, closeBtnSize, closeBtnSize / 2.0f)
            fill(c.textPrimary)
            textFont("roboto", 20)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("✕", closeBtnX + closeBtnSize / 2.0f, closeBtnY + closeBtnSize / 2.0f)

            // Settings controls - positioned relative to panel
            val controlX = panelX + panelW * 0.05f
            val controlW = panelW * 0.90f

            // Dark Mode toggle
            AppSettings.darkMode = drawOverlayToggleCard(
                sceneInfo = sceneInfo,
                cardX     = controlX,
                cardY     = panelY + 110f,
                cardW     = controlW,
                title     = "Dark Mode",
                enabled   = AppSettings.darkMode,
                slideOffset = slideOffset
            )

            // Use Camera toggle
            AppSettings.canEnableCamera = drawOverlayToggleCard(
                sceneInfo = sceneInfo,
                cardX     = controlX,
                cardY     = panelY + 250f,
                cardW     = controlW,
                title     = "Use Camera",
                enabled   = AppSettings.canEnableCamera,
                slideOffset = slideOffset
            )

            // Radius slider
            val newRadius = drawOverlaySlider(
                sceneInfo   = sceneInfo,
                cardX       = controlX,
                cardY       = panelY + 390f,
                cardW       = controlW,
                title       = "Aircraft Search Radius",
                min         = 1,
                max         = 50,
                current     = AppSettings.searchRadiusNm,
                units       = "nm",
                dragging    = radiusSliderDragging,
                onDragStart = { radiusSliderDragging = true },
                onDragEnd   = { radiusSliderDragging = false },
                slideOffset = slideOffset
            )
            AppSettings.searchRadiusNm = newRadius

            popMatrix()

            // Input handling (unadjusted coords)
            if (!settingsClosing && settingsAnimProgress >= 1.0f) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    val adjY = ty + slideOffset

                    // X button tap
                    if (tx >= closeBtnX && tx <= closeBtnX + closeBtnSize &&
                        adjY >= closeBtnY && adjY <= closeBtnY + closeBtnSize) {
                        settingsClosing = true
                    }

                    // Backdrop tap (outside panel)
                    if (tx < panelX || tx > panelX + panelW ||
                        adjY < panelY || adjY > panelY + panelH) {
                        settingsClosing = true
                    }
                }
            }
        }
    }

    private fun drawOverlayToggleCard(
        sceneInfo: SceneInfo,
        cardX: Float,
        cardY: Float,
        cardW: Float,
        title: String,
        enabled: Boolean,
        slideOffset: Float
    ): Boolean {
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            val cardH   = 120f
            val cornerR = 20f

            // Card background
            fill(c.background)
            rect(cardX, cardY, cardW, cardH, cornerR)

            // Label
            fill(c.textPrimary)
            textFont("roboto", 15)
            textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
            text(title, cardX + 30f, cardY + cardH / 2f)

            // Pill toggle
            val pillW  = 110f
            val pillH  = 60f
            val pillR  = pillH / 2f
            val thumbR = pillR - 6f
            val pillX  = cardX + cardW - 30f - pillW
            val pillY  = cardY + (cardH - pillH) / 2f

            fill(if (enabled) c.accent else c.trackBackground)
            rect(pillX, pillY, pillW, pillH, pillR)

            fill(255, 255, 255)
            val thumbX = if (enabled) pillX + pillW - pillR else pillX + pillR
            rect(thumbX - thumbR, pillY + 6f, thumbR * 2f, thumbR * 2f, thumbR)

            // Tap detection (adjusted for slide offset)
            if (!settingsClosing && settingsAnimProgress >= 1.0f) {
                val tapped = gestures.singleTapUpPosition?.let { (tx, ty) ->
                    val adjY = ty + slideOffset
                    tx in cardX..(cardX + cardW) && adjY in cardY..(cardY + cardH)
                } ?: false

                return if (tapped) !enabled else enabled
            }

            return enabled
        }
    }

    private fun drawOverlaySlider(
        sceneInfo: SceneInfo,
        cardX: Float,
        cardY: Float,
        cardW: Float,
        title: String,
        min: Int,
        max: Int,
        current: Int,
        units: String,
        dragging: Boolean,
        onDragStart: () -> Unit,
        onDragEnd: () -> Unit,
        slideOffset: Float
    ): Int {
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            val cardH   = 200f
            val cornerR = 20f

            // Card background
            fill(c.background)
            rect(cardX, cardY, cardW, cardH, cornerR)

            // Title label
            fill(c.textPrimary)
            textFont("roboto", 14)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text(title, cardX + 30f, cardY + 50f)

            // Current value label
            fill(c.accent)
            textFont("roboto", 14)
            textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
            text("$current $units", cardX + cardW - 30f, cardY + 50f)

            // Track geometry
            val trackPad   = 40f
            val trackLeft  = cardX + trackPad
            val trackRight = cardX + cardW - trackPad
            val trackWidth = trackRight - trackLeft
            val trackY     = cardY + 120f
            val trackH     = 8f
            val fraction   = (current - min).toFloat() / (max - min).toFloat()
            val thumbX     = trackLeft + fraction * trackWidth

            // Track background
            fill(c.trackBackground)
            rect(trackLeft, trackY - trackH / 2f, trackWidth, trackH, trackH / 2f)

            // Filled portion up to thumb
            fill(c.accent)
            rect(trackLeft, trackY - trackH / 2f, thumbX - trackLeft, trackH, trackH / 2f)

            // Thumb outer circle
            val thumbR = 28f
            fill(c.accent)
            rect(thumbX - thumbR, trackY - thumbR, thumbR * 2f, thumbR * 2f, thumbR)

            // Thumb inner circle
            val innerR = 14f
            fill(c.textOnAccent)
            rect(thumbX - innerR, trackY - innerR, innerR * 2f, innerR * 2f, innerR)

            // Min / max labels
            fill(c.textHint)
            textFont("roboto", 11)
            textAlign(TextAlignH.LEFT, TextAlignV.TOP)
            text("$min $units", trackLeft, trackY + 20f)
            textAlign(TextAlignH.RIGHT, TextAlignV.TOP)
            text("$max $units", trackRight, trackY + 20f)

            if (!settingsClosing && settingsAnimProgress >= 1.0f) {
                // Hit area — adjust touch y for slide offset
                val hitTop    = trackY - 44f
                val hitBottom = trackY + 44f

                gestures.touchDownPosition?.let { (tx, ty) ->
                    val adjY = ty + slideOffset
                    if (adjY in hitTop..hitBottom && tx in trackLeft..trackRight) onDragStart()
                }

                if (!gestures.isTouching) onDragEnd()

                if (dragging && gestures.isTouching) {
                    val fingerX = gestures.scrollPosition?.first
                        ?: gestures.singleTapPosition?.first
                    if (fingerX != null) {
                        val clamped = fingerX.coerceIn(trackLeft, trackRight)
                        val newFrac = (clamped - trackLeft) / trackWidth
                        return (min + newFrac * (max - min)).toInt().coerceIn(min, max)
                    }
                }
            }

            return current
        }
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

        // Don't process nav input when settings overlay is open
        val settingsBlocking = settingsOverlayOpen || settingsAnimProgress > 0f

        // Swipe left/right on the nav bar to switch tabs
        if (!settingsBlocking && gestures.flung && !sheetClosing && sheetAnimProgress == 0f) {
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

                // Check for and handle button press (skip when settings overlay is open)
                if (!settingsBlocking) {
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
            }

            // Small bar above buttons
            fill(c.divider)
            rect(0, screenHeight - navHeight, screenWidth, 1)
        }
    }
}