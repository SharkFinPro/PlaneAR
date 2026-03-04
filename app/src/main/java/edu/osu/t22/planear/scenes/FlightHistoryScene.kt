package edu.osu.t22.planear.scenes

import edu.osu.t22.planear.graphicsEngine.*
import edu.osu.t22.planear.models.FlightData
import edu.osu.t22.planear.models.MockFlightRepository

class FlightHistoryScene : Scene {

    private val flights: List<FlightData> = MockFlightRepository.getFlights()
    private val itemsPerPage = 14

    private var currentPage = 0
    private val totalPages: Int get() = ((flights.size - 1) / itemsPerPage) + 1

    // Detail widget state
    private var selectedFlight: FlightData? = null
    private var showWidget = false

    private var tapConsumed = false

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val w = sceneInfo.screenWidth
        val h = sceneInfo.screenHeight
        tapConsumed = false

        val r = GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()

        with(r) {
            rectMode(RectMode.CORNER)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)

            fill(245, 248, 250)
            rect(0, 0, w, h)

            val margin = w * 0.06f
            val headerY = h * 0.06f
            val titleY = headerY + 70f
            val subtitleY = titleY + 45f
            val listStartY = subtitleY + 50f
            val listEndY = h - 160f
            val rowHeight = (listEndY - listStartY) / itemsPerPage

            drawHeader(r, w, margin, headerY, titleY, subtitleY)

            if (sceneInfo.tapOccurred && !showWidget) {
                handlePaginationTap(sceneInfo, w, margin, headerY)
            }

            val pageStart = currentPage * itemsPerPage
            val pageEnd = minOf(pageStart + itemsPerPage, flights.size)
            val pageFlights = flights.subList(pageStart, pageEnd)

            for (i in pageFlights.indices) {
                val rowY = listStartY + i * rowHeight
                drawFlightRow(r, pageFlights[i], margin, rowY, w, rowHeight)

                // Tap detection on row
                if (sceneInfo.tapOccurred && !tapConsumed && !showWidget) {
                    if (sceneInfo.mouseX in margin..(w - margin) &&
                        sceneInfo.mouseY in rowY..(rowY + rowHeight)
                    ) {
                        selectedFlight = pageFlights[i]
                        showWidget = true
                        tapConsumed = true
                    }
                }
            }

