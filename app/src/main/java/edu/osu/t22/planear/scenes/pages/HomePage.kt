package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.graphicsEngine.EllipseMode
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.ImageMode
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher
import kotlin.math.max

class HomePage : Page {
    override val sceneId = SceneId.Home

    private var scrollOffset       = 0f
    private var homeSelectedFlight = -1
    private var favScrollOffset    = 0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight - navHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        var homeTapConsumed = false

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

            if (gestures.isScrolling && homeSelectedFlight < 0) {
                val pos = gestures.scrollPosition
                val inZone = pos != null &&
                        pos.second >= rvZoneTop && pos.second <= rvZoneBottom &&
                        pos.first  >= margin    && pos.first  <= margin + cardW
                if (inZone) scrollOffset += gestures.scrollDelta.first
            }
            scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

            for (i in 0 until rvCount) {
                val rawX = margin + i * (rvCardW + rvCardGap) - scrollOffset
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

            if (homeSelectedFlight < 0) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (ty >= rvTop && ty <= rvTop + rvImgH + 70f) {
                        for (i in 0 until rvCount) {
                            val rawX = margin + i * (rvCardW + rvCardGap) - scrollOffset
                            if (tx >= rawX && tx <= rawX + rvCardW) {
                                homeSelectedFlight     = i
                                Page.sheetAnimProgress = 0.0f
                                Page.sheetClosing      = false
                                homeTapConsumed        = true
                                break
                            }
                        }
                    }
                }
            }

            // Favorites
            val favSectionY = rvTop + rvImgH + 160f
            fill(c.textPrimary)
            textFont("roboto", 15)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("Favorites", margin, favSectionY)

            val homeFavIndices   = flightData.indices.filter { i -> i < Page.flightFavorites.size && Page.flightFavorites[i] }
            val favCount         = homeFavIndices.size
            val favTop           = favSectionY + 25f
            val favTotalContentW = if (favCount > 0) favCount * (rvCardW + rvCardGap) - rvCardGap else 0f
            val favMaxScroll     = max(0f, favTotalContentW - cardW)

            val favZoneTop    = favTop - 10f
            val favZoneBottom = favTop + rvImgH + 80f
            if (gestures.isScrolling && homeSelectedFlight < 0) {
                val pos = gestures.scrollPosition
                val inFavZone = pos != null &&
                        pos.second >= favZoneTop  && pos.second <= favZoneBottom &&
                        pos.first  >= margin      && pos.first  <= margin + cardW
                if (inFavZone) favScrollOffset += gestures.scrollDelta.first
            }
            favScrollOffset = favScrollOffset.coerceIn(0f, favMaxScroll)

            for (fi in 0 until favCount) {
                val idx  = homeFavIndices[fi]
                val rawX = margin + fi * (rvCardW + rvCardGap) - favScrollOffset
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

            if (homeSelectedFlight < 0) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (ty >= favTop && ty <= favTop + rvImgH + 70f) {
                        for (fi in 0 until favCount) {
                            val idx  = homeFavIndices[fi]
                            val rawX = margin + fi * (rvCardW + rvCardGap) - favScrollOffset
                            if (tx >= rawX && tx <= rawX + rvCardW) {
                                homeSelectedFlight     = idx
                                Page.sheetAnimProgress = 0.0f
                                Page.sheetClosing      = false
                                homeTapConsumed        = true
                                break
                            }
                        }
                    }
                }
            }

            if (homeSelectedFlight >= 0 && homeSelectedFlight < flightData.size) {
                val result = drawFlightDetailWidget(sceneInfo, flightData[homeSelectedFlight], homeTapConsumed)
                if (result == SheetResult.DISMISSED) {
                    homeSelectedFlight = -1
                    homeTapConsumed    = true
                }
            }

            if (homeSelectedFlight < 0) {
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