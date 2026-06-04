package edu.osu.t22.planear.scenes.pages

import android.util.Log
import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.FrameGestureDetector.FlingDirection
import edu.osu.t22.planear.graphicsEngine.EllipseMode
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.achievements.AchievementStore
import edu.osu.t22.planear.scenes.Scene
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher
import edu.osu.t22.planear.adsb.Aircraft

enum class SceneId(val id: Int) {
    AR(2), FlightHistory(3), Settings(4), Favorites(5), Achievements(6)
}

val sceneIdMap     = listOf(SceneId.AR, SceneId.FlightHistory, SceneId.Achievements, SceneId.Settings)
val navLabels      = listOf("AR View", "History", "Achievements", "Settings")
val navEmojiLabels = listOf("📷", "🕒", "🏆", "⚙️")

enum class SheetResult { ANIMATING, OPEN, DISMISSED }

object FlightDetailSheet {

    private const val ANIM_STEP = 0.06f

    private var frameCount = 0

    private var fieldScrollOffset = 0f
    private var fieldMaxScroll = 0f

    var pendingData: FlightSheetData? = null
        private set

    var isOpen: Boolean = false
        private set

    private var animProgress: Float = 0f
    private var closing: Boolean = false



    // Opens the sheet for the given flight. Safe to call from any page.
    fun open(aircraft: Aircraft) = open(FlightSheetData.fromAircraft(aircraft))

    // Called from History/Favorites with a FlightEntry
    fun open(entry: FlightEntry)  = open(FlightSheetData.fromEntry(entry))


    fun open(data: FlightSheetData) {
        pendingData = data
        animProgress = 0f
        closing = false
        isOpen = true
    }


    /** Reset all state (called internally after dismiss animation completes). */
    private fun reset() {
        pendingData = null
        animProgress = 0f
        closing = false
        isOpen = false
        frameCount = 0
        fieldScrollOffset = 0f
        fieldMaxScroll    = 0f
    }

    /**
     * Draw the flight detail bottom sheet on top of the current frame.
     * Call this at the end of any page's render() while [isOpen] is true.
     *
     * @param sceneInfo      Current frame's SceneInfo
     * @param tapConsumed    Pass true if the current frame already handled a tap
     *                       (prevents double-consuming the same gesture).
     * @return [SheetResult] indicating current animation state.
     */
    fun draw(
        sceneInfo: SceneInfo,
        tapConsumed: Boolean = false
    ): SheetResult {
        val flight = pendingData ?: return SheetResult.DISMISSED

        val screenW = sceneInfo.screenWidth
        val screenH = sceneInfo.screenHeight
        // Use the same navHeight constant that Page uses so the sheet never overlaps nav
        val navHeight = 225.0f
        val navTop = screenH - navHeight

        val gestures = sceneInfo.gestures
        val c = AppColors.current

        // --- Advance animation ---
        if (closing) {
            animProgress = (animProgress - ANIM_STEP).coerceAtLeast(0f)
            if (animProgress == 0f) {
                reset()
                return SheetResult.DISMISSED
            }
        } else {
            animProgress = (animProgress + ANIM_STEP).coerceAtMost(1f)
        }

        // Ease-out curve: fast start, settles smoothly
        val eased = 1f - (1f - animProgress) * (1f - animProgress)

        // --- Live data refresh every ~4 seconds ---
        if (flight.isLive && ++frameCount % 400 == 0) {
            val liveAircraft = SceneSwitcher.adsbManager
                .getRepository()
                .getAircraft()
                .firstOrNull { it.label == flight.callsign }

            pendingData = if (liveAircraft != null) {
                FlightSheetData.fromAircraft(liveAircraft)
            } else {
                // Plane gone — fall back to history snapshot if available
                val entry = flightData.firstOrNull { it.callsign == flight.callsign }
                if (entry != null) {
                    FlightSheetData.fromEntry(entry)
                } else {
                    flight.copy(isLive = false, altitudeFt = "N/A", headingDeg = "N/A", verticalRate = "N/A")
                }
            }
        }

        // --- Layout ---
        // Sheet covers exactly the bottom half of the screen (above the nav bar)
        val sheetH = screenH * 0.655f
        val sheetY = screenH - navHeight - sheetH // resting Y (fully open)
        val slideOffset = sheetH * (1f - eased)        // 0 when open, sheetH when hidden
        val sheetR = 32f
        val padX = screenW * 0.08f
        val rightEdge = screenW - padX

        // Favourite lookup — match by callsign against flightData
        val flightIndex = flightData.indexOfFirst { it.callsign == flight.callsign }
        val isFavorited = flightIndex >= 0 &&
                flightIndex < Page.flightFavorites.size &&
                Page.flightFavorites[flightIndex]

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)