            // Widget Overlay
            if (showWidget && selectedFlight != null) {
                drawDetailWidget(r, sceneInfo, w, h)
            }
        }
    }
    // helper functions
    private fun drawHeader(r: Renderer2D, w: Float, margin: Float,
                           headerY: Float, titleY: Float, subtitleY: Float) {
        r.fill(76, 175, 80)
        r.textFont("roboto", 42)
        r.textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
        r.text("Back", margin, titleY)

        r.fill(30, 30, 30)
        r.textFont("roboto", 64)
        r.textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
        r.text("Flight History", w / 2f, titleY)

        r.fill(76, 175, 80)
        r.textFont("roboto", 42)
        r.textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
        r.text("Next", w - margin, titleY)

        r.fill(120, 120, 120)
        r.textFont("roboto", 36)
        r.textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
        r.text("Page ${currentPage + 1} / $totalPages", w / 2f, subtitleY)
    }

    private fun handlePaginationTap(sceneInfo: SceneInfo, w: Float,
                                    margin: Float, headerY: Float) {
        val tapX = sceneInfo.mouseX
        val tapY = sceneInfo.mouseY
        val buttonZoneTop = headerY
        val buttonZoneBottom = headerY + 80f

        if (tapX < w * 0.25f && tapY in buttonZoneTop..buttonZoneBottom) {
            if (currentPage > 0) {
                currentPage--
                tapConsumed = true
            }
        }

        if (tapX > w * 0.75f && tapY in buttonZoneTop..buttonZoneBottom) {
            if (currentPage < totalPages - 1) {
                currentPage++
                tapConsumed = true
            }
        }
    }

    private fun drawFlightRow(r: Renderer2D, flight: FlightData,
                              margin: Float, y: Float, w: Float, rowHeight: Float) {
        val textY = y + rowHeight * 0.65f
        val dotRadius = 16f
        val rightEdge = w - margin

        // Row separator line
        r.fill(220, 220, 220)
        r.rect(margin, y + rowHeight - 2f, w - 2f * margin, 2f)

        r.fill(50, 50, 50)
        r.textFont("roboto", 38)
        r.textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
        r.text(flight.callsign, margin + 10f, textY)

        r.fill(100, 100, 100)
        r.textFont("roboto", 34)
        r.textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
        r.text(flight.date, rightEdge - dotRadius * 3f, textY)

        r.fill(76, 175, 80)
        r.ellipseMode(EllipseMode.CENTER)
        r.ellipse(rightEdge - dotRadius, y + rowHeight / 2f, dotRadius * 2f, dotRadius * 2f)
    }

    private fun drawDetailWidget(r: Renderer2D, sceneInfo: SceneInfo,
                                 w: Float, h: Float) {
        val flight = selectedFlight ?: return

        r.fill(0, 0, 0, 80)
        r.rect(0, 0, w, h)

        // Widget dimensions
        val widgetW = w * 0.82f
        val widgetH = 380f
        val widgetX = (w - widgetW) / 2f
        val widgetY = h * 0.30f

        r.fill(76, 175, 80)
        r.rect(widgetX, widgetY, widgetW, widgetH)

        r.fill(56, 142, 60)
        r.rect(widgetX, widgetY, widgetW, 80f)

        r.fill(255, 255, 255)
        r.textFont("roboto", 52)
        r.textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
        r.text(flight.callsign, w / 2f, widgetY + 40f)

        val closeX = widgetX + widgetW - 55f
        val closeY = widgetY + 15f
        val closeSize = 50f
        r.fill(255, 255, 255, 200)
        r.rect(closeX, closeY, closeSize, closeSize)
        r.fill(56, 142, 60)
        r.textFont("roboto", 42)
        r.textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
        r.text("X", closeX + closeSize / 2f, closeY + closeSize / 2f)

        val contentX = widgetX + 30f
        val contentRightX = widgetX + widgetW - 30f
        val rowStart = widgetY + 110f
        val rowGap = 70f

        r.fill(240, 248, 255)
        r.rect(contentX, rowStart, (widgetW - 70f) / 2f, 55f)
        r.rect(contentX + (widgetW - 70f) / 2f + 10f, rowStart, (widgetW - 70f) / 2f, 55f)

        r.fill(30, 30, 30)
        r.textFont("roboto", 36)
        r.textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
        r.text("Takeoff: ${flight.takeoffTime}", contentX + (widgetW - 70f) / 4f, rowStart + 27f)
        r.text("Landing: ${flight.landingTime}", contentX + (widgetW - 70f) * 3f / 4f + 10f, rowStart + 27f)

        val row2Y = rowStart + rowGap
        r.fill(240, 248, 255)
        r.rect(contentX, row2Y, widgetW - 60f, 55f)
        r.fill(30, 30, 30)
        r.textFont("roboto", 36)
        r.textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
        r.text("Plane Type: ${flight.planeType}", contentX + 15f, row2Y + 27f)

        val row3Y = row2Y + rowGap
        r.fill(240, 248, 255)
        r.rect(contentX, row3Y, widgetW - 60f, 55f)
        r.fill(30, 30, 30)
        r.textFont("roboto", 36)
        r.textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
        r.text("Airspeed: ${flight.airspeed} kts", contentX + 15f, row3Y + 27f)

        if (sceneInfo.tapOccurred && !tapConsumed) {
            val tx = sceneInfo.mouseX
            val ty = sceneInfo.mouseY

            if (tx in closeX..(closeX + closeSize) && ty in closeY..(closeY + closeSize)) {
                showWidget = false
                selectedFlight = null
                tapConsumed = true
                return
            }

            if (tx !in widgetX..(widgetX + widgetW) || ty !in widgetY..(widgetY + widgetH)) {
                showWidget = false
                selectedFlight = null
                tapConsumed = true
            }
        }
    }
}
