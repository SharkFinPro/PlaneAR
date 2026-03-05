package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.graphicsEngine.EllipseMode
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.ImageMode
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher
import kotlin.math.abs
import kotlin.math.max

class HomePage : Page {
    override val sceneId = SceneId.Home

    // Carousel state
    private var scrollOffset = 0f
    private var prevMouseX = 0f
    private var dragging = false
    private var dragStartOffset = 0f
    private var totalDragDist = 0f
    private var homeSelectedFlight = -1

    // Favorites carousel stat
    private var favScrollOffset = 0f
    private var favPrevMouseX = 0f
    private var favDragging = false
    private var favTotalDragDist = 0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        super.render(sceneInfo, sceneSwitcher)

        val screenW = sceneInfo.screenWidth
        val screenH = sceneInfo.screenHeight - navHeight

        var homeTapConsumed = false

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            // Background
            rectMode(RectMode.CORNER)
            imageMode(ImageMode.CORNER)
            fill(245, 245, 245)
            rect(0, 0, screenW, screenH)

            // Layout constants
            val margin   = screenW * 0.05f
            val cardTop  = screenH * 0.05f
            val cardW    = screenW - 2f * margin
            val cardH    = screenH * 0.38f
            val cornerR  = 30f

            // Green card
            fill(76, 175, 80)
            rect(margin, cardTop, cardW, cardH, cornerR);

            // "Flights in Air" pill button
            val btnW = cardW * 0.75f
            val btnH = 80f
            val btnX = margin + (cardW - btnW) / 2f
            val btnY = cardTop + cardH * 0.10f
            val btnR = btnH / 2f

            fill(255, 255, 255)
            rect(btnX, btnY, btnW, btnH, btnR);

            fill(76, 175, 80)
            textFont("roboto", 14)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Flights in Air", screenW / 2f, btnY + btnH / 2f)

            // Plane image panel
            val imgPad     = cardW * 0.08f
            val imgTop     = btnY + btnH + cardH * 0.06f
            val imgW       = cardW - 2f * imgPad
            val imgH       = cardTop + cardH - imgTop - cardH * 0.06f
            val imgCornerR = 20f

            fill(255, 255, 255)
            rect(margin + imgPad + imgCornerR, imgTop, imgW - 2f * imgCornerR, imgH)
            rect(margin + imgPad,              imgTop + imgCornerR, imgW, imgH - 2f * imgCornerR)
            ellipseMode(EllipseMode.CENTER)
            ellipse(margin + imgPad + imgCornerR,          imgTop + imgCornerR,         imgCornerR * 2f, imgCornerR * 2f)
            ellipse(margin + imgPad + imgW - imgCornerR,   imgTop + imgCornerR,         imgCornerR * 2f, imgCornerR * 2f)
            ellipse(margin + imgPad + imgCornerR,          imgTop + imgH - imgCornerR,  imgCornerR * 2f, imgCornerR * 2f)
            ellipse(margin + imgPad + imgW - imgCornerR,   imgTop + imgH - imgCornerR,  imgCornerR * 2f, imgCornerR * 2f)
            image("plane", margin + imgPad, imgTop, imgW, imgH)

            // Recently Viewed section
            val rvSectionY = cardTop + cardH + 80f
            fill(30, 30, 30)
            textFont("roboto", 15)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("Recently Viewed", margin, rvSectionY)

            // Carousel layout
            val rvCount   = minOf(10, flightData.size)
            val rvCardGap = 20f
            val rvCardW   = (cardW - 2f * rvCardGap) / 3f
            val rvTop     = rvSectionY + 25f
            val rvImgH    = rvCardW * 0.75f
            val rvCornerR = 12f
            val totalContentW = rvCount * (rvCardW + rvCardGap) - rvCardGap
            val maxScroll     = max(0f, totalContentW - cardW)

            // Touch zone for recently-viewed carousel
            val rvZoneTop    = rvTop - 10f
            val rvZoneBottom = rvTop + rvImgH + 80f
            val inZone = sceneInfo.mouseY >= rvZoneTop && sceneInfo.mouseY <= rvZoneBottom &&
                         sceneInfo.mouseX >= margin && sceneInfo.mouseX <= margin + cardW

            if (sceneInfo.tapOccurred && inZone && homeSelectedFlight < 0) {
                dragging       = true
                dragStartOffset = scrollOffset
                totalDragDist  = 0f
                prevMouseX     = sceneInfo.mouseX
            }
            if (dragging && sceneInfo.isTouching) {
                val delta = sceneInfo.mouseX - prevMouseX
                scrollOffset  -= delta
                totalDragDist += abs(delta)
                prevMouseX     = sceneInfo.mouseX
            }
            if (dragging && !sceneInfo.isTouching) {
                dragging = false
            }
            scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