            // ── Backdrop ──────────────────────────────────────────────────────
            fill(c.overlay, (140 * eased).toInt())
            rect(0, 0, screenW, navTop)

            // ── Sheet (translated up from below nav) ──────────────────────────
            pushMatrix()
            translate(0, slideOffset)

            // Background
            fill(c.backgroundCard)
            rect(0, sheetY + sheetR, screenW, sheetH - sheetR)
            rect(0, sheetY, screenW, sheetH * 0.4f, sheetR)

            // Drag handle pill
            fill(c.divider)
            ellipseMode(EllipseMode.CENTER)
            ellipse(screenW / 2f, sheetY + 22f, 100f, 10f)

            // "LIVE" badge or date pill
            val badgeY = sheetY + sheetH * 0.03f
            val badgeCenterY = badgeY + 14f  // half of pill height (28f)

            if (flight.isLive) {
                fill(c.accent)
                rect(padX, badgeY, 80f, 28f, 14f)
                fill(c.textOnAccent)
                textFont("roboto", 11)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text("LIVE", padX + 40f, badgeCenterY)
            } else if (flight.date != "N/A") {
                fill(c.divider)
                rect(padX, badgeY, 180f, 28f, 14f)
                fill(c.textSecondary)
                textFont("roboto", 11)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text(flight.date, padX + 90f, badgeCenterY)
            }

            val headerY = sheetY + sheetH * 0.13f

            // Callsign
            fill(c.textPrimary)
            textFont("roboto", 26)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text(flight.callsign, padX, headerY)

            // Accent underline
            fill(c.accent)
            rect(padX, headerY + 12f, 200f, 4f, 2f)


            // Favourite star
            val starX = rightEdge
            val starY = headerY
            textFont("emoji", 32)
            textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
            if (isFavorited) fill(239, 191, 4) else fill(c.divider)
            text("⭐", starX, headerY)

            // ── Close button data — anchored inside the sheet near the bottom ──────
            // data placed here so it can be reference by data fields below
            val btnW = screenW * 0.60f
            val btnH = 72f
            val btnX = (screenW - btnW) / 2f
            // Place it with a fixed margin above the nav bar top
            val btnY = screenH - navHeight - btnH - 28f

            fun dynamicFontSize(text: String, maxSize: Int = 15, minSize: Int = 9): Int {
                return when {
                    text.length <= 20 -> maxSize
                    text.length <= 30 -> maxSize - 2
                    text.length >  30 -> maxSize - 4
                    else              -> minSize
                }
            }

            // ── Data fields ───────────────────────────────────────────────────
            val fieldWindowTop = headerY + sheetH * 0.08f
            val fieldWindowBottom = btnY - 20f
            val fieldWindowH      = fieldWindowBottom - fieldWindowTop
            val fieldRowH         = 100f

