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

class FavoritesPage : Page {
    override val sceneId = SceneId.Favorites

    companion object {
        /** Set to true before navigating to Favorites to land directly on Overview */
        var navigateToOverview = false
    }

    private var currentPage = 0

    private var showingOverview = false
    private var favScrollOffset = 0f
    private var rvScrollOffset = 0f
    private var ovFavScrollOffset = 0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        // Consume the external navigation flag
        if (navigateToOverview) {
            showingOverview = true
            navigateToOverview = false
        }

        if (showingOverview) {
            renderOverview(sceneInfo, sceneSwitcher)
        } else {
            renderFavoritesList(sceneInfo, sceneSwitcher)
        }
    }

    private fun renderFavoritesList(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight - navHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        var tapConsumed = false
        val tapPos      = gestures.singleTapUpPosition

        val favIndices = flightData.indices.filter { i -> i < Page.flightFavorites.size && Page.flightFavorites[i] }
        val totalFavs  = favIndices.size

        val margin     = screenW * 0.05f
        val contentW   = screenW - 2f * margin

        // ── Header area ────────────────────────────────────────────────
        val headerY    = screenH * 0.04f
        val titleY     = headerY + 55.0f

        // ── Tab bar (pill-style: History | Favorites | Overview) ──────
        val tabBarY    = titleY + 50.0f
        val tabBarH    = 65f
        val tabCount   = 3
        val tabGap     = 16f
        val tabW       = (contentW - (tabCount - 1) * tabGap) / tabCount
        val tabR       = tabBarH / 2f
        val tabLabels  = arrayOf("History", "Favorites", "Overview")

        // ── Card list area ─────────────────────────────────────────────
        val listStartY = tabBarY + tabBarH + 30f
        val listEndY   = screenH - 30f
        val cardH      = 150f
        val cardGap    = 18f
        val cardR      = 20f

        // Thumbnail dimensions
        val thumbW     = cardH - 30f
        val thumbH     = cardH - 30f
        val thumbR     = 14f
        val thumbPad   = 15f

        // Scroll handling
        val totalContentH = totalFavs * (cardH + cardGap)
        val visibleH      = listEndY - listStartY
        val maxScroll     = max(0f, totalContentH - visibleH)

        if (gestures.isScrolling && !FlightDetailSheet.isOpen) {
            val pos = gestures.scrollPosition
            val inZone = pos != null &&
                    pos.second >= listStartY && pos.second <= listEndY &&
                    pos.first >= margin && pos.first <= margin + contentW
            if (inZone) favScrollOffset += gestures.scrollDelta.second
        }
        favScrollOffset = favScrollOffset.coerceIn(0f, maxScroll)

        // Fling to scroll faster
        if (gestures.flung && !FlightDetailSheet.isOpen) {
            val startY = gestures.flingStartPosition?.second ?: 0f
            if (startY >= listStartY && startY <= listEndY) {
                when (gestures.flingDirection) {
                    FlingDirection.UP   -> favScrollOffset = (favScrollOffset + visibleH * 0.6f).coerceAtMost(maxScroll)
                    FlingDirection.DOWN -> favScrollOffset = (favScrollOffset - visibleH * 0.6f).coerceAtLeast(0f)
                    else -> {}
                }
            }
        }

        val widgetShown = FlightDetailSheet.isOpen

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            rectMode(RectMode.CORNER)
            imageMode(ImageMode.CORNER)

            // Background
            fill(c.background)
            rect(0, 0, screenW, screenH)

            // ── Title ──────────────────────────────────────────────────
            fill(c.textPrimary)
            textFont("roboto", 20)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Favorites", screenW / 2.0f, titleY)

            // ── Tab bar ────────────────────────────────────────────────
            for (ti in 0 until tabCount) {
                val tabX = margin + ti * (tabW + tabGap)

                if (ti == 1) {
                    // Active tab (Favorites) — filled accent
                    fill(c.accent)
                    rect(tabX, tabBarY, tabW, tabBarH, tabR)
                    fill(c.textOnAccent)
                } else {
                    // Inactive tabs — outlined style
                    fill(c.backgroundRow)
                    rect(tabX, tabBarY, tabW, tabBarH, tabR)
                    fill(c.textSecondary)
                }

                textFont("roboto", 13)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text(tabLabels[ti], tabX + tabW / 2f, tabBarY + tabBarH / 2f)
            }

            // Tab tap handling
            if (tapPos != null && !widgetShown) {
                val (tx, ty) = tapPos
                if (ty >= tabBarY && ty <= tabBarY + tabBarH) {
                    for (ti in 0 until tabCount) {
                        val tabX = margin + ti * (tabW + tabGap)
                        if (tx >= tabX && tx <= tabX + tabW) {
                            when (ti) {
                                0 -> { sceneSwitcher.setCurrentScene(SceneId.FlightHistory.id); tapConsumed = true }
                                1 -> { /* Already on Favorites */ }
                                2 -> { showingOverview = true; tapConsumed = true }
                            }
                            break
                        }
                    }
                }
            }

            // ── Empty state ────────────────────────────────────────────
            if (totalFavs == 0) {
                fill(c.textHint)
                textFont("roboto", 14)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text("No favorites yet", screenW / 2f, listStartY + visibleH / 2f - 20f)
                textFont("roboto", 11)
                text("Star flights in History to add them here", screenW / 2f, listStartY + visibleH / 2f + 20f)
            }

            // ── Favorite flight cards ──────────────────────────────────
            for (fi in 0 until totalFavs) {
                val i = favIndices[fi]
                val rawY = listStartY + fi * (cardH + cardGap) - favScrollOffset

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
                image("plane", imgX, imgY, thumbW, thumbH)

                // ── Text content ───────────────────────────────────────
                val textX     = imgX + thumbW + 20f
                val rightEdge = cardX + contentW - 20f

                // Callsign (bold, primary)
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

                // ── Right side: Favorite star (always gold here) ───────
                val starX = rightEdge - 10f
                fill(239, 191, 4)
                textFont("emoji", 20)
                textAlign(TextAlignH.RIGHT, TextAlignV.CENTER)
                text("⭐", starX, cardY + 40f)

                // Status badge
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
                if (tapPos != null && !tapConsumed && !widgetShown) {
                    val (tx, ty) = tapPos

                    if (ty >= cardY && ty <= cardY + cardH && tx >= cardX && tx <= cardX + contentW) {
                        // Star tap zone — un-favorite
                        if (tx >= starX - 50f && tx <= starX + 10f && ty >= cardY + 15f && ty <= cardY + 65f) {
                            Page.flightFavorites[i] = false
                            FlightHistoryStore.save()
                            tapConsumed = true
                        }

                        // Row tap (open detail)
                        if (!tapConsumed) {
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
                val scrollBarY      = listStartY + (favScrollOffset / maxScroll) * (scrollBarTrackH - scrollBarH)

                fill(c.divider, 80)
                rect(scrollBarX - 4f, listStartY, 8f, scrollBarTrackH, 4f)
                fill(c.accent, 180)
                rect(scrollBarX - 4f, scrollBarY, 8f, scrollBarH, 4f)
            }
        }

        postRender(sceneInfo, sceneSwitcher)
    }

    private fun renderOverview(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight - navHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        var overviewTapConsumed = false

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            rectMode(RectMode.CORNER)
            imageMode(ImageMode.CORNER)
            fill(c.background)
            rect(0, 0, screenW, screenH)

            val margin  = screenW * 0.05f
            val contentW = screenW - 2f * margin

            // ── Header ────────────────────────────────────────────────
            val headerY = screenH * 0.04f
            val titleY  = headerY + 55.0f

            fill(c.textPrimary)
            textFont("roboto", 20)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Overview", screenW / 2f, titleY)

            // ── Tab bar ───────────────────────────────────────────────
            val tabBarY   = titleY + 50.0f
            val tabBarH   = 65f
            val tabCount  = 3
            val tabGap    = 16f
            val tabW      = (contentW - (tabCount - 1) * tabGap) / tabCount
            val tabR      = tabBarH / 2f
            val tabLabels = arrayOf("History", "Favorites", "Overview")

            for (ti in 0 until tabCount) {
                val tabX = margin + ti * (tabW + tabGap)

                if (ti == 2) {
                    // Active tab (Overview) — filled accent
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

            // Tab tap handling
            if (!FlightDetailSheet.isOpen) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (ty >= tabBarY && ty <= tabBarY + tabBarH) {
                        for (ti in 0 until tabCount) {
                            val tabX = margin + ti * (tabW + tabGap)
                            if (tx >= tabX && tx <= tabX + tabW) {
                                when (ti) {
                                    0 -> { sceneSwitcher.setCurrentScene(SceneId.FlightHistory.id); overviewTapConsumed = true }
                                    1 -> { showingOverview = false; overviewTapConsumed = true }
                                    2 -> { /* Already on Overview */ }
                                }
                                break
                            }
                        }
                    }
                }
            }

            // ── Hero card (Flights in Air) ────────────────────────────
            val heroTop  = tabBarY + tabBarH + 30f
            val heroH    = screenH * 0.30f
            val heroR    = 28f

            // Gradient-like hero card
            fill(c.accent)
            rect(margin, heroTop, contentW, heroH, heroR)

            // "Flights in Air" button pill
            val btnW = contentW * 0.65f
            val btnH = 72f
            val btnX = margin + (contentW - btnW) / 2f
            val btnY = heroTop + 25f
            val btnR = btnH / 2f

            fill(c.backgroundCard)
            rect(btnX, btnY, btnW, btnH, btnR)
            fill(c.accent)
            textFont("roboto", 14)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Flights in Air", screenW / 2f, btnY + btnH / 2f)

            // Plane image in hero card
            val imgPad = contentW * 0.08f
            val imgTop = btnY + btnH + 20f
            val imgW   = contentW - 2f * imgPad
            val imgH   = heroTop + heroH - imgTop - 20f
            val imgR   = 18f

            fill(c.backgroundCard)
            rect(margin + imgPad, imgTop, imgW, imgH, imgR)
            image("plane", margin + imgPad, imgTop, imgW, imgH)

            // ── Stats row ─────────────────────────────────────────────
            val statsY    = heroTop + heroH + 30f
            val statCardH = 120f
            val statCardW = (contentW - tabGap * 2f) / 3f
            val statR     = 18f

            val totalFlights = flightData.size
            val totalFavorites = flightData.indices.count { i -> i < Page.flightFavorites.size && Page.flightFavorites[i] }
            val liveCount = try {
                edu.osu.t22.planear.scenes.SceneSwitcher.adsbManager
                    .getRepository()
                    .getAircraft()
                    .size
            } catch (_: Exception) { 0 }

            val statLabels = arrayOf("Total", "Favorites", "Live")
            val statValues = arrayOf("$totalFlights", "$totalFavorites", "$liveCount")

            for (si in 0 until 3) {
                val sx = margin + si * (statCardW + tabGap)

                fill(c.backgroundCard)
                rect(sx, statsY, statCardW, statCardH, statR)

                fill(c.textPrimary)
                textFont("roboto", 22)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text(statValues[si], sx + statCardW / 2f, statsY + statCardH * 0.40f)

                fill(c.textHint)
                textFont("roboto", 11)
                text(statLabels[si], sx + statCardW / 2f, statsY + statCardH * 0.75f)
            }

            // ── Recently Viewed carousel ──────────────────────────────
            val rvSectionY = statsY + statCardH + 40f
            fill(c.textPrimary)
            textFont("roboto", 15)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("Recently Viewed", margin, rvSectionY)

            val rvCount       = minOf(10, flightData.size)
            val rvCardGap     = 18f
            val rvCardW       = (contentW - 2f * rvCardGap) / 3f
            val rvTop         = rvSectionY + 25f
            val rvImgH        = rvCardW * 0.75f
            val rvCornerR     = 14f
            val rvTotalW      = rvCount * (rvCardW + rvCardGap) - rvCardGap
            val rvMaxScroll   = max(0f, rvTotalW - contentW)

            val rvZoneTop     = rvTop - 10f
            val rvZoneBottom  = rvTop + rvImgH + 80f

            if (gestures.isScrolling && !FlightDetailSheet.isOpen) {
                val pos = gestures.scrollPosition
                val inZone = pos != null &&
                        pos.second >= rvZoneTop && pos.second <= rvZoneBottom &&
                        pos.first >= margin && pos.first <= margin + contentW
                if (inZone) rvScrollOffset += gestures.scrollDelta.first
            }
            rvScrollOffset = rvScrollOffset.coerceIn(0f, rvMaxScroll)

            for (i in 0 until rvCount) {
                val rawX = margin + i * (rvCardW + rvCardGap) - rvScrollOffset
                if (rawX + rvCardW < margin - 20f || rawX > margin + contentW + 20f) continue

                // Card background
                fill(c.backgroundCard)
                rect(rawX, rvTop, rvCardW, rvImgH + 70f, rvCornerR)

                // Image
                fill(c.backgroundRow)
                rect(rawX + 8f, rvTop + 8f, rvCardW - 16f, rvImgH - 8f, rvCornerR - 4f)
                image("plane", rawX + 8f, rvTop + 8f, rvCardW - 16f, rvImgH - 8f)

                // Callsign
                val textY = rvTop + rvImgH + 25f
                fill(c.textPrimary)
                textFont("roboto", 12)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[i].callsign, rawX + 10f, textY)

                // Date
                fill(c.textSecondary)
                textFont("roboto", 10)
                text(flightData[i].date, rawX + 10f, textY + 28f)
            }

            if (!FlightDetailSheet.isOpen && !overviewTapConsumed) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (ty >= rvTop && ty <= rvTop + rvImgH + 70f) {
                        for (i in 0 until rvCount) {
                            val rawX = margin + i * (rvCardW + rvCardGap) - rvScrollOffset
                            if (tx >= rawX && tx <= rawX + rvCardW) {
                                FlightDetailSheet.open(flightData[i])
                                overviewTapConsumed = true
                                break
                            }
                        }
                    }
                }
            }

            // ── Favorites carousel ────────────────────────────────────
            val favSectionY = rvTop + rvImgH + 120f
            fill(c.textPrimary)
            textFont("roboto", 15)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("Favorites", margin, favSectionY)

            val ovFavIndices     = flightData.indices.filter { i -> i < Page.flightFavorites.size && Page.flightFavorites[i] }
            val favCount         = ovFavIndices.size
            val favTop           = favSectionY + 25f
            val favTotalContentW = if (favCount > 0) favCount * (rvCardW + rvCardGap) - rvCardGap else 0f
            val favMaxScroll     = max(0f, favTotalContentW - contentW)

            val favZoneTop    = favTop - 10f
            val favZoneBottom = favTop + rvImgH + 80f
            if (gestures.isScrolling && !FlightDetailSheet.isOpen) {
                val pos = gestures.scrollPosition
                val inFavZone = pos != null &&
                        pos.second >= favZoneTop  && pos.second <= favZoneBottom &&
                        pos.first  >= margin      && pos.first  <= margin + contentW
                if (inFavZone) ovFavScrollOffset += gestures.scrollDelta.first
            }
            ovFavScrollOffset = ovFavScrollOffset.coerceIn(0f, favMaxScroll)

            if (favCount == 0) {
                fill(c.textHint)
                textFont("roboto", 12)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text("No favorites yet", margin, favSectionY + 50f)
            }

            for (fi in 0 until favCount) {
                val idx  = ovFavIndices[fi]
                val rawX = margin + fi * (rvCardW + rvCardGap) - ovFavScrollOffset
                if (rawX + rvCardW < margin - 20f || rawX > margin + contentW + 20f) continue

                // Card background
                fill(c.backgroundCard)
                rect(rawX, favTop, rvCardW, rvImgH + 70f, rvCornerR)

                // Image
                fill(c.backgroundRow)
                rect(rawX + 8f, favTop + 8f, rvCardW - 16f, rvImgH - 8f, rvCornerR - 4f)
                image("plane", rawX + 8f, favTop + 8f, rvCardW - 16f, rvImgH - 8f)

                // Callsign
                val ftextY = favTop + rvImgH + 25f
                fill(c.textPrimary)
                textFont("roboto", 12)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[idx].callsign, rawX + 10f, ftextY)

                // Date
                fill(c.textSecondary)
                textFont("roboto", 10)
                text(flightData[idx].date, rawX + 10f, ftextY + 28f)
            }

            if (!FlightDetailSheet.isOpen && !overviewTapConsumed) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (ty >= favTop && ty <= favTop + rvImgH + 70f) {
                        for (fi in 0 until favCount) {
                            val idx  = ovFavIndices[fi]
                            val rawX = margin + fi * (rvCardW + rvCardGap) - ovFavScrollOffset
                            if (tx >= rawX && tx <= rawX + rvCardW) {
                                FlightDetailSheet.open(flightData[idx])
                                overviewTapConsumed = true
                                break
                            }
                        }
                    }
                }
            }

            // "Flights in Air" button tap -> go to AR
            if (!FlightDetailSheet.isOpen && !overviewTapConsumed) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (tx >= btnX && tx <= btnX + btnW && ty >= btnY && ty <= btnY + btnH) {
                        sceneSwitcher.setCurrentScene(SceneId.AR.id)
                    }
                }
            }
        }

        postRender(sceneInfo, sceneSwitcher)
    }
}