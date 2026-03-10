package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.graphicsEngine.EllipseMode
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
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
    private var selectedIndex = -1 // -1 = no selection, widget hidden

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenW = sceneInfo.screenWidth
        val screenH = sceneInfo.screenHeight - navHeight

        var tapConsumed = false

        // Build list of favorite flight indices
        val favIndices = flightData.indices.filter { i ->
            i < Page.flightFavorites.size && Page.flightFavorites[i]
        }

        val totalFavs  = favIndices.size
        val totalPages = if (totalFavs > 0) ((totalFavs - 1) / FlightHistoryPage.FLIGHTS_PER_PAGE) + 1 else 1
        currentPage = currentPage.coerceIn(0, max(0, totalPages - 1))

        val margin     = screenW * 0.06f
        val headerY    = screenH * 0.06f
        val titleY     = headerY + 70.0f
        val subtitleY  = titleY + 45.0f
        val histLinkY  = subtitleY + 45.0f
        val listStartY = histLinkY + 50.0f
        val listEndY   = screenH - 280.0f
        val rowHeight  = (listEndY - listStartY) / FlightHistoryPage.FLIGHTS_PER_PAGE

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
            text("Favorites", screenW / 2.0f, titleY)

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

            // "Flight History" link
            fill(76, 175, 80)
            textFont("roboto", 11)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("Flight History", screenW / 2.0f, histLinkY)

            val widgetShown = selectedIndex >= 0

            // Header tap handling: Back / Next pagination + Flight History link
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

                // Tap on "Flight History" link
                if (sceneInfo.mouseY >= histLinkY - 35.0f && sceneInfo.mouseY <= histLinkY + 10.0f &&
                    sceneInfo.mouseX >= screenW * 0.2f && sceneInfo.mouseX <= screenW * 0.8f) {
                    sceneSwitcher.setCurrentScene(SceneId.FlightHistory.id)
                    tapConsumed = true
                }
            }

            // Draw rows
            val pageStart = currentPage * FlightHistoryPage.FLIGHTS_PER_PAGE
            val pageEnd   = min(pageStart + FlightHistoryPage.FLIGHTS_PER_PAGE, totalFavs)

            for (fi in pageStart until pageEnd) {
                val i         = favIndices[fi] // actual flight index
                val rowIdx    = fi - pageStart
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

                // Favorite indicator star
                fill(239, 191, 4)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                textFont("emoji", 20)
                text("⭐", starX, rowY + rowHeight / 2.0f)

                // Date
                fill(100, 100, 100)
                textFont("roboto", 10)
                textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
                text(flightData[i].date, rightEdge - dotRadius * 3.0f, textY)

                // Green dot
                fill(76, 175, 80)
                ellipseMode(EllipseMode.CENTER)
                ellipse(rightEdge - dotRadius, rowY + rowHeight / 2.0f, dotRadius * 2.0f, dotRadius * 2.0f)

                // Tap on star to un-favorite
                if (sceneInfo.tapOccurred && !tapConsumed && !widgetShown) {
                    if (sceneInfo.mouseX >= starX - 30.0f && sceneInfo.mouseX <= starX + 30.0f &&
                        sceneInfo.mouseY >= rowY && sceneInfo.mouseY <= rowY + rowHeight) {
                        Page.flightFavorites[i] = false
                        tapConsumed = true
                    }
                }

                // Tap on row to show detail widget
                if (sceneInfo.tapOccurred && !tapConsumed && !widgetShown) {
                    if (sceneInfo.mouseX >= margin && sceneInfo.mouseX <= rightEdge &&
                        sceneInfo.mouseY >= rowY && sceneInfo.mouseY <= rowY + rowHeight) {
                        selectedIndex = i
                        Page.sheetAnimProgress = 0.0f
                        Page.sheetClosing = false
                        tapConsumed = true
                    }
                }
            }

            if (selectedIndex >= 0 && selectedIndex < flightData.size) {
                val result = drawFlightDetailWidget(
                    sceneInfo,
                    flightData[selectedIndex],
                    tapConsumed
                )
                if (result == SheetResult.DISMISSED) {
                    selectedIndex = -1
                    tapConsumed   = true
                }
            }
        }

        postRender(sceneInfo, sceneSwitcher);
    }
}