            fun drawField(label: String, value: String, y: Float) {
                if (y + fieldRowH < fieldWindowTop || y > fieldWindowBottom) return

                fill(c.textHint)
                textFont("roboto", 18)
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                text("$label:  ", padX, y + 30f)

                fill(c.textPrimary)
                textFont("roboto", dynamicFontSize(value))
                textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
                // Offset value to sit after the label — adjust labelWidth if labels vary a lot in length
                val labelWidth = screenW * 0.38f
                text(value, padX + labelWidth, y + 30f)

                fill(c.divider)
                rect(padX, y + 44f, screenW - 2f * padX, 1.5f)
            }

            // Build field list dynamically
            val fields = buildList {
                add(flight.callsignLabel to flight.callsign)
                if (flight.callsignLabel != "REGISTRATION" &&
                    flight.registration != "N/A" &&
                    flight.registration != flight.callsign) {
                    add("REGISTRATION" to flight.registration)
                }
                if (flight.origin != "N/A")      add("ORIGIN"      to flight.origin)
                if (flight.destination != "N/A") add("DESTINATION" to flight.destination)
                add("ALTITUDE"  to flight.altitudeFt)
                add("VERT RATE" to flight.verticalRate)
                add("SPEED"     to flight.speedKts)
                add("TYPE"      to flight.type)
                add("HEADING"   to flight.headingDeg)
            }

            val totalContentH = fields.size * fieldRowH
            fieldMaxScroll    = (totalContentH - fieldWindowH).coerceAtLeast(0f)

            fields.forEachIndexed { index, (label, value) ->
                val y = fieldWindowTop + index * fieldRowH - fieldScrollOffset
                drawField(label, value, y)
            }

            // Scrollbar
            if (fieldMaxScroll > 0f) {
                val scrollBarX      = screenW - padX / 2f
                val scrollBarTrackH = fieldWindowH
                val scrollBarH      = (fieldWindowH / totalContentH * scrollBarTrackH).coerceAtLeast(40f)
                val scrollBarY      = fieldWindowTop + (fieldScrollOffset / fieldMaxScroll) * (scrollBarTrackH - scrollBarH)

                fill(c.divider)
                rect(scrollBarX - 3f, fieldWindowTop, 6f, scrollBarTrackH, 3f)
                fill(c.accent)
                rect(scrollBarX - 3f, scrollBarY, 6f, scrollBarH, 3f)
            }

            // Close button
            fill(c.accent)
            rect(btnX, btnY, btnW, btnH, btnH / 2f)
            fill(c.textOnAccent)
            textFont("roboto", 16)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Close", screenW / 2f, btnY + btnH / 2f)

            popMatrix()

            // ── Input handling (only when fully open and not already closing) ──
            if (!tapConsumed && !closing && animProgress >= 1f) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    val adjY = ty + slideOffset

                    // Toggle favourite
                    if (flightIndex >= 0 &&
                        tx >= starX - 70f && tx <= starX + 10f &&
                        adjY >= starY - 60f && adjY <= starY + 10f
                    ) {
                        Page.flightFavorites[flightIndex] = !Page.flightFavorites[flightIndex]
                        FlightHistoryStore.save()
                    }

                    // Close taps
                    val hitClose =
                        tx >= btnX && tx <= btnX + btnW && adjY >= btnY && adjY <= btnY + btnH
                    val hitBackdrop = adjY < sheetY
                    if (hitClose || hitBackdrop) closing = true
                }

                // Scroll field list
                if (gestures.isScrolling) {
                    val (scrollX, scrollY) = gestures.scrollPosition ?: Pair(0f, 0f)
                    val adjScrollY = scrollY + slideOffset
                    val onSheet     = adjScrollY >= sheetY && scrollX >= 0f && scrollX <= screenW
                    val swipingDown = gestures.scrollDelta.second < -30f

                    if (onSheet) {
                        val inFieldWindow = adjScrollY >= fieldWindowTop && adjScrollY <= fieldWindowBottom
                        if (inFieldWindow) {
                            // Scroll fields — negative delta = scrolling up = content moves up
                            fieldScrollOffset = (fieldScrollOffset + gestures.scrollDelta.second)
                                .coerceIn(0f, fieldMaxScroll)
                        } else if (swipingDown) {
                            closing = true
                        }
                    }
                }
            }
        }

        return if (animProgress >= 1f && !closing) SheetResult.OPEN else SheetResult.ANIMATING
    }
}

