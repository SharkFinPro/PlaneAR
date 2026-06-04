package edu.osu.t22.planear.scenes.pages

import android.hardware.HardwareBuffer
import android.util.Log
import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.achievements.ALL_ACHIEVEMENTS
import edu.osu.t22.planear.achievements.AchievementStore
import edu.osu.t22.planear.adsb.AdsbRepository
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

    // Achievement popup state
    private var showingAchievementId: String? = null
    private var achievementAnimProgress: Float = 0.0f
    private var achievementClosing: Boolean = false

    private val initialDisplayRadius = 3000.0f

    private val layerStep = 250.0f

    private var waitingOnMousePickingResult: Boolean = false
    private var selectedId: Long = 0
    private var lastConsumedTapPos: Pair<Float, Float>? = null

    val c = AppColors.current

    private var cachedSorted: List<edu.osu.t22.planear.adsb.Aircraft> = emptyList()
    private var lastSortTimeMs: Long = 0L

    private var filteredMatrix = FloatArray(9) { if (it % 4 == 0) 1f else 0f }

    private var filteredYaw   = 0.0
    private var filteredPitch = 0.0
    private var filteredRoll  = 0.0

    private var orientationInitialized = false
    private var lastFrameTimeNs = 0L

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val width = sceneInfo.screenWidth
        val height = sceneInfo.screenHeight - navHeight

        // Mark that we are on the AR page that enables achievement tracking
        AchievementStore.isOnArPage = true

        val orientation = OrientationStore.data

        val nowNs = System.nanoTime()

        val dt = if (lastFrameTimeNs == 0L) {
            1f / 60f
        } else {
            ((nowNs - lastFrameTimeNs) / 1_000_000_000.0f)
                .coerceIn(0.001f, 0.1f)
        }

        lastFrameTimeNs = nowNs

        val R = orientation.rotationMatrix

        if (!orientationInitialized) {
            filteredMatrix = R.copyOf()
            filteredYaw   = orientation.azimuthDeg
            filteredPitch = orientation.pitchDeg
            filteredRoll  = orientation.rollDeg
            orientationInitialized = true
        } else {
            val tau   = 0.20
            val alpha = (1.0 - kotlin.math.exp(-dt.toDouble() / tau)).toFloat()
            filteredMatrix = slerpMatrix(filteredMatrix, R, alpha)

            val yawTarget   = applyAngleDeadband(filteredYaw,   orientation.azimuthDeg, 0.25)
            val pitchTarget = applyDeadband(filteredPitch, orientation.pitchDeg,  0.20)
            val rollTarget  = applyDeadband(filteredRoll,  orientation.rollDeg,   0.20)

            filteredYaw   = lerpAngle(filteredYaw,   yawTarget,   alpha.toDouble())
            filteredPitch += (pitchTarget - filteredPitch) * alpha
            filteredRoll  += (rollTarget  - filteredRoll)  * alpha
        }

        val phoneLat: Double = orientation.x.toDouble()
        val phoneLon: Double = orientation.z.toDouble()
        val phoneAlt: Double = orientation.y.toDouble()
        val phoneGeo = GeoPoint(phoneLat, phoneLon, phoneAlt)

        if (!AppSettings.cameraIsEnabled && AppSettings.canEnableCamera && AppSettings.hasCameraPermissions) {
            AppSettings.cameraIsEnabled = true
        }

        val metersPerDegLat = 111_320.0
        val metersPerDegLon = 111_320.0 * cos(Math.toRadians(phoneLat))

        val camAzimuth = orientation.azimuthDeg - 90.0
        val camPitch   = orientation.pitchDeg.toDouble()
        val camRoll    = orientation.rollDeg.toDouble()

        val tapPos = sceneInfo.gestures.touchDownPosition

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            tapPos?.let { (tx, ty) ->
                if (tapPos != lastConsumedTapPos &&
                    tx > 0 && tx < width &&
                    ty > 0 && ty < height &&
                    !waitingOnMousePickingResult) {

                    if (!FlightDetailSheet.isOpen) {
                        requestMousePicking(tx, ty)

                        waitingOnMousePickingResult = true
                    }

                    lastConsumedTapPos = tapPos

                    sceneInfo.gestures.markTouchDownConsumed()
                }
            }

            // Sort aircraft nearest-first so closer planes draw on top
            val nowMs = System.currentTimeMillis()
            if (nowMs - lastSortTimeMs >= 1_000L) {
                val aircraftRepository = SceneSwitcher.adsbManager.getRepository()
                cachedSorted = aircraftRepository.getAircraft().sortedBy { p ->
                    GeoUtils.distanceMeters(phoneGeo, p.getPosition())
                }
                lastSortTimeMs = nowMs
            }
            val sorted = cachedSorted

            if (waitingOnMousePickingResult && hasNewMousePickingResult()) {
                selectedId = getMousePickingResult()

                if (selectedId != 0L) {
                    val selectedAircraft = sorted.find {
                        (it.id.toLongOrNull(16) ?: 0L) == selectedId
                    }

                    selectedAircraft?.let {
                        FlightDetailSheet.open(it)
                    }
                }

                lastConsumedTapPos = null

                waitingOnMousePickingResult = false
            }

            if (AppSettings.cameraIsEnabled) {
                updateCameraTexture()

                imageMode(ImageMode.CORNER)
                camera(0, 0, width, height)
            } else {
                rectMode(RectMode.CORNER)
                fill(AppColors.current.skyBackground)
                rect(0, 0, width, height)
            }

            set3DViewMatrix(
                filteredMatrix,
                width,
                height
            )

            val metersPerDegLat = 111_320.0
            val metersPerDegLon = 111_320.0 * cos(Math.toRadians(phoneLat))

            val forward = floatArrayOf(
                -filteredMatrix[2],
                -filteredMatrix[5],
                -filteredMatrix[8]
            )

            val cx = forward[0]
            val cy = forward[2]
            val cz = -forward[1]

            var bestIndex = -1
            var bestDot = -1f

            sorted.forEachIndexed { index, p ->
                val position = p.getPosition()
                val rawX = ((position.lonDeg - phoneLon) * metersPerDegLon).toFloat()
                val rawY = (position.altM - phoneAlt).toFloat()
                val rawZ = -((position.latDeg - phoneLat) * metersPerDegLat).toFloat()

                val len = sqrt(rawX * rawX + rawY * rawY + rawZ * rawZ)
                if (len > 0.01f) {
                    val dot = (rawX / len) * cx + (rawY / len) * cy + (rawZ / len) * cz
                    if (dot > bestDot) {
                        bestDot = dot
                        bestIndex = index
                    }
                }
            }

            if (bestIndex >= 0 && bestDot > 0.95f) {
                logFlightHistory(sorted[bestIndex])
            }

            val closestId = if (bestIndex >= 0) {
                sorted[bestIndex].id.toLongOrNull(16) ?: 0L
            } else 0L

            val reordered = sorted.sortedWith(
                compareByDescending { aircraft ->
                    val id = aircraft.id.toLongOrNull(16) ?: 0L

                    when (id) {
                        selectedId -> 2
                        closestId  -> 1
                        else        -> 0
                    }
                }
            )

            data class AircraftRenderData(
                val label:         String,
                val rawLen:        Float,
                val displayRadius: Float,
                val nx: Float,
                val ny: Float,
                val nz: Float
            )

            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)

            val renderData = reordered.mapIndexed { index, p ->
                // Raw direction vector from phone to aircraft (East / Up / North in meters)
                val position = p.getPosition()

                val rawX = ((position.lonDeg - phoneLon) * metersPerDegLon).toFloat()
                val rawY = (position.altM - phoneAlt).toFloat()
                val rawZ = -((position.latDeg - phoneLat) * metersPerDegLat).toFloat()

                // Distance in the XZ plane + full 3-D magnitude
                val rawLen = sqrt((rawX * rawX + rawY * rawY + rawZ * rawZ).toDouble()).toFloat()

                // Avoid division by zero for aircraft exactly at phone position
                val displayRadius = initialDisplayRadius + index * layerStep

                val (nx, ny, nz) = if (rawLen > 0.01f) {
                    Triple(
                        rawX / rawLen * displayRadius,
                        rawY / rawLen * displayRadius,
                        rawZ / rawLen * displayRadius
                    )
                } else {
                    // Fallback: place directly in front of the camera
                    Triple(0f, 0f, -displayRadius)
                }

                val distKm  = rawLen / 1000.0
                val distStr = if (distKm < 1.0) "${"%.0f".format(rawLen)} m"
                else "${"%.1f".format(distKm)} km"

                val dotFrontRadius = displayRadius - layerStep * 0.4f

                val (fx, fy, fz) = Triple(nx / displayRadius * dotFrontRadius, ny / displayRadius * dotFrontRadius, nz / displayRadius * dotFrontRadius)

                val id = p.id.toLongOrNull(16) ?: 0L

                // ── Aircraft billboard card ───────────────────────────────────
                // One draw call: the frag shader handles the border itself,
                // so the old black-background point() is no longer needed.
                // aspectX = 2.2 → card is 2.2× wider than it is tall.
                val cardHalfH = 200f   // half-height in world units (= size arg)
                val cardAspect = 1.5f

                pointAspect(cardAspect, 1.0f)
                if (id == selectedId) {
                    fill(150, 245, 150)
                } else {
                    fill(245)
                }
                point(fx, fy, fz, cardHalfH)

                if (waitingOnMousePickingResult) {
                    // Mouse picking hitbox — use the card's true half-width so the
                    // full horizontal extent is clickable.
                    mousePickingPoint(fx, fy, fz, cardHalfH, id)
                }

                // ── Compass sits on the left side of the card ─────────────────
                // offsetX = -(cardHalfH * cardAspect) centres it at the left edge;
                // offsetY = 0 vertically centres it on the card.
                val headingRad = Math.toRadians(p.headingDegrees ?: 0.0).toFloat()
                val compassSize = cardHalfH * 0.5f
                val compassOffX = -(cardHalfH / 2.25 * cardAspect) + compassSize * 0.05f
                compass(
                    fx * 0.99,
                    fy * 0.99,
                    fz * 0.99,
                    compassSize,
                    compassOffX,
                    -compassSize * 0.6,
                    headingRad,
                    1f
                )

                // ── Text: callsign above separator line, distance below ────────
                val textRadius = displayRadius - layerStep * 0.7
                val tx = nx / displayRadius * textRadius
                val ty = ny / displayRadius * textRadius
                val tz = nz / displayRadius * textRadius

                val textLeftShift = cardHalfH * 0.4f
                val textRightShift = cardHalfH * 0.6f
                val distScale = 0.98

                // 1. Manually calculate Screen-Right vector (Perpendicular to Forward vector on the horizontal XZ plane)
                // This ensures that "Right" means moving toward the right side of your phone screen.
                val rLen = sqrt(cx * cx + cz * cz)
                val rx = if (rLen > 0.01f) cz / rLen else 1f
                val ry = 0f
                val rz = if (rLen > 0.01f) -cx / rLen else 0f

                // 2. Cross product (Forward × Right) to get a true Screen-Up vector
                // This is mathematically guaranteed to point toward the top edge of your screen, even at the zenith.
                val ux = cy * rz - cz * ry
                val uy = cz * rx - cx * rz
                val uz = cx * ry - cy * rx

                // Normalize the constructed Up vector
                val uLen = sqrt(ux * ux + uy * uy + uz * uz)
                val uxNorm = if (uLen > 0.01f) ux / uLen else 0f
                val uyNorm = if (uLen > 0.01f) uy / uLen else 1f
                val uzNorm = if (uLen > 0.01f) uz / uLen else 0f

                // 3. Render Top Text (Callsign) shifted UP and RIGHT relative to screen space
                textFont("roboto", 16);
                fill(230, 232, 240)
                text3D(
                    p.label,
                    (tx + rx * textLeftShift + uxNorm * cardHalfH * 0.6f) * distScale,
                    (ty + ry * textLeftShift + uyNorm * cardHalfH * 0.6f) * distScale,
                    (tz + rz * textLeftShift + uzNorm * cardHalfH * 0.6f) * distScale
                )

                // 4. Render Bottom Text (Distance) shifted DOWN and LEFT relative to screen space
                textSize(14);
                fill(160, 165, 185)
                text3D(
                    distStr,
                    (tx - rx * textRightShift - uxNorm * cardHalfH * 0.25f) * distScale,
                    (ty - ry * textRightShift - uyNorm * cardHalfH * 0.25f) * distScale,
                    (tz - rz * textRightShift - uzNorm * cardHalfH * 0.25f) * distScale
                )

                AircraftRenderData(p.label, rawLen, displayRadius, nx, ny, nz)
            }

            data class AircraftProjection(
                val label:  String,
                val spVec:  ScreenPoint,
                val spGeo:  ScreenPoint
            )

            val projections = reordered.mapIndexed { index, aircraft ->
                val rd = renderData[index]

                val spVec = Planeprojector.projectFromArVector(
                    rawX       = rd.nx,
                    rawY       = rd.ny,
                    rawZ       = rd.nz,
                    distance   = rd.rawLen,
                    azimuthDeg = camAzimuth,
                    pitchDeg   = camPitch,
                    rollDeg    = camRoll,
                    screenWidth  = width.toInt(),
                    screenHeight = height.toInt()
                )

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

            var totalError = 0f
            var errorCount = 0
            var visibleCount = 0
            var offscreenCount = 0

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

            projections.forEach { (label, spVec, _) ->
                if (!spVec.visible) {
                    if (spVec.camZ <= 0f) return@forEach

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

                    fill(c.navActive)
                    triangle(s / 2f, 0f, -s / 2f, -h / 2f, -s / 2f, h / 2f)

                    popMatrix()
                }
            }

            drawHud(sceneInfo.enginePtr, width, height)
        }

        // Check for new achievement notifications
        if (showingAchievementId == null) {
            val nextId = AchievementStore.popNotification()
            if (nextId != null) {
                showingAchievementId   = nextId
                achievementAnimProgress = 0.0f
                achievementClosing      = false
            }
        }

        // Draw achievement popup if active
        if (showingAchievementId != null) {
            drawAchievementPopup(sceneInfo)
        }

        postRender(sceneInfo, sceneSwitcher)
    }

    /**
     * Draw the achievement unlocked popup card, centered on screen.
     * Shows the achievement emoji, name, and a dismiss button.
     * Slides in from top with eased animation.
     */
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

        // Animate
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

        val eased = 1f - (1f - achievementAnimProgress) * (1f - achievementAnimProgress)

        // Card dimensions
        val cardW = screenW * 0.75f
        val cardH = cardW * 1.2f
        val cardX = (screenW - cardW) / 2f
        val cardR = 28f

        // Slide down from top
        val targetY = (screenH - cardH) / 2f
        val startY  = -cardH - 50f
        val cardY   = startY + (targetY - startY) * eased

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)

            // Semi-transparent backdrop
            fill(c.overlay, (120 * eased).toInt())
            rect(0, 0, screenW, screenH)

            // Card background
            fill(c.backgroundCard)
            rect(cardX, cardY, cardW, cardH, cardR)

            // Accent top border
            fill(c.accent)
            rect(cardX + cardR, cardY, cardW - 2f * cardR, 4f)

            // "Achievement Unlocked!" label
            fill(c.accent)
            textFont("roboto", 12)
            textAlign(TextAlignH.CENTER, TextAlignV.BASELINE)
            text("🎉  ACHIEVEMENT UNLOCKED  🎉", screenW / 2f, cardY + 50f)

            // Large emoji
            textFont("emoji", 48)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            fill(c.textPrimary)
            text(ach.emoji, screenW / 2f, cardY + cardH * 0.35f)

            // Achievement name
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

            // Close button
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

            // Handle close tap
            if (!achievementClosing && achievementAnimProgress >= 1f) {
                gestures.singleTapUpPosition?.let { (tx, ty) ->
                    // Tap on button
                    if (tx >= btnX && tx <= btnX + btnW && ty >= btnY && ty <= btnY + btnH) {
                        achievementClosing = true
                    }
                    // Tap outside card (backdrop dismiss)
                    if (tx < cardX || tx > cardX + cardW || ty < cardY || ty > cardY + cardH) {
                        achievementClosing = true
                    }
                }
            }
        }
    }

    /**
     * Draws the 2D aviation HUD overlay: compass tape.
     * Uses smoothed Euler-angle fields to avoid the gimbal/axis issues present in the matrix path.
     */
    private fun drawHud(enginePtr: Long, width: Float, height: Float) {
        val hudYaw = filteredYaw.toFloat()

        with(GraphicsEngineWrapper(enginePtr).getRenderer2D()) {
            val hud = AppColors.current

            // ── Compass tape ──────────────────────────────────────────────────
            // tapeY clears punch-hole / notch cameras that sit inside the top ~80 px.
            val tapeH     = 55f
            val tapeY     = 148f + tapeH / 2f
            val tapeW     = width * 0.86f
            val tapeLeft  = (width - tapeW) / 2f
            val tapeRight = tapeLeft + tapeW

            // Background pill
            rectMode(RectMode.CORNER)
            fill(hud.navBackground, 200)
            rect(tapeLeft, tapeY - tapeH / 2f, tapeW, tapeH, tapeH / 2f)

            // Degree-per-pixel ratio: full 360° across tapeW * 1.5 virtual span
            val degPerPx   = 360f / (tapeW * 1.5f)
            val headingDeg = hudYaw
            val screenCx   = width / 2f

            val cardinals = mapOf(
                0f   to "N",
                45f  to "NE",
                90f  to "E",
                135f to "SE",
                180f to "S",
                225f to "SW",
                270f to "W",
                315f to "NW"
            )

            // Cardinal labels are ~20 px wide at size 16; keep them fully inside the pill.
            val labelMargin = 22f

            // Draw tick marks and cardinal labels
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)

            for (deg in 0..359 step 5) {
                val deltaAngle = ((deg.toFloat() - headingDeg + 540f) % 360f) - 180f
                val tickX      = screenCx + deltaAngle / degPerPx

                if (tickX < tapeLeft + 4f || tickX > tapeRight - 4f) continue

                val isMajor  = deg % 45 == 0
                val isMedium = deg % 15 == 0 && !isMajor
                val tickHalf = when {
                    isMajor  -> tapeH * 0.40f
                    isMedium -> tapeH * 0.26f
                    else     -> tapeH * 0.16f
                }
                val tickAlpha = when {
                    isMajor  -> 220
                    isMedium -> 160
                    else     -> 100
                }

                fill(hud.textPrimary, tickAlpha)
                rect(tickX - 0.75f, tapeY - tickHalf, 1.5f, tickHalf * 2f)

                if (tickX >= tapeLeft + labelMargin && tickX <= tapeRight - labelMargin) {
                    cardinals[deg.toFloat()]?.let { label ->
                        fill(hud.textPrimary, 230)
                        textFont("roboto", 16)
                        text(label, tickX, tapeY)
                    }
                }
            }

            // Center heading cursor (inverted triangle notch at bottom of tape)
            val cursorH = 12f
            val cursorW = 13f

            fill(hud.accent)
            triangle(
                screenCx,           tapeY + tapeH / 2f,
                screenCx - cursorW, tapeY + tapeH / 2f - cursorH,
                screenCx + cursorW, tapeY + tapeH / 2f - cursorH
            )
        }
    }

    private fun lerpAngle(current: Double, target: Double, alpha: Double): Double {
        var delta = target - current
        while (delta > 180.0)  delta -= 360.0
        while (delta < -180.0) delta += 360.0
        return current + delta * alpha
    }

    private fun applyAngleDeadband(current: Double, target: Double, thresholdDeg: Double): Double {
        var delta = target - current
        while (delta > 180.0)  delta -= 360.0
        while (delta < -180.0) delta += 360.0
        return if (kotlin.math.abs(delta) < thresholdDeg) current else target
    }

    private fun applyDeadband(current: Double, target: Double, threshold: Double): Double =
        if (kotlin.math.abs(target - current) < threshold) current else target

    private fun slerpMatrix(a: FloatArray, b: FloatArray, t: Float): FloatArray {
        val result = FloatArray(9)
        for (i in 0..8) result[i] = a[i] + (b[i] - a[i]) * t

        fun normalize3(i0: Int, i1: Int, i2: Int) {
            val len = sqrt(result[i0] * result[i0] + result[i1] * result[i1] + result[i2] * result[i2])
            if (len > 1e-6f) { result[i0] /= len; result[i1] /= len; result[i2] /= len }
        }

        normalize3(0, 1, 2)

        result[3] -= result[0] * (result[0]*result[3] + result[1]*result[4] + result[2]*result[5])
        result[4] -= result[1] * (result[0]*result[3] + result[1]*result[4] + result[2]*result[5])
        result[5] -= result[2] * (result[0]*result[3] + result[1]*result[4] + result[2]*result[5])
        normalize3(3, 4, 5)

        result[6] = result[1]*result[5] - result[2]*result[4]
        result[7] = result[2]*result[3] - result[0]*result[5]
        result[8] = result[0]*result[4] - result[1]*result[3]

        return result
    }
}