            // Draw recently-viewed cards
            for (i in 0 until rvCount) {
                val rawX = margin + i * (rvCardW + rvCardGap) - scrollOffset
                if (rawX + rvCardW < margin - 20f || rawX > margin + cardW + 20f) continue

                fill(230, 235, 240)
                rectMode(RectMode.CORNER)
                rect(rawX + rvCornerR, rvTop, rvCardW - 2f * rvCornerR, rvImgH)
                rect(rawX,             rvTop + rvCornerR, rvCardW, rvImgH - 2f * rvCornerR)
                ellipseMode(EllipseMode.CENTER)
                fill(230, 235, 240)
                ellipse(rawX + rvCornerR,          rvTop + rvCornerR,          rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCardW - rvCornerR, rvTop + rvCornerR,          rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCornerR,          rvTop + rvImgH - rvCornerR, rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCardW - rvCornerR, rvTop + rvImgH - rvCornerR, rvCornerR * 2f, rvCornerR * 2f)
                image("plane", rawX, rvTop, rvCardW, rvImgH)

                val textY = rvTop + rvImgH + 30f
                fill(50, 50, 50)
                textFont("roboto", 10)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[i].callsign, rawX, textY)

                fill(120, 120, 120)
                textFont("roboto", 9)
                text(flightData[i].date, rawX, textY + 36f)
            }

            // Detect card tap when finger lifts
            if (!sceneInfo.isTouching && !dragging && totalDragDist < 15f && homeSelectedFlight < 0) {
                val tapX = sceneInfo.mouseX
                val tapY = sceneInfo.mouseY
                if (tapY >= rvTop && tapY <= rvTop + rvImgH + 70f) {
                    for (i in 0 until rvCount) {
                        val rawX = margin + i * (rvCardW + rvCardGap) - scrollOffset
                        if (tapX >= rawX && tapX <= rawX + rvCardW) {
                            homeSelectedFlight = i
                            homeTapConsumed    = true
                            totalDragDist      = 9999f
                            break
                        }
                    }
                }
            }

            // Favorites section
            val favSectionY = rvTop + rvImgH + 160f
            fill(30, 30, 30)
            textFont("roboto", 15)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("Favorites", margin, favSectionY)

            val homeFavIndices = flightData.indices.filter { i ->
                i < Page.flightFavorites.size && Page.flightFavorites[i]
            }
            val favCount = homeFavIndices.size

            val favTop          = favSectionY + 25f
            val favTotalContentW = if (favCount > 0) favCount * (rvCardW + rvCardGap) - rvCardGap else 0f
            val favMaxScroll    = max(0f, favTotalContentW - cardW)

            val favZoneTop    = favTop - 10f
            val favZoneBottom = favTop + rvImgH + 80f
            val inFavZone = sceneInfo.mouseY >= favZoneTop && sceneInfo.mouseY <= favZoneBottom &&
                            sceneInfo.mouseX >= margin     && sceneInfo.mouseX <= margin + cardW

            if (sceneInfo.tapOccurred && inFavZone && homeSelectedFlight < 0) {
                favDragging      = true
                favTotalDragDist = 0f
                favPrevMouseX    = sceneInfo.mouseX
            }
            if (favDragging && sceneInfo.isTouching) {
                val delta = sceneInfo.mouseX - favPrevMouseX
                favScrollOffset  -= delta
                favTotalDragDist += abs(delta)
                favPrevMouseX     = sceneInfo.mouseX
            }
            if (favDragging && !sceneInfo.isTouching) {
                favDragging = false
            }
            favScrollOffset = favScrollOffset.coerceIn(0f, favMaxScroll)

            for (fi in 0 until favCount) {
                val idx  = homeFavIndices[fi]
                val rawX = margin + fi * (rvCardW + rvCardGap) - favScrollOffset
                if (rawX + rvCardW < margin - 20f || rawX > margin + cardW + 20f) continue

                fill(230, 235, 240)
                rectMode(RectMode.CORNER)
                rect(rawX + rvCornerR, favTop, rvCardW - 2f * rvCornerR, rvImgH)
                rect(rawX,             favTop + rvCornerR, rvCardW, rvImgH - 2f * rvCornerR)
                ellipseMode(EllipseMode.CENTER)
                fill(230, 235, 240)
                ellipse(rawX + rvCornerR,           favTop + rvCornerR,          rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCardW - rvCornerR, favTop + rvCornerR,          rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCornerR,           favTop + rvImgH - rvCornerR, rvCornerR * 2f, rvCornerR * 2f)
                ellipse(rawX + rvCardW - rvCornerR, favTop + rvImgH - rvCornerR, rvCornerR * 2f, rvCornerR * 2f)
                image("plane", rawX, favTop, rvCardW, rvImgH)

                val ftextY = favTop + rvImgH + 30f
                fill(50, 50, 50)
                textFont("roboto", 10)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text(flightData[idx].callsign, rawX, ftextY)

                fill(120, 120, 120)
                textFont("roboto", 9)
                text(flightData[idx].date, rawX, ftextY + 36f)
            }

            // Detect favorites card tap when finger lifts
            if (!sceneInfo.isTouching && !favDragging && favTotalDragDist < 15f && homeSelectedFlight < 0) {
                val tapX = sceneInfo.mouseX
                val tapY = sceneInfo.mouseY
                if (tapY >= favTop && tapY <= favTop + rvImgH + 70f) {
                    for (fi in 0 until favCount) {
                        val idx  = homeFavIndices[fi]
                        val rawX = margin + fi * (rvCardW + rvCardGap) - favScrollOffset
                        if (tapX >= rawX && tapX <= rawX + rvCardW) {
                            homeSelectedFlight = idx
                            homeTapConsumed    = true
                            favTotalDragDist   = 9999f
                            break
                        }
                    }
                }
            }

            // Detail widget overlay
            if (homeSelectedFlight >= 0 && homeSelectedFlight < flightData.size) {
                val flight = flightData[homeSelectedFlight]

                fill(0, 0, 0, 80)
                rect(0, 0, screenW, screenH)

                val widgetW = screenW * 0.82f
                val widgetH = 380f
                val widgetX = (screenW - widgetW) / 2f
                val widgetY = screenH * 0.30f

                fill(76, 175, 80)
                rect(widgetX, widgetY, widgetW, widgetH)
                fill(56, 142, 60)
                rect(widgetX, widgetY, widgetW, 80f)

                fill(255, 255, 255)
                textFont("roboto", 15)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text(flight.callsign, screenW / 2f, widgetY + 40f)

                val closeX    = widgetX + widgetW - 55f
                val closeY    = widgetY + 15f
                val closeSize = 50f
                fill(255, 255, 255, 200)
                rect(closeX, closeY, closeSize, closeSize)
                fill(56, 142, 60)
                textFont("roboto", 12)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text("X", closeX + closeSize / 2f, closeY + closeSize / 2f)

                val contentX  = widgetX + 30f
                val rowStart  = widgetY + 110f
                val rowGap    = 70f
                val halfW     = (widgetW - 70f) / 2f

                fill(240, 248, 255)
                rect(contentX,              rowStart, halfW, 55f)
                rect(contentX + halfW + 10f, rowStart, halfW, 55f)
                fill(30, 30, 30)
                textFont("roboto", 10)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text("Takeoff: ${flight.takeoffTime}", contentX + halfW / 2f,          rowStart + 27f)
                text("Landing: ${flight.landingTime}", contentX + halfW * 1.5f + 10f,  rowStart + 27f)

                val row2Y = rowStart + rowGap
                fill(240, 248, 255)
                rect(contentX, row2Y, widgetW - 60f, 55f)
                fill(30, 30, 30)
                textFont("roboto", 10)
                textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
                text("Plane Type: ${flight.planeType}", contentX + 15f, row2Y + 27f)

                val row3Y = row2Y + rowGap
                fill(240, 248, 255)
                rect(contentX, row3Y, widgetW - 60f, 55f)
                fill(30, 30, 30)
                textFont("roboto", 10)
                textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
                text("Airspeed: ${flight.airspeed} kts", contentX + 15f, row3Y + 27f)

                if (sceneInfo.tapOccurred && !homeTapConsumed) {
                    val mx = sceneInfo.mouseX
                    val my = sceneInfo.mouseY
                    if (mx >= closeX && mx <= closeX + closeSize &&
                        my >= closeY && my <= closeY + closeSize) {
                        homeSelectedFlight = -1
                        homeTapConsumed    = true
                    } else if (mx < widgetX || mx > widgetX + widgetW ||
                        my < widgetY || my > widgetY + widgetH) {
                        homeSelectedFlight = -1
                        homeTapConsumed    = true
                    }
                }
            }

            // "Flights in Air" button tap > switch to AR scene
            if (sceneInfo.tapOccurred && homeSelectedFlight < 0) {
                val mx = sceneInfo.mouseX
                val my = sceneInfo.mouseY
                if (mx >= btnX && mx <= btnX + btnW &&
                    my >= btnY && my <= btnY + btnH) {
                    sceneSwitcher.setCurrentScene(SceneId.AR.ordinal.toUInt().toInt())
                }
            }
        }
    }
}