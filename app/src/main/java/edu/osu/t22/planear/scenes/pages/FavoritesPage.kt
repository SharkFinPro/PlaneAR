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
        /**
         * Set before navigating to Favorites to control which sub-view appears.
         *   null  → keep whatever sub-view is currently showing (default)
         *   false → force the favorites list
         *   true  → force the overview
         */
        var requestedSubView: Boolean? = null
    }

    private var currentPage = 0

    private var showingOverview = false
    private var favScrollOffset = 0f
    private var rvScrollOffset = 0f
    private var ovFavScrollOffset = 0f

    // Internal transition for the Favorites ↔ Overview sub-view switch
    private var internalTransProgress = 1f
    private var internalTransDirection = 1

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        // Consume the external navigation request
        requestedSubView?.let {
            showingOverview = it
            requestedSubView = null
        }

        if (showingOverview) {
            renderOverview(sceneInfo, sceneSwitcher)
        } else {
            renderFavoritesList(sceneInfo, sceneSwitcher)
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Shared: compute header layout values
    // ════════════════════════════════════════════════════════════════════
    private data class HeaderLayout(
        val margin: Float, val contentW: Float,
        val headerY: Float, val titleY: Float,
        val tabBarY: Float, val tabBarH: Float,
        val tabGap: Float, val tabW: Float, val tabR: Float,
        val headerBottom: Float
    )

    private fun headerLayout(screenW: Float, screenH: Float): HeaderLayout {
        val margin   = screenW * 0.05f
        val contentW = screenW - 2f * margin
        val headerY  = screenH * 0.04f
        val titleY   = headerY + 55.0f
        val tabBarY  = titleY + 50.0f
        val tabBarH  = 65f
        val tabGap   = 16f
        val tabW     = (contentW - 2f * tabGap) / 3f
        val tabR     = tabBarH / 2f
        val headerBottom = tabBarY + tabBarH + 15f
        return HeaderLayout(margin, contentW, headerY, titleY, tabBarY, tabBarH, tabGap, tabW, tabR, headerBottom)
    }

    // ════════════════════════════════════════════════════════════════════
    // FAVORITES LIST
    // ════════════════════════════════════════════════════════════════════
    private fun renderFavoritesList(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight - navHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current
        val hl       = headerLayout(screenW, screenH)

        var tapConsumed = false
        val tapPos      = gestures.singleTapUpPosition

        val favIndices = flightData.indices.filter { i -> i < Page.flightFavorites.size && Page.flightFavorites[i] }
        val totalFavs  = favIndices.size

        val tabLabels  = arrayOf("History", "Favorites", "Overview")

        // ── Card list area ─────────────────────────────────────────────
        val listStartY = hl.headerBottom + 15f
        val listEndY   = screenH - 30f
        val cardH      = 150f
        val cardGap    = 18f
        val cardR      = 20f
        val thumbW     = cardH - 30f
        val thumbH     = cardH - 30f
        val thumbR     = 14f
        val thumbPad   = 15f

        val totalContentH = totalFavs * (cardH + cardGap)
        val visibleH      = listEndY - listStartY
        val maxScroll     = max(0f, totalContentH - visibleH)

        if (gestures.isScrolling && !FlightDetailSheet.isOpen) {
            val pos = gestures.scrollPosition
            val inZone = pos != null &&
                    pos.second >= listStartY && pos.second <= listEndY &&
                    pos.first >= hl.margin && pos.first <= hl.margin + hl.contentW
            if (inZone) favScrollOffset += gestures.scrollDelta.second
        }
        favScrollOffset = favScrollOffset.coerceIn(0f, maxScroll)

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

        // ── Transition ─────────────────────────────────────────────────
        val transEased = TabTransition.advance()
        val slideOffsetX = if (TabTransition.active)
            screenW * (1f - transEased) * TabTransition.direction
        else 0f

        // Internal sub-view transition
        if (internalTransProgress < 1f) {
            internalTransProgress = (internalTransProgress + 0.08f).coerceAtMost(1f)
        }
        val intEased = 1f - (1f - internalTransProgress) * (1f - internalTransProgress)
        val intSlideX = if (internalTransProgress < 1f)
            screenW * (1f - intEased) * internalTransDirection
        else 0f

        val totalSlideX = slideOffsetX + intSlideX

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            rectMode(RectMode.CORNER)
            imageMode(ImageMode.CORNER)

            // Background
            fill(c.background)
            rect(0, 0, screenW, screenH)

            // ════════════════════════════════════════════════════════════
            // LAYER 1 — Scrollable cards (with transition slide)
            // ════════════════════════════════════════════════════════════
            pushMatrix()
            translate(totalSlideX, 0)

            // Empty state
            if (totalFavs == 0) {
                fill(c.textHint)
                textFont("roboto", 14)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text("No favorites yet", screenW / 2f, listStartY + visibleH / 2f - 20f)
                textFont("roboto", 11)
                text("Star flights in History to add them here", screenW / 2f, listStartY + visibleH / 2f + 20f)
            }

            for (fi in 0 until totalFavs) {
                val i = favIndices[fi]
                val rawY = listStartY + fi * (cardH + cardGap) - favScrollOffset

                if (rawY + cardH < listStartY - 10f || rawY > listEndY + 10f) continue

                val cardX = hl.margin
                val cardY = rawY

                fill(c.backgroundCard)
                rect(cardX, cardY, hl.contentW, cardH, cardR)

                val imgX = cardX + thumbPad
                val imgY = cardY + (cardH - thumbH) / 2f
                fill(c.backgroundRow)
                rect(imgX, imgY, thumbW, thumbH, thumbR)
                image("plane", imgX, imgY, thumbW, thumbH)

                val textX     = imgX + thumbW + 20f
                val rightEdge = cardX + hl.contentW - 20f

                fill(c.textPrimary)
                textFont("roboto", 15)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[i].callsign, textX, cardY + 45f)

                fill(c.textSecondary)
                textFont("roboto", 11)
                text(flightData[i].planeType.ifBlank { "Unknown" }, textX, cardY + 70f)

                fill(c.textSecondary)
                textFont("roboto", 11)
                text(flightData[i].date, textX, cardY + 95f)

                val starX = rightEdge - 10f
                fill(239, 191, 4)
                textFont("emoji", 20)
                textAlign(TextAlignH.RIGHT, TextAlignV.CENTER)
                text("⭐", starX, cardY + 40f)

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

                if (tapPos != null && !tapConsumed && !widgetShown && !TabTransition.active && internalTransProgress >= 1f) {
                    val (tx, ty) = tapPos
                    if (ty >= cardY && ty <= cardY + cardH && tx >= cardX && tx <= cardX + hl.contentW) {
                        if (tx >= starX - 50f && tx <= starX + 10f && ty >= cardY + 15f && ty <= cardY + 65f) {
                            Page.flightFavorites[i] = false
                            FlightHistoryStore.save()
                            tapConsumed = true
                        }
                        if (!tapConsumed) {
                            FlightDetailSheet.open(flightData[i])
                            tapConsumed = true
                        }
                    }
                }
            }

            // Scrollbar
            if (maxScroll > 0f) {
                val scrollBarX      = screenW - hl.margin / 2f
                val scrollBarTrackH = visibleH
                val scrollBarH      = (visibleH / totalContentH * scrollBarTrackH).coerceAtLeast(50f)
                val scrollBarY      = listStartY + (favScrollOffset / maxScroll) * (scrollBarTrackH - scrollBarH)

                fill(c.divider, 80)
                rect(scrollBarX - 4f, listStartY, 8f, scrollBarTrackH, 4f)
                fill(c.accent, 180)
                rect(scrollBarX - 4f, scrollBarY, 8f, scrollBarH, 4f)
            }

            popMatrix()

            // ════════════════════════════════════════════════════════════
            // LAYER 2 — Fixed header + tab bar (always on top)
            // ════════════════════════════════════════════════════════════
            fill(c.background)
            rect(0, 0, screenW, hl.headerBottom)

            fill(c.textPrimary)
            textFont("roboto", 20)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Favorites", screenW / 2.0f, hl.titleY)

            for (ti in 0 until 3) {
                val tabX = hl.margin + ti * (hl.tabW + hl.tabGap)

                if (ti == 1) {
                    fill(c.accent)
                    rect(tabX, hl.tabBarY, hl.tabW, hl.tabBarH, hl.tabR)
                    fill(c.textOnAccent)
                } else {
                    fill(c.backgroundRow)
                    rect(tabX, hl.tabBarY, hl.tabW, hl.tabBarH, hl.tabR)
                    fill(c.textSecondary)
                }

                textFont("roboto", 13)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text(tabLabels[ti], tabX + hl.tabW / 2f, hl.tabBarY + hl.tabBarH / 2f)
            }

            // Tab tap handling
            if (tapPos != null && !widgetShown && !TabTransition.active && internalTransProgress >= 1f) {
                val (tx, ty) = tapPos
                if (ty >= hl.tabBarY && ty <= hl.tabBarY + hl.tabBarH) {
                    for (ti in 0 until 3) {
                        val tabX = hl.margin + ti * (hl.tabW + hl.tabGap)
                        if (tx >= tabX && tx <= tabX + hl.tabW) {
                            when (ti) {
                                0 -> {
                                    TabTransition.start(slideFromRight = false)
                                    sceneSwitcher.setCurrentScene(SceneId.FlightHistory.id)
                                    tapConsumed = true
                                }
                                1 -> { /* Already on Favorites */ }
                                2 -> {
                                    internalTransDirection = 1
                                    internalTransProgress = 0f
                                    showingOverview = true
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

    // ════════════════════════════════════════════════════════════════════
    // OVERVIEW
    // ════════════════════════════════════════════════════════════════════
    private fun renderOverview(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight - navHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current
        val hl       = headerLayout(screenW, screenH)

        var overviewTapConsumed = false

        val tabLabels = arrayOf("History", "Favorites", "Overview")

        // ── Transition ─────────────────────────────────────────────────
        val transEased = TabTransition.advance()
        val slideOffsetX = if (TabTransition.active)
            screenW * (1f - transEased) * TabTransition.direction
        else 0f

        // Internal sub-view transition
        if (internalTransProgress < 1f) {
            internalTransProgress = (internalTransProgress + 0.08f).coerceAtMost(1f)
        }
        val intEased = 1f - (1f - internalTransProgress) * (1f - internalTransProgress)
        val intSlideX = if (internalTransProgress < 1f)
            screenW * (1f - intEased) * internalTransDirection
        else 0f

        val totalSlideX = slideOffsetX + intSlideX

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            rectMode(RectMode.CORNER)
            imageMode(ImageMode.CORNER)

            // Background
            fill(c.background)
            rect(0, 0, screenW, screenH)

            // ════════════════════════════════════════════════════════════
            // LAYER 1 — Overview content (with transition slide)
            // ════════════════════════════════════════════════════════════
            pushMatrix()
            translate(totalSlideX, 0)

            val contentTop = hl.headerBottom + 15f

            // ── Hero card ─────────────────────────────────────────────
            val heroTop  = contentTop
            val heroH    = screenH * 0.30f
            val heroR    = 28f

            fill(c.accent)
            rect(hl.margin, heroTop, hl.contentW, heroH, heroR)

            val btnW = hl.contentW * 0.65f
            val btnH = 72f
            val btnX = hl.margin + (hl.contentW - btnW) / 2f
            val btnY = heroTop + 25f
            val btnR = btnH / 2f

            fill(c.backgroundCard)
            rect(btnX, btnY, btnW, btnH, btnR)
            fill(c.accent)
            textFont("roboto", 14)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Flights in Air", screenW / 2f, btnY + btnH / 2f)

            val imgPad = hl.contentW * 0.08f
            val imgTop = btnY + btnH + 20f
            val imgW   = hl.contentW - 2f * imgPad
            val imgH   = heroTop + heroH - imgTop - 20f
            val imgR   = 18f

            fill(c.backgroundCard)
            rect(hl.margin + imgPad, imgTop, imgW, imgH, imgR)
            image("plane", hl.margin + imgPad, imgTop, imgW, imgH)

            // ── Stats row ─────────────────────────────────────────────
            val statsY    = heroTop + heroH + 30f
            val statCardH = 120f
            val statCardW = (hl.contentW - hl.tabGap * 2f) / 3f
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
                val sx = hl.margin + si * (statCardW + hl.tabGap)

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
            text("Recently Viewed", hl.margin, rvSectionY)

            val rvCount       = minOf(10, flightData.size)
            val rvCardGap     = 18f
            val rvCardW       = (hl.contentW - 2f * rvCardGap) / 3f
            val rvTop         = rvSectionY + 25f
            val rvImgH        = rvCardW * 0.75f
            val rvCornerR     = 14f
            val rvTotalW      = rvCount * (rvCardW + rvCardGap) - rvCardGap
            val rvMaxScroll   = max(0f, rvTotalW - hl.contentW)

            val rvZoneTop     = rvTop - 10f
            val rvZoneBottom  = rvTop + rvImgH + 80f

            if (gestures.isScrolling && !FlightDetailSheet.isOpen) {
                val pos = gestures.scrollPosition
                val inZone = pos != null &&
                        pos.second >= rvZoneTop && pos.second <= rvZoneBottom &&
                        pos.first >= hl.margin && pos.first <= hl.margin + hl.contentW
                if (inZone) rvScrollOffset += gestures.scrollDelta.first
            }
            rvScrollOffset = rvScrollOffset.coerceIn(0f, rvMaxScroll)

            for (i in 0 until rvCount) {
                val rawX = hl.margin + i * (rvCardW + rvCardGap) - rvScrollOffset
                if (rawX + rvCardW < hl.margin - 20f || rawX > hl.margin + hl.contentW + 20f) continue

                fill(c.backgroundCard)
                rect(rawX, rvTop, rvCardW, rvImgH + 70f, rvCornerR)

                fill(c.backgroundRow)
                rect(rawX + 8f, rvTop + 8f, rvCardW - 16f, rvImgH - 8f, rvCornerR - 4f)
                image("plane", rawX + 8f, rvTop + 8f, rvCardW - 16f, rvImgH - 8f)

                val textY = rvTop + rvImgH + 25f
                fill(c.textPrimary)
                textFont("roboto", 12)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[i].callsign, rawX + 10f, textY)

                fill(c.textSecondary)
                textFont("roboto", 10)
                text(flightData[i].date, rawX + 10f, textY + 28f)
            }

            if (!FlightDetailSheet.isOpen && !overviewTapConsumed && !TabTransition.active && internalTransProgress >= 1f) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (ty >= rvTop && ty <= rvTop + rvImgH + 70f) {
                        for (i in 0 until rvCount) {
                            val rawX = hl.margin + i * (rvCardW + rvCardGap) - rvScrollOffset
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
            text("Favorites", hl.margin, favSectionY)

            val ovFavIndices     = flightData.indices.filter { i -> i < Page.flightFavorites.size && Page.flightFavorites[i] }
            val favCount         = ovFavIndices.size
            val favTop           = favSectionY + 25f
            val favTotalContentW = if (favCount > 0) favCount * (rvCardW + rvCardGap) - rvCardGap else 0f
            val favMaxScroll     = max(0f, favTotalContentW - hl.contentW)

            val favZoneTop    = favTop - 10f
            val favZoneBottom = favTop + rvImgH + 80f
            if (gestures.isScrolling && !FlightDetailSheet.isOpen) {
                val pos = gestures.scrollPosition
                val inFavZone = pos != null &&
                        pos.second >= favZoneTop  && pos.second <= favZoneBottom &&
                        pos.first  >= hl.margin   && pos.first  <= hl.margin + hl.contentW
                if (inFavZone) ovFavScrollOffset += gestures.scrollDelta.first
            }
            ovFavScrollOffset = ovFavScrollOffset.coerceIn(0f, favMaxScroll)

            if (favCount == 0) {
                fill(c.textHint)
                textFont("roboto", 12)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text("No favorites yet", hl.margin, favSectionY + 50f)
            }

            for (fi in 0 until favCount) {
                val idx  = ovFavIndices[fi]
                val rawX = hl.margin + fi * (rvCardW + rvCardGap) - ovFavScrollOffset
                if (rawX + rvCardW < hl.margin - 20f || rawX > hl.margin + hl.contentW + 20f) continue

                fill(c.backgroundCard)
                rect(rawX, favTop, rvCardW, rvImgH + 70f, rvCornerR)

                fill(c.backgroundRow)
                rect(rawX + 8f, favTop + 8f, rvCardW - 16f, rvImgH - 8f, rvCornerR - 4f)
                image("plane", rawX + 8f, favTop + 8f, rvCardW - 16f, rvImgH - 8f)

                val ftextY = favTop + rvImgH + 25f
                fill(c.textPrimary)
                textFont("roboto", 12)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[idx].callsign, rawX + 10f, ftextY)

                fill(c.textSecondary)
                textFont("roboto", 10)
                text(flightData[idx].date, rawX + 10f, ftextY + 28f)
            }

            if (!FlightDetailSheet.isOpen && !overviewTapConsumed && !TabTransition.active && internalTransProgress >= 1f) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (ty >= favTop && ty <= favTop + rvImgH + 70f) {
                        for (fi in 0 until favCount) {
                            val idx  = ovFavIndices[fi]
                            val rawX = hl.margin + fi * (rvCardW + rvCardGap) - ovFavScrollOffset
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
            if (!FlightDetailSheet.isOpen && !overviewTapConsumed && !TabTransition.active && internalTransProgress >= 1f) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (tx >= btnX && tx <= btnX + btnW && ty >= btnY && ty <= btnY + btnH) {
                        sceneSwitcher.setCurrentScene(SceneId.AR.id)
                    }
                }
            }

            popMatrix()

            // ════════════════════════════════════════════════════════════
            // LAYER 2 — Fixed header + tab bar (always on top)
            // ════════════════════════════════════════════════════════════
            fill(c.background)
            rect(0, 0, screenW, hl.headerBottom)

            fill(c.textPrimary)
            textFont("roboto", 20)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Overview", screenW / 2f, hl.titleY)

            for (ti in 0 until 3) {
                val tabX = hl.margin + ti * (hl.tabW + hl.tabGap)

                if (ti == 2) {
                    fill(c.accent)
                    rect(tabX, hl.tabBarY, hl.tabW, hl.tabBarH, hl.tabR)
                    fill(c.textOnAccent)
                } else {
                    fill(c.backgroundRow)
                    rect(tabX, hl.tabBarY, hl.tabW, hl.tabBarH, hl.tabR)
                    fill(c.textSecondary)
                }

                textFont("roboto", 13)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text(tabLabels[ti], tabX + hl.tabW / 2f, hl.tabBarY + hl.tabBarH / 2f)
            }

            // Tab tap handling
            if (!FlightDetailSheet.isOpen && !TabTransition.active && internalTransProgress >= 1f) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (ty >= hl.tabBarY && ty <= hl.tabBarY + hl.tabBarH) {
                        for (ti in 0 until 3) {
                            val tabX = hl.margin + ti * (hl.tabW + hl.tabGap)
                            if (tx >= tabX && tx <= tabX + hl.tabW) {
                                when (ti) {
                                    0 -> {
                                        TabTransition.start(slideFromRight = false)
                                        sceneSwitcher.setCurrentScene(SceneId.FlightHistory.id)
                                        overviewTapConsumed = true
                                    }
                                    1 -> {
                                        internalTransDirection = -1
                                        internalTransProgress = 0f
                                        showingOverview = false
                                        overviewTapConsumed = true
                                    }
                                    2 -> { /* Already on Overview */ }
                                }
                                break
                            }
                        }
                    }
                }
            }
        }

        postRender(sceneInfo, sceneSwitcher)
    }
}