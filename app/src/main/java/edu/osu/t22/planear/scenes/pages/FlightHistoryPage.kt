package edu.osu.t22.planear.scenes.pages

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
        const val FLIGHTS_PER_PAGE = 14
    }

    private var currentPage = 0
    private var selectedIndex = -1 // -1 = no selection, widget hidden

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        super.render(sceneInfo, sceneSwitcher)

        val screenW = sceneInfo.screenWidth
        val screenH = sceneInfo.screenHeight - navHeight

        var tapConsumed = false

        val totalFlights = flightData.size
        val totalPages = ((totalFlights - 1) / FLIGHTS_PER_PAGE) + 1
        currentPage = currentPage.coerceIn(0, totalPages - 1)

        val margin      = screenW * 0.06f
        val headerY     = screenH * 0.06f
        val titleY      = headerY + 70.0f
        val subtitleY   = titleY + 45.0f
        val favLinkY    = subtitleY + 45.0f
        val listStartY  = favLinkY + 50.0f
        val listEndY    = screenH - 280.0f
        val rowHeight   = (listEndY - listStartY) / FLIGHTS_PER_PAGE

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            rectMode(RectMode.CORNER)
            fill(245, 248, 250)
            rect(0, 0, screenW, screenH)

            // Header: Back
            fill(76, 175, 80)
            textFont("roboto", 12)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("Back", margin, titleY)

            // Header: Title
            fill(30, 30, 30)
            textFont("roboto", 18)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Flight History", screenW / 2.0f, titleY)

            // Header: Next
            fill(76, 175, 80)
            textFont("roboto", 12)
            textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
            text("Next", screenW - margin, titleY)

            // Page indicator
            fill(120, 120, 120)
            textFont("roboto", 10)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Page ${currentPage + 1} / $totalPages", screenW / 2.0f, subtitleY)

            // "Favorites" link
            fill(76, 175, 80)
            textFont("roboto", 11)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Favorites", screenW / 2.0f, favLinkY)

            val widgetShown = selectedIndex >= 0

            // Header tap handling: Back / Next pagination + Favorites link
            if (sceneInfo.tapOccurred && !widgetShown) {
                val btnTop    = headerY
                val btnBottom = headerY + 80.0f
                if (sceneInfo.mouseY >= btnTop && sceneInfo.mouseY <= btnBottom) {
                    if (sceneInfo.mouseX < screenW * 0.25f && currentPage > 0) {
                        currentPage--
                        tapConsumed = true
                    }
                    if (sceneInfo.mouseX > screenW * 0.75f && currentPage < totalPages - 1) {
                        currentPage++
                        tapConsumed = true
                    }
                }

                // Tap on "Favorites" link
                if (sceneInfo.mouseY >= favLinkY - 35.0f && sceneInfo.mouseY <= favLinkY + 10.0f &&
                    sceneInfo.mouseX >= screenW * 0.3f && sceneInfo.mouseX <= screenW * 0.7f) {
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
                val starHalf  = 18.0f

                // Row dividers
                fill(200, 200, 200)
                rect(margin, rowY, screenW - 2.0f * margin, 2.0f)
                rect(margin, rowY + rowHeight - 2.0f, screenW - 2.0f * margin, 2.0f)

                // Callsign
                fill(50, 50, 50)
                textFont("roboto", 11)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[i].callsign, margin + 10.0f, textY)

                // Favorite indicator square (purple = favorited, grey = not)
                if (i < Page.flightFavorites.size && Page.flightFavorites[i]) {
                    fill(128, 0, 128)
                } else {
                    fill(180, 180, 180)
                }
                rectMode(RectMode.CORNER)
                rect(starX - starHalf, rowY + rowHeight / 2.0f - starHalf, starHalf * 2.0f, starHalf * 2.0f)

                // Date
                fill(100, 100, 100)
                textFont("roboto", 10)
                textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
                text(flightData[i].date, rightEdge - dotRadius * 3.0f, textY)

                // Green dot
                fill(76, 175, 80)
                ellipseMode(EllipseMode.CENTER)
                ellipse(rightEdge - dotRadius, rowY + rowHeight / 2.0f, dotRadius * 2.0f, dotRadius * 2.0f)

                // Tap on star to toggle favorite
                if (sceneInfo.tapOccurred && !tapConsumed && !widgetShown) {
                    if (sceneInfo.mouseX >= starX - 30.0f && sceneInfo.mouseX <= starX + 30.0f &&
                        sceneInfo.mouseY >= rowY && sceneInfo.mouseY <= rowY + rowHeight) {
                        if (i < Page.flightFavorites.size) {
                            Page.flightFavorites[i] = !Page.flightFavorites[i]
                        }
                        tapConsumed = true
                    }
                }

                // Tap on row (excluding star) to open detail widget
                if (sceneInfo.tapOccurred && !tapConsumed && !widgetShown) {
                    if (sceneInfo.mouseX >= margin && sceneInfo.mouseX <= rightEdge &&
                        sceneInfo.mouseY >= rowY && sceneInfo.mouseY <= rowY + rowHeight) {
                        selectedIndex = i
                        Page.sheetAnimProgress = 0.0f
                        tapConsumed = true
                    }
                }
            }

            // Detail widget overlay
            if (selectedIndex >= 0 && selectedIndex < totalFlights) {
                val dismissed = drawFlightDetailWidget(
                    sceneInfo,
                    flightData[selectedIndex],
                    tapConsumed
                )
                if (dismissed) {
                    selectedIndex = -1
                    tapConsumed   = true
                }
            }
        }
    }
}