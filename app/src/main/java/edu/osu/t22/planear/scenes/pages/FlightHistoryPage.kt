package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.FrameGestureDetector.FlingDirection
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.ImageMode
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher
import kotlin.math.max

class FlightHistoryPage : Page {
    override val sceneId = SceneId.FlightHistory

    companion object {
        const val FLIGHTS_PER_PAGE = 10
    }

    private var scrollOffset = 0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight - navHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        var tapConsumed = false
        val tapPos      = gestures.singleTapUpPosition

        val totalFlights = flightData.size

        val margin     = screenW * 0.05f
        val contentW   = screenW - 2f * margin

        // ── Header area ────────────────────────────────────────────────
        val headerY    = screenH * 0.04f

        // ── Tab bar (pill-style: History | Favorites | Overview) ──────
        val tabBarY    = headerY + 65.0f
        val tabBarH    = 65f
        val tabCount   = 3
        val tabGap     = 16f
        val tabW       = (contentW - (tabCount - 1) * tabGap) / tabCount
        val tabR       = tabBarH / 2f
        val tabLabels  = arrayOf("History", "Favorites", "Overview")

        // The header+tabs area that sits on top of everything
        val headerBottom = tabBarY + tabBarH + 15f

        // ── Card list area ─────────────────────────────────────────────
        val listStartY = headerBottom + 15f
        val listEndY   = screenH - 30f
        val cardH      = 150f
        val cardGap    = 40f
        val cardR      = 20f

        // Thumbnail dimensions
        val thumbW     = cardH - 30f
        val thumbH     = cardH - 30f
        val thumbR     = 14f
        val thumbPad   = 15f

        // Scroll handling
        val totalContentH = totalFlights * (cardH + cardGap)
        val visibleH      = listEndY - listStartY
        val maxScroll     = max(0f, totalContentH - visibleH)

        if (gestures.isScrolling && !FlightDetailSheet.isOpen) {
            val pos = gestures.scrollPosition
            val inZone = pos != null &&
                    pos.second >= listStartY && pos.second <= listEndY &&
                    pos.first >= margin && pos.first <= margin + contentW
            if (inZone) scrollOffset += gestures.scrollDelta.second
        }
        scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

        // Fling to scroll faster
        if (gestures.flung && !FlightDetailSheet.isOpen) {
            val startY = gestures.flingStartPosition?.second ?: 0f
            if (startY >= listStartY && startY <= listEndY) {
                when (gestures.flingDirection) {
                    FlingDirection.UP   -> scrollOffset = (scrollOffset + visibleH * 0.6f).coerceAtMost(maxScroll)
                    FlingDirection.DOWN -> scrollOffset = (scrollOffset - visibleH * 0.6f).coerceAtLeast(0f)
                    else -> {}
                }
            }
        }

        val widgetShown = FlightDetailSheet.isOpen

        // ── Transition animation ───────────────────────────────────────
        val transEased = TabTransition.advance()
        val slideOffsetX = if (TabTransition.active)
            screenW * (1f - transEased) * TabTransition.direction
        else 0f

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            rectMode(RectMode.CORNER)
            imageMode(ImageMode.CORNER)

            // Full-screen background (drawn first, always static)
            fill(c.background)
            rect(0, 0, screenW, screenH)

            // ════════════════════════════════════════════════════════════
            // LAYER 1 — Scrollable card content (slides with transition)
            // ════════════════════════════════════════════════════════════
            pushMatrix()
            translate(slideOffsetX, 0)

            for (i in 0 until totalFlights) {
                val rawY = listStartY + i * (cardH + cardGap) - scrollOffset

                // Skip cards outside visible area
                if (rawY + cardH < listStartY - 10f || rawY > listEndY + 10f) continue

                val cardX = margin
                val cardY = rawY

                // Card background
                fill(c.backgroundCard)
                rect(cardX, cardY, contentW, cardH, cardR)

                // ── Thumbnail ──────────────────────────────────────────
                val imgX = cardX + thumbPad
                val imgY = cardY + (cardH - thumbH) / 2f

                fill(c.backgroundRow)
                rect(imgX, imgY, thumbW, thumbH, thumbR)
                
                fill(c.textPrimary)
                textFont("emoji", 32)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text("✈️", imgX + thumbW / 2f, imgY + thumbH / 2f)

                // ── Text content ───────────────────────────────────────
                val textX     = imgX + thumbW + 20f
                val rightEdge = cardX + contentW - 20f

                // Callsign
                fill(c.textPrimary)
                textFont("roboto", 15)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[i].callsign, textX, cardY + 45f)

                // Plane type
                fill(c.textSecondary)
                textFont("roboto", 11)
                val typeLabel = flightData[i].planeType.ifBlank { "Unknown" }
                text(typeLabel, textX, cardY + 70f)

                // Date
                fill(c.textSecondary)
                textFont("roboto", 11)
                text(flightData[i].date, textX, cardY + 95f)

