package edu.osu.t22.planear.scenes.pages

import android.hardware.HardwareBuffer
import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.achievements.ALL_ACHIEVEMENTS
import edu.osu.t22.planear.achievements.AchievementStore
import edu.osu.t22.planear.geo.GeoPoint
import edu.osu.t22.planear.geo.GeoUtils
import edu.osu.t22.planear.geo.Planeprojector
import edu.osu.t22.planear.geo.ScreenPoint
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.ImageMode
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.orientation.OrientationStore
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ArPage : Page {
    override val sceneId = SceneId.AR

    private var lastHb: HardwareBuffer? = null

    private var showingAchievementId: String? = null
    private var achievementAnimProgress: Float = 0.0f
    private var achievementClosing: Boolean = false

    private val initialDisplayRadius = 3000.0f
    private val layerStep = 250.0f

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val width  = sceneInfo.screenWidth
        val height = sceneInfo.screenHeight - navHeight

        AchievementStore.isOnArPage = true

        val hb          = AppSettings.hb
        val orientation = OrientationStore.data

        // ── Phone position ───────────────────────────────────────────────────
        val phoneLat: Double = orientation.x.toDouble()
        val phoneLon: Double = orientation.z.toDouble()
        val phoneAlt: Double = orientation.y.toDouble()
        val phoneGeo = GeoPoint(phoneLat, phoneLon, phoneAlt)

        if (!AppSettings.cameraIsEnabled && AppSettings.canEnableCamera && AppSettings.hasCameraPermissions) {
            AppSettings.cameraIsEnabled = true
        }

        // Metres per degree at the phone's latitude
        val metersPerDegLat = 111_320.0
        val metersPerDegLon = 111_320.0 * cos(Math.toRadians(phoneLat))

        // Shared camera parameters used for both projection calls
        // ArPage passes azimuthDeg - 90 so that 0° faces East in the rotation math.
        val camAzimuth = orientation.azimuthDeg - 90.0
        val camPitch   = orientation.pitchDeg.toDouble()
        val camRoll    = orientation.rollDeg.toDouble()
        // FOV is baked into Planeprojector (matches engine's hardcoded 50° vFOV)

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {

            // ── Camera feed / sky background ────────────────────────────────
            if (hb != null && hb != lastHb) {
                updateCameraBuffer(hb)
                lastHb = hb
            }

            if (AppSettings.cameraIsEnabled) {
                imageMode(ImageMode.CORNER)
                camera(0, 0, width, height)
            } else {
                rectMode(RectMode.CORNER)
                fill(90, 160, 255)
                rect(0, 0, width, height)
            }

            // ── 3-D view matrix ─────────────────────────────────────────────
            set3DView(
                0,
                phoneAlt,
                0,
                orientation.pitchDeg,
                orientation.azimuthDeg - 90,
                orientation.rollDeg,
                width,
                height
            )

            // ── Aircraft list (sorted nearest → farthest) ────────────────────
            val aircraftRepository = SceneSwitcher.adsbManager.getRepository()

            val sorted = aircraftRepository.getAircraft().sortedBy { p ->
                GeoUtils.distanceMeters(phoneGeo, p.getPosition())
            }

            // ── Find the aircraft most centred in the view (for history log) ─
            val yaw   = Math.toRadians(orientation.azimuthDeg - 90)
            val pitch = Math.toRadians(orientation.pitchDeg)

            val fx = cos(pitch) * cos(yaw)
            val fy = sin(pitch)
            val fz = cos(pitch) * sin(yaw)
            val flen = sqrt(fx * fx + fy * fy + fz * fz).toFloat()

            val cx = (fx / flen).toFloat()
            val cy = (fy / flen).toFloat()
            val cz = (fz / flen).toFloat()

            var bestIndex = -1
            var bestDot   = -1f

            sorted.forEachIndexed { index, p ->
                val position = p.getPosition()
                // ArPage vector convention: rawZ = –north
                val rawX = ((position.lonDeg - phoneLon) * metersPerDegLon).toFloat()
                val rawY = (position.altM - phoneAlt).toFloat()
                val rawZ = -((position.latDeg - phoneLat) * metersPerDegLat).toFloat()

                val len = sqrt(rawX * rawX + rawY * rawY + rawZ * rawZ)
                if (len > 0.01f) {
                    val dot = (rawX / len) * cx + (rawY / len) * cy + (rawZ / len) * cz
                    if (dot > bestDot) {
                        bestDot   = dot
                        bestIndex = index
                    }
                }
            }

            if (bestIndex >= 0 && bestDot > 0.95f) {
                logFlightHistory(sorted[bestIndex])
            }

            // Draw best-match aircraft first so its label renders on top
            val reordered = if (bestIndex > 0) {
                val mutable = sorted.toMutableList()
                val best = mutable.removeAt(bestIndex)
                mutable.add(0, best)
                mutable
            } else {
                sorted
            }

            // ── 3-D aircraft dots + labels ───────────────────────────────────
            // Also compute and store the normalized display-radius vectors so the
            // 2-D overlay projection can use the exact same world positions.
            data class AircraftRenderData(
                val label:         String,
                val rawLen:        Float,
                val displayRadius: Float,
                // Normalised direction × displayRadius – what the 3D engine draws
                val nx: Float,
                val ny: Float,
                val nz: Float
            )

            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)

            val renderData = reordered.mapIndexed { index, p ->
                val position = p.getPosition()

                // ArPage vector convention
                //   +rawX = East
                //   +rawY = Up
                //   +rawZ = –North  (camera forward when azimuth offset = 0)
                val rawX = ((position.lonDeg - phoneLon) * metersPerDegLon).toFloat()
                val rawY = (position.altM - phoneAlt).toFloat()
                val rawZ = -((position.latDeg - phoneLat) * metersPerDegLat).toFloat()

                val rawLen = sqrt((rawX * rawX + rawY * rawY + rawZ * rawZ).toDouble()).toFloat()

                val displayRadius = initialDisplayRadius + index * layerStep

                // Normalise direction then scale to display radius —
                // these are the exact world positions the 3D engine will render.
                val (nx, ny, nz) = if (rawLen > 0.01f) {
                    Triple(
                        rawX / rawLen * displayRadius,
                        rawY / rawLen * displayRadius,
                        rawZ / rawLen * displayRadius
                    )
                } else {
                    Triple(0f, 0f, -displayRadius)   // directly in front
                }

                val distKm  = rawLen / 1000.0
                val distStr = if (distKm < 1.0) "${"%.0f".format(rawLen)} m"
                else              "${"%.1f".format(distKm)} km"

                // Back dot (shadow)
                val dotBackRadius  = displayRadius
                val dotFrontRadius = displayRadius - layerStep * 0.4f

                val (bx, by, bz) = Triple(
                    nx / displayRadius * dotBackRadius,
                    ny / displayRadius * dotBackRadius,
                    nz / displayRadius * dotBackRadius
                )
                val (frontX, frontY, frontZ) = Triple(
                    nx / displayRadius * dotFrontRadius,
                    ny / displayRadius * dotFrontRadius,
                    nz / displayRadius * dotFrontRadius
                )

                fill(0)
                point(bx, by, bz, 270)

                fill(245)
                point(frontX, frontY, frontZ, 250)

                val textRadius = displayRadius - layerStep * 0.7f
                val tx = nx / displayRadius * textRadius
                val ty = ny / displayRadius * textRadius
                val tz = nz / displayRadius * textRadius

                textFont("roboto", 30)
                fill(42, 42, 42)
                text3D(p.label, tx, ty + 50, tz)
                text3D(distStr, tx, ty - 50, tz)

                AircraftRenderData(p.label, rawLen, displayRadius, nx, ny, nz)
            }

            // ── 2-D projected overlay (debug dots + edge arrows) ─────────────
            //
            // Both projection methods now use the SAME world-space position:
            //   (nx, ny, nz) = normalised direction × displayRadius
            // This matches exactly where the 3D engine placed the dots/labels,
            // so green 2D debug dots will align with the white 3D squares.

            data class AircraftProjection(
                val label:  String,
                val spVec:  ScreenPoint,   // ArPage-vector projection (normalised)
                val spGeo:  ScreenPoint    // ENH geo projection (ground-truth check)
            )

            val projections = reordered.mapIndexed { index, aircraft ->
                val rd = renderData[index]

                // 2-D projection uses the same normalised world position as the 3D engine.
                val spVec = Planeprojector.projectFromArVector(
                    rawX       = rd.nx,
                    rawY       = rd.ny,
                    rawZ       = rd.nz,
                    distance   = rd.rawLen,   // real distance for the distance label
                    azimuthDeg = camAzimuth,
                    pitchDeg   = camPitch,
                    rollDeg    = camRoll,
                    screenWidth  = width.toInt(),
                    screenHeight = height.toInt()
                )

                // Geo-based projection (wZ = +north internally, DO NOT negate)
                val spGeo = Planeprojector.project(
                    user       = phoneGeo,
                    aircraft   = aircraft.getPosition(),
                    azimuthDeg = camAzimuth,
                    pitchDeg   = camPitch,
                    rollDeg    = camRoll,
                    screenWidth  = width.toInt(),
                    screenHeight = height.toInt()
                )

                AircraftProjection(rd.label, spVec, spGeo)
            }

            // ── Error metric ─────────────────────────────────────────────────
            // Measure pixel distance between the two projection methods for
            // offscreen targets (where disagreement is most visible).
            var totalError  = 0f
            var errorCount  = 0
            var visibleCount    = 0
            var offscreenCount  = 0

            projections.forEach { ap ->
                if (ap.spVec.visible) visibleCount++ else offscreenCount++

                if (!ap.spVec.visible) {
                    val dx = ap.spGeo.x - ap.spVec.x
                    val dy = ap.spGeo.y - ap.spVec.y
                    totalError += sqrt(dx * dx + dy * dy)
                    errorCount++
                }
            }

            val avgError = if (errorCount > 0) totalError / errorCount else 0f

            // ── Debug dots ───────────────────────────────────────────────────
            projections.forEach { (label, spVec, _) ->
                if (spVec.visible && spVec.x.isFinite() && spVec.y.isFinite()) {
                    fill(0, 255, 0)
                    ellipse(spVec.x, spVec.y, 12f, 12f)

                    textFont("roboto", 14)
                    textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
                    text(label, spVec.x + 10f, spVec.y)
                }

                if (!spVec.visible) {
                    val edge = Planeprojector.getEdgeIndicator(
                        spVec,
                        width.toInt(),
                        height.toInt()
                    )

                    pushMatrix()
                    translate(edge.x, edge.y)
                    rotate(edge.angleDeg)

                    val s = 24f
                    val h = (kotlin.math.sqrt(3.0) / 2.0 * s).toFloat()

                    fill(255, 200, 0)
                    triangle(s / 2f, 0f, -s / 2f, -h / 2f, -s / 2f, h / 2f)

                    popMatrix()

                    fill(255, 255, 0)
                    textFont("roboto", 18)
                    textAlign(TextAlignH.LEFT, TextAlignV.CENTER)
                    text(
                        "${label}: ${spVec.x.toInt()}, ${spVec.y.toInt()}",
                        (edge.x + 20f).coerceIn(0f, width - 260f),
                        edge.y.coerceIn(20f, height - 20f)
                    )
                }
            }

            // ── HUD ──────────────────────────────────────────────────────────
            fill(0)
            textFont("roboto", 18)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("AR Scene", width / 2, 40)

            val cardinal = orientation.getCardinalDirection()

            textAlign(TextAlignH.LEFT, TextAlignV.TOP)
            textFont("roboto", 30)

            fill(0)
            text("Yaw: ${orientation.azimuthDeg.toInt()}° ($cardinal)", 50, 300)
            text("Pitch: ${orientation.pitchDeg.toInt()}°",             50, 400)
            text("Roll: ${orientation.rollDeg.toInt()}°",               50, 500)

            // Error metric display
            fill(255, 255, 0)
            text("Offscreen Avg Error: ${"%.1f".format(avgError)} px", 50, 600)
            text("Offscreen Targets: $offscreenCount",                 50, 700)
        }

        // ── Achievement popup ────────────────────────────────────────────────
        if (showingAchievementId == null) {
            val nextId = AchievementStore.popNotification()
            if (nextId != null) {
                showingAchievementId    = nextId
                achievementAnimProgress = 0.0f
                achievementClosing      = false
            }
        }

        if (showingAchievementId != null) {
            drawAchievementPopup(sceneInfo)
        }

        postRender(sceneInfo, sceneSwitcher)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Achievement popup
    // ─────────────────────────────────────────────────────────────────────────
    private fun drawAchievementPopup(sceneInfo: SceneInfo) {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight - navHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current
        val step     = 0.05f

        val achId = showingAchievementId ?: return
        val ach   = ALL_ACHIEVEMENTS.find { it.id == achId } ?: run {
            showingAchievementId = null
            return
        }

        if (achievementClosing) {
            achievementAnimProgress = (achievementAnimProgress - step).coerceAtLeast(0f)
            if (achievementAnimProgress == 0f) {
                showingAchievementId = null
                achievementClosing   = false
                return
            }
        } else {
            achievementAnimProgress = (achievementAnimProgress + step).coerceAtMost(1f)
        }

        // Ease-out quadratic
        val eased = 1f - (1f - achievementAnimProgress) * (1f - achievementAnimProgress)

        val cardW = screenW * 0.75f
        val cardH = cardW * 1.2f
        val cardX = (screenW - cardW) / 2f
        val cardR = 28f

        val targetY = (screenH - cardH) / 2f
        val startY  = -cardH - 50f
        val cardY   = startY + (targetY - startY) * eased

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)

            // Backdrop
            fill(c.overlay, (120 * eased).toInt())
            rect(0, 0, screenW, screenH)

            // Card
            fill(c.backgroundCard)
            rect(cardX, cardY, cardW, cardH, cardR)

            // Accent top stripe
            fill(c.accent)
            rect(cardX + cardR, cardY, cardW - 2f * cardR, 4f)

            // Header
            fill(c.accent)
            textFont("roboto", 12)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("🎉  ACHIEVEMENT UNLOCKED  🎉", screenW / 2f, cardY + 50f)

            // Emoji
            textFont("emoji", 48)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            fill(c.textPrimary)
            text(ach.emoji, screenW / 2f, cardY + cardH * 0.35f)

            // Name
            fill(c.textPrimary)
            textFont("roboto", 18)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text(ach.name, screenW / 2f, cardY + cardH * 0.55f)

            // Description
            fill(c.textSecondary)
            textFont("roboto", 12)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text(ach.description, screenW / 2f, cardY + cardH * 0.65f)

            // Requirement
            fill(c.textHint)
            textFont("roboto", 10)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text(ach.requirement, screenW / 2f, cardY + cardH * 0.73f)

            // Dismiss button
            val btnW = cardW * 0.55f
            val btnH = 70f
            val btnX = (screenW - btnW) / 2f
            val btnY = cardY + cardH - btnH - 30f
            val btnR = btnH / 2f

            fill(c.accent)
            rect(btnX, btnY, btnW, btnH, btnR)

            fill(c.textOnAccent)
            textFont("roboto", 14)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("Awesome!", screenW / 2f, btnY + btnH / 2f)

            // Touch handling – only active once animation is complete
            if (!achievementClosing && achievementAnimProgress >= 1f) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    if (tx >= btnX && tx <= btnX + btnW && ty >= btnY && ty <= btnY + btnH) {
                        achievementClosing = true
                    }
                    if (tx < cardX || tx > cardX + cardW || ty < cardY || ty > cardY + cardH) {
                        achievementClosing = true
                    }
                }
            }
        }
    }
}