interface Page : Scene {
    companion object {
        val flightFavorites: MutableList<Boolean> = MutableList(flightData.size) { false }
    }

    val sceneId: SceneId
    val navHeight: Float get() = 225.0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {}

    fun postRender(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        // Update AR page flag — achievements only track on the AR tab.
        // ArPage.render() sets this to true; all other pages leave it untouched,
        // so we correct it here for non-AR pages.
        if (sceneId != SceneId.AR) {
            AchievementStore.isOnArPage = false
        }

        if (FlightDetailSheet.isOpen) {
            FlightDetailSheet.draw(sceneInfo)
        }

        drawNavButtons(sceneInfo, sceneSwitcher)
    }


    private fun drawNavButtons(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenWidth    = sceneInfo.screenWidth
        val screenHeight   = sceneInfo.screenHeight
        val buttonWidth    = screenWidth / navLabels.size.toFloat()
        val buttonTop      = screenHeight - navHeight
        val activeNavIndex = sceneIdMap.indexOf(sceneId).let { if (it < 0) -1 else it }
        val tapPos         = sceneInfo.gestures.singleTapUpPosition
        val gestures       = sceneInfo.gestures
        val c              = AppColors.current

        // Swipe left/right on the nav bar to switch tabs
        if (gestures.flung && !FlightDetailSheet.isOpen) {
            val startY = gestures.flingStartPosition?.second ?: Float.MAX_VALUE
            if (startY >= buttonTop) {
                val newIndex = when (gestures.flingDirection) {
                    FlingDirection.LEFT  -> (activeNavIndex + 1).coerceAtMost(sceneIdMap.lastIndex)
                    FlingDirection.RIGHT -> (activeNavIndex - 1).coerceAtLeast(0)
                    else -> activeNavIndex
                }
                if (newIndex != activeNavIndex && newIndex >= 0) {
                    // Only navigate if the scene is registered (Achievements placeholder is not yet)
                    val targetId = sceneIdMap[newIndex].id
                    try { sceneSwitcher.setCurrentScene(targetId) } catch (_: Exception) {}
                }
            }
        }

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            // Navigation Background
            fill(c.navBackground)
            rect(0, buttonTop, screenWidth, navHeight)

            for (i in navLabels.indices) {
                val offsetX = i.toFloat() * buttonWidth

                if (i == activeNavIndex) {
                    fill(c.navActive)
                    rect(offsetX, buttonTop, buttonWidth, navHeight)
                }

                // Button Text
                val (tr, tg, tb) = if (i == activeNavIndex) c.textOnAccent else c.navInactive
                fill(tr, tg, tb)

                val yOffset = 30.0f
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                textFont("roboto", 13)
                text(navLabels[i], offsetX + buttonWidth / 2.0f, screenHeight - navHeight / 2.0f + yOffset)
                textFont("emoji", 22)
                text(navEmojiLabels[i], offsetX + buttonWidth / 2.0f, screenHeight - navHeight / 2.0f - yOffset)

                // Check for and handle button press
                tapPos?.let { (tx, ty) ->
                    if (tx > offsetX && tx < offsetX + buttonWidth &&
                        ty > buttonTop && ty < buttonTop + navHeight) {
                        val targetId = sceneIdMap[i].id
                        try {
                            sceneSwitcher.setCurrentScene(targetId)
                            Log.i("Page", "Nav $i tapped - switching to scene $targetId")
                        } catch (_: Exception) {
                            Log.i("Page", "Nav $i tapped - scene $targetId not yet registered")
                        }
                    }
                }
            }

            // Small bar above buttons
            fill(c.divider)
            rect(0, screenHeight - navHeight, screenWidth, 1)
        }
    }
}