                // ── Favorite star ──────────────────────────────────────
                val isFav = i < Page.flightFavorites.size && Page.flightFavorites[i]
                val starX = rightEdge - 10f
                if (isFav) fill(239, 191, 4) else fill(c.divider)
                textFont("emoji", 20)
                textAlign(TextAlignH.RIGHT, TextAlignV.CENTER)
                text("⭐", starX, cardY + 40f)

                // ── Status badge ───────────────────────────────────────
                val badgeW = 120f
                val badgeH = 38f
                val badgeX = rightEdge - badgeW
                val badgeY = cardY + cardH - 55f
                val badgeR = badgeH / 2f

                val isLive = edu.osu.t22.planear.scenes.SceneSwitcher.adsbManager
                    .getRepository()
                    .getAircraft()
                    .any { it.label == flightData[i].callsign }

                if (isLive) {
                    fill(c.accent)
                    rect(badgeX, badgeY, badgeW, badgeH, badgeR)
                    fill(c.textOnAccent)
                    textFont("roboto", 11)
                    textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                    text("In Air", badgeX + badgeW / 2f, badgeY + badgeH / 2f)
                } else {
                    fill(c.backgroundRow)
                    rect(badgeX, badgeY, badgeW, badgeH, badgeR)
                    fill(c.textSecondary)
                    textFont("roboto", 11)
                    textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                    text("Logged", badgeX + badgeW / 2f, badgeY + badgeH / 2f)
                }

                // ── Card tap handling ──────────────────────────────────
                if (tapPos != null && !tapConsumed && !widgetShown && !TabTransition.active) {
                    val (tx, ty) = tapPos

                    if (ty >= cardY && ty <= cardY + cardH && tx >= cardX && tx <= cardX + contentW) {
                        if (tx >= starX - 50f && tx <= starX + 10f && ty >= cardY + 15f && ty <= cardY + 65f) {
                            if (i < Page.flightFavorites.size) {
                                Page.flightFavorites[i] = !Page.flightFavorites[i]
                                FlightHistoryStore.save()
                            }
                            tapConsumed = true
                        }

                        if (!tapConsumed && ty >= cardY && ty <= cardY + cardH) {
                            FlightDetailSheet.open(flightData[i])
                            tapConsumed = true
                        }
                    }
                }
            }

            // ── Scrollbar ──────────────────────────────────────────────
            if (maxScroll > 0f) {
                val scrollBarX      = screenW - margin / 2f
                val scrollBarTrackH = visibleH
                val scrollBarH      = (visibleH / totalContentH * scrollBarTrackH).coerceAtLeast(50f)
                val scrollBarY      = listStartY + (scrollOffset / maxScroll) * (scrollBarTrackH - scrollBarH)

                fill(c.divider, 80)
                rect(scrollBarX - 4f, listStartY, 8f, scrollBarTrackH, 4f)
                fill(c.accent, 180)
                rect(scrollBarX - 4f, scrollBarY, 8f, scrollBarH, 4f)
            }

            popMatrix()

            // ════════════════════════════════════════════════════════════
            // LAYER 2 — Fixed header + tab bar (always on top)
            // ════════════════════════════════════════════════════════════

            // Solid background behind header — covers any cards scrolling behind
            fill(c.background)
            rect(0, 0, screenW, headerBottom)

            // Tab pills
            for (ti in 0 until tabCount) {
                val tabX = margin + ti * (tabW + tabGap)

                if (ti == 0) {
                    fill(c.accent)
                    rect(tabX, tabBarY, tabW, tabBarH, tabR)
                    fill(c.textOnAccent)
                } else {
                    fill(c.backgroundRow)
                    rect(tabX, tabBarY, tabW, tabBarH, tabR)
                    fill(c.textSecondary)
                }

                textFont("roboto", 13)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text(tabLabels[ti], tabX + tabW / 2f, tabBarY + tabBarH / 2f)
            }

            // Tab tap handling (only when transition is not playing)
            if (tapPos != null && !widgetShown && !TabTransition.active) {
                val (tx, ty) = tapPos
                if (ty >= tabBarY && ty <= tabBarY + tabBarH) {
                    for (ti in 0 until tabCount) {
                        val tabX = margin + ti * (tabW + tabGap)
                        if (tx >= tabX && tx <= tabX + tabW) {
                            when (ti) {
                                0 -> { /* Already on History */ }
                                1 -> {
                                    TabTransition.start(slideFromRight = true)
                                    FavoritesPage.requestedSubView = false
                                    sceneSwitcher.setCurrentScene(SceneId.Favorites.id)
                                    tapConsumed = true
                                }
                                2 -> {
                                    TabTransition.start(slideFromRight = true)
                                    FavoritesPage.requestedSubView = true
                                    sceneSwitcher.setCurrentScene(SceneId.Favorites.id)
                                    tapConsumed = true
                                }
                            }
                            break
                        }
                    }
                }
            }
        }

        postRender(sceneInfo, sceneSwitcher)
    }
}