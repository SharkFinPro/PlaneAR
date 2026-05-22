package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.FrameGestureDetector.FlingDirection
import edu.osu.t22.planear.graphicsEngine.EllipseMode
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.ImageMode
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher
import kotlin.math.max
import kotlin.math.min

class FavoritesPage : Page {
    override val sceneId = SceneId.Favorites

    private var currentPage = 0

    private var showingOverview = false
    private var rvScrollOffset = 0f
    private var ovFavScrollOffset = 0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
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
        val totalPages = if (totalFavs > 0) ((totalFavs - 1) / FlightHistoryPage.FLIGHTS_PER_PAGE) + 1 else 1
        currentPage    = currentPage.coerceIn(0, max(0, totalPages - 1))

        val margin     = screenW * 0.06f
        val headerY    = screenH * 0.06f
        val titleY     = headerY + 70.0f
        val subtitleY  = titleY + 45.0f
        val linksY     = subtitleY + 45.0f
        val listStartY = linksY + 50.0f
        val listEndY   = screenH - 280.0f
        val rowHeight  = (listEndY - listStartY) / FlightHistoryPage.FLIGHTS_PER_PAGE

        val btnZoneTop    = headerY
        val btnZoneBottom = linksY + 20.0f
        val backBtnRight  = screenW * 0.33f
        val nextBtnLeft   = screenW * 0.67f

        val canGoBack = currentPage > 0
        val canGoNext = currentPage < totalPages - 1

        if (gestures.flung && !FlightDetailSheet.isOpen) {
            when (gestures.flingDirection) {
                FlingDirection.LEFT  -> if (canGoNext) currentPage++
                FlingDirection.RIGHT -> if (canGoBack) currentPage--
                else -> {}
            }
        }

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            rectMode(RectMode.CORNER)
            fill(c.background)
            rect(0, 0, screenW, screenH)

            // Back button
            if (canGoBack) fill(c.accent)
            else           fill(c.accentDisabled)
            textFont("roboto", 13)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("< Back", margin, titleY)

            // Header: Title
            fill(c.textPrimary)
            textFont("roboto", 18)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Favorites", screenW / 2.0f, titleY)

            // Next button
            if (canGoNext) fill(c.accent)
            else           fill(c.accentDisabled)
            textFont("roboto", 13)
            textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
            text("Next >", screenW - margin, titleY)

            // Page indicator
            fill(c.textSecondary)
            textFont("roboto", 11)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Page ${currentPage + 1} / $totalPages", screenW / 2.0f, subtitleY)

            // Links row: "Flight History" and "Overview"
            fill(c.accent)
            textFont("roboto", 12)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("Flight History", margin, linksY)
            textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
            text("Overview", screenW - margin, linksY)

            val widgetShown = FlightDetailSheet.isOpen

            if (tapPos != null && !widgetShown) {
                val (tx, ty) = tapPos
                if (ty >= btnZoneTop && ty <= btnZoneBottom) {
                    if (tx <= backBtnRight && canGoBack) { currentPage--; tapConsumed = true }
                    if (tx >= nextBtnLeft  && canGoNext) { currentPage++; tapConsumed = true }
                }
                // "Flight History" link tap
                if (!tapConsumed &&
                    ty >= linksY - 35.0f && ty <= linksY + 10.0f &&
                    tx >= margin && tx <= screenW * 0.5f) {
                    sceneSwitcher.setCurrentScene(SceneId.FlightHistory.id)
                    tapConsumed = true
                }
                // "Overview" link tap
                if (!tapConsumed &&
                    ty >= linksY - 35.0f && ty <= linksY + 10.0f &&
                    tx >= screenW * 0.5f && tx <= screenW - margin) {
                    showingOverview = true
                    tapConsumed = true
                }
            }

            val pageStart = currentPage * FlightHistoryPage.FLIGHTS_PER_PAGE
            val pageEnd   = min(pageStart + FlightHistoryPage.FLIGHTS_PER_PAGE, totalFavs)

            for (fi in pageStart until pageEnd) {
                val i = favIndices[fi] // actual flight index
                val rowIdx = fi - pageStart
                val rowY = listStartY + rowIdx * rowHeight
                val textY = rowY + rowHeight * 0.65f
                val rightEdge = screenW - margin
                val dotRadius = 16.0f
                val starX = margin + 240.0f

                fill(c.divider)
                rect(margin, rowY, screenW - 2.0f * margin, 2.0f)
                rect(margin, rowY + rowHeight - 2.0f, screenW - 2.0f * margin, 2.0f)

                fill(c.textPrimary)
                textFont("roboto", 13)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[i].callsign, margin + 10.0f, textY)

                // Favorite indicator star. All items here are favorites, so the star is always gold
                fill(239, 191, 4)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                textFont("emoji", 22)
                text("⭐", starX, rowY + rowHeight / 2.0f)

                // Date
                fill(c.textSecondary)
                textFont("roboto", 11)
                textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
                text(flightData[i].date, rightEdge - dotRadius * 3.0f, textY)

                // Green dot
                fill(c.accent)
                ellipseMode(EllipseMode.CENTER)
                ellipse(
                    rightEdge - dotRadius,
                    rowY + rowHeight / 2.0f,
                    dotRadius * 2.0f,
                    dotRadius * 2.0f
                )

                if (tapPos != null && !tapConsumed && !widgetShown) {
                    val (tx, ty) = tapPos

                    // Tap on star to un-favorite
                    if (tx >= starX - 44.0f && tx <= starX + 44.0f && ty >= rowY && ty <= rowY + rowHeight) {
                        Page.flightFavorites[i] = false
                        tapConsumed = true
                    }

                    // Tap on row to show detail widget
                    if (!tapConsumed && tx >= margin && tx <= rightEdge && ty >= rowY && ty <= rowY + rowHeight) {
                        FlightDetailSheet.open(
                            Aircraft(
                                id = flightData[i].callsign,
                                callsign = flightData[i].callsign
                            )
                        )
                        tapConsumed = true
                    }
                }
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
            val cardTop = screenH * 0.05f
            val cardW   = screenW - 2f * margin
            val cardH   = screenH * 0.38f
            val cornerR = 30f

            fill(c.accent)
            rect(margin, cardTop, cardW, cardH, cornerR)

            val btnW = cardW * 0.75f
            val btnH = 80f
            val btnX = margin + (cardW - btnW) / 2f
            val btnY = cardTop + cardH * 0.10f
            val btnR = btnH / 2f

            fill(c.backgroundCard)
            rect(btnX, btnY, btnW, btnH, btnR)
            fill(c.accent)
            textFont("roboto", 14)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Flights in Air", screenW / 2f, btnY + btnH / 2f)

            val imgPad     = cardW * 0.08f
            val imgTop     = btnY + btnH + cardH * 0.06f
            val imgW       = cardW - 2f * imgPad
            val imgH       = cardTop + cardH - imgTop - cardH * 0.06f
            val imgCornerR = 20f

            fill(c.backgroundCard)
            rect(margin + imgPad + imgCornerR, imgTop, imgW - 2f * imgCornerR, imgH)
            rect(margin + imgPad, imgTop + imgCornerR, imgW, imgH - 2f * imgCornerR)
            ellipseMode(EllipseMode.CENTER)
            ellipse(margin + imgPad + imgCornerR,        imgTop + imgCornerR,        imgCornerR * 2f, imgCornerR * 2f)
            ellipse(margin + imgPad + imgW - imgCornerR, imgTop + imgCornerR,        imgCornerR * 2f, imgCornerR * 2f)
            ellipse(margin + imgPad + imgCornerR,        imgTop + imgH - imgCornerR, imgCornerR * 2f, imgCornerR * 2f)
            ellipse(margin + imgPad + imgW - imgCornerR, imgTop + imgH - imgCornerR, imgCornerR * 2f, imgCornerR * 2f)
            image("plane", margin + imgPad, imgTop, imgW, imgH)

            // "Back to list" link (top-left)
            fill(c.accent)
            textFont("roboto", 12)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("< Favorites List", margin, cardTop - 15f)

            // Recently Viewed
            val rvSectionY = cardTop + cardH + 80f
            fill(c.textPrimary)
            textFont("roboto", 15)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("Recently Viewed", margin, rvSectionY)

            val rvCount       = minOf(10, flightData.size)
            val rvCardGap     = 20f
            val rvCardW       = (cardW - 2f * rvCardGap) / 3f
            val rvTop         = rvSectionY + 25f
            val rvImgH        = rvCardW * 0.75f
            val rvCornerR     = 12f
            val totalContentW = rvCount * (rvCardW + rvCardGap) - rvCardGap
            val maxScroll     = max(0f, totalContentW - cardW)

            val rvZoneTop    = rvTop - 10f
            val rvZoneBottom = rvTop + rvImgH + 80f

            if (gestures.isScrolling && !FlightDetailSheet.isOpen) {
                val pos = gestures.scrollPosition
                val inZone = pos != null &&
                        pos.second >= rvZoneTop && pos.second <= rvZoneBottom &&
                        pos.first  >= margin    && pos.first  <= margin + cardW
                if (inZone) rvScrollOffset += gestures.scrollDelta.first
            }
            rvScrollOffset = rvScrollOffset.coerceIn(0f, maxScroll)

            for (i in 0 until rvCount) {
                val rawX = margin + i * (rvCardW + rvCardGap) - rvScrollOffset
                if (rawX + rvCardW < margin - 20f || rawX > margin + cardW + 20f) continue

                fill(c.backgroundRow)
                rectMode(RectMode.CORNER)
                rect(rawX + rvCornerR, rvTop, rvCardW - 2f * rvCornerR, rvImgH)
                rect(rawX, rvTop + rvCornerR, rvCardW, rvImgH - 2f * rvCornerR)
                ellipseMode(EllipseMode.CENTER)
                ellipse(rawX + rvCornerR,           rvTop + rvCornerR,          rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCardW - rvCornerR, rvTop + rvCornerR,          rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCornerR,           rvTop + rvImgH - rvCornerR, rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCardW - rvCornerR, rvTop + rvImgH - rvCornerR, rvCornerR * 2f, rvCornerR * 2f)
                image("plane", rawX, rvTop, rvCardW, rvImgH)

                val textY = rvTop + rvImgH + 30f
                fill(c.textPrimary)
                textFont("roboto", 12)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[i].callsign, rawX, textY)
                fill(c.textSecondary)
                textFont("roboto", 11)
                text(flightData[i].date, rawX, textY + 36f)
            }

            if (!FlightDetailSheet.isOpen) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (ty >= rvTop && ty <= rvTop + rvImgH + 70f) {
                        for (i in 0 until rvCount) {
                            val rawX = margin + i * (rvCardW + rvCardGap) - rvScrollOffset
                            if (tx >= rawX && tx <= rawX + rvCardW) {
                                FlightDetailSheet.open(Aircraft(
                                    id       = flightData[i].callsign,
                                    callsign = flightData[i].callsign
                                ))
                                overviewTapConsumed    = true
                                break
                            }
                        }
                    }
                }
            }

            // Favorites carousel
            val favSectionY = rvTop + rvImgH + 160f
            fill(c.textPrimary)
            textFont("roboto", 15)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("Favorites", margin, favSectionY)

            val ovFavIndices     = flightData.indices.filter { i -> i < Page.flightFavorites.size && Page.flightFavorites[i] }
            val favCount         = ovFavIndices.size
            val favTop           = favSectionY + 25f
            val favTotalContentW = if (favCount > 0) favCount * (rvCardW + rvCardGap) - rvCardGap else 0f
            val favMaxScroll     = max(0f, favTotalContentW - cardW)

            val favZoneTop    = favTop - 10f
            val favZoneBottom = favTop + rvImgH + 80f
            if (gestures.isScrolling && !FlightDetailSheet.isOpen) {
                val pos = gestures.scrollPosition
                val inFavZone = pos != null &&
                        pos.second >= favZoneTop  && pos.second <= favZoneBottom &&
                        pos.first  >= margin      && pos.first  <= margin + cardW
                if (inFavZone) ovFavScrollOffset += gestures.scrollDelta.first
            }
            ovFavScrollOffset = ovFavScrollOffset.coerceIn(0f, favMaxScroll)

            for (fi in 0 until favCount) {
                val idx  = ovFavIndices[fi]
                val rawX = margin + fi * (rvCardW + rvCardGap) - ovFavScrollOffset
                if (rawX + rvCardW < margin - 20f || rawX > margin + cardW + 20f) continue

                fill(c.backgroundRow)
                rectMode(RectMode.CORNER)
                rect(rawX + rvCornerR, favTop, rvCardW - 2f * rvCornerR, rvImgH)
                rect(rawX, favTop + rvCornerR, rvCardW, rvImgH - 2f * rvCornerR)
                ellipseMode(EllipseMode.CENTER)
                ellipse(rawX + rvCornerR,           favTop + rvCornerR,          rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCardW - rvCornerR, favTop + rvCornerR,          rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCornerR,           favTop + rvImgH - rvCornerR, rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCardW - rvCornerR, favTop + rvImgH - rvCornerR, rvCornerR * 2f, rvCornerR * 2f)
                image("plane", rawX, favTop, rvCardW, rvImgH)

                val ftextY = favTop + rvImgH + 30f
                fill(c.textPrimary)
                textFont("roboto", 12)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[idx].callsign, rawX, ftextY)
                fill(c.textSecondary)
                textFont("roboto", 11)
                text(flightData[idx].date, rawX, ftextY + 36f)
            }

            if (!FlightDetailSheet.isOpen) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (ty >= favTop && ty <= favTop + rvImgH + 70f) {
                        for (fi in 0 until favCount) {
                            val idx  = ovFavIndices[fi]
                            val rawX = margin + fi * (rvCardW + rvCardGap) - ovFavScrollOffset
                            if (tx >= rawX && tx <= rawX + rvCardW) {
                                FlightDetailSheet.open(Aircraft(
                                    id       = flightData[fi].callsign,
                                    callsign = flightData[fi].callsign
                                ))
                                overviewTapConsumed    = true
                                break
                            }
                        }
                    }
                }
            }


            // "Back to list" link tap
            if (!FlightDetailSheet.isOpen) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (tx >= margin && tx <= margin + 300f &&
                        ty >= cardTop - 40f && ty <= cardTop) {
                        showingOverview = false
                    }
                }
            }

            // "Flights in Air" button tap -> go to AR
            if (!FlightDetailSheet.isOpen) {
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