package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.FrameGestureDetector.FlingDirection
import edu.osu.t22.planear.graphicsEngine.EllipseMode
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher
import kotlin.math.min

class FlightHistoryPage : Page {
    override val sceneId = SceneId.FlightHistory

    companion object {
        const val FLIGHTS_PER_PAGE = 10
    }

    private var currentPage = 0


    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight - navHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        var tapConsumed = false
        val tapPos      = gestures.singleTapUpPosition

        val totalFlights = flightData.size
        val totalPages   = ((totalFlights - 1) / FLIGHTS_PER_PAGE) + 1
        currentPage      = currentPage.coerceIn(0, totalPages - 1)

        val margin     = screenW * 0.06f
        val headerY    = screenH * 0.06f
        val titleY     = headerY + 70.0f
        val subtitleY  = titleY + 45.0f
        val favLinkY   = subtitleY + 45.0f
        val listStartY = favLinkY + 50.0f
        val listEndY   = screenH - 280.0f
        val rowHeight  = (listEndY - listStartY) / FLIGHTS_PER_PAGE

        val btnZoneTop    = headerY
        val btnZoneBottom = favLinkY + 20.0f
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
            text("Flight History", screenW / 2.0f, titleY)

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

            // "Favorites" link
            fill(c.accent)
            textFont("roboto", 12)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Favorites", screenW / 2.0f, favLinkY)

            val widgetShown = FlightDetailSheet.isOpen

            // Header tap handling: Back / Next pagination + Favorites link
            if (tapPos != null && !widgetShown) {
                val (tx, ty) = tapPos
                if (ty >= btnZoneTop && ty <= btnZoneBottom) {
                    if (tx <= backBtnRight && canGoBack) { currentPage--; tapConsumed = true }
                    if (tx >= nextBtnLeft  && canGoNext) { currentPage++; tapConsumed = true }
                }

                // Tap on "Favorites" link
                if (!tapConsumed &&
                    ty >= favLinkY - 35.0f && ty <= favLinkY + 10.0f &&
                    tx >= screenW * 0.3f   && tx <= screenW * 0.7f) {
                    sceneSwitcher.setCurrentScene(SceneId.Favorites.id)
                    tapConsumed = true
                }
            }

            // Draw flight rows
            val pageStart = currentPage * FLIGHTS_PER_PAGE
            val pageEnd   = min(pageStart + FLIGHTS_PER_PAGE, totalFlights)

            for (i in pageStart until pageEnd) {
                val rowIdx    = i - pageStart
                val rowY      = listStartY + rowIdx * rowHeight
                val textY     = rowY + rowHeight * 0.65f
                val rightEdge = screenW - margin
                val dotRadius = 16.0f
                val starX     = margin + 240.0f

                // Row dividers
                fill(c.divider)
                rect(margin, rowY, screenW - 2.0f * margin, 2.0f)
                rect(margin, rowY + rowHeight - 2.0f, screenW - 2.0f * margin, 2.0f)

                // Callsign
                fill(c.textPrimary)
                textFont("roboto", 13)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[i].callsign, margin + 10.0f, textY)

                // Favorite indicator star
                val isFav = i < Page.flightFavorites.size && Page.flightFavorites[i]
                if (isFav) fill(239, 191, 4) else fill(c.divider)
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
                ellipse(rightEdge - dotRadius, rowY + rowHeight / 2.0f, dotRadius * 2.0f, dotRadius * 2.0f)

                if (tapPos != null && !tapConsumed && !widgetShown) {
                    val (tx, ty) = tapPos

                    // Tap on star to toggle favorite
                    if (tx >= starX - 44.0f && tx <= starX + 44.0f && ty >= rowY && ty <= rowY + rowHeight) {
                        if (i < Page.flightFavorites.size) {
                            Page.flightFavorites[i] = !Page.flightFavorites[i]
                            FlightHistoryStore.save()
                        }
                        tapConsumed = true
                    }

                    // Tap on row (excluding star) to open detail widget
                    if (!tapConsumed && tx >= margin && tx <= rightEdge && ty >= rowY && ty <= rowY + rowHeight) {
                        FlightDetailSheet.open(flightData[i])
                        tapConsumed            = true
                    }
                }
            }
        }

        postRender(sceneInfo, sceneSwitcher)
    }
}