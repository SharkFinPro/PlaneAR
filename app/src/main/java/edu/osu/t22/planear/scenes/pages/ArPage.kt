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

    private val layerStep = 100.0f

    private var waitingOnMousePickingResult: Boolean = false
    private var selectedId: Long = 0
    private var lastConsumedTapPos: Pair<Float, Float>? = null

    val c = AppColors.current

    private var cachedSorted: List<edu.osu.t22.planear.adsb.Aircraft> = emptyList()
    private var lastSortTimeMs: Long = 0L

    private var filteredYaw = 0.0
    private var filteredPitch = 0.0
    private var filteredRoll = 0.0

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

        if (!orientationInitialized) {
            filteredYaw = orientation.azimuthDeg
            filteredPitch = orientation.pitchDeg
            filteredRoll = orientation.rollDeg

            orientationInitialized = true
        } else {

            val yawTarget = applyAngleDeadband(
                filteredYaw,
                orientation.azimuthDeg,
                0.25
            )

            val pitchTarget = applyDeadband(
                filteredPitch,
                orientation.pitchDeg,
                0.20
            )

            val rollTarget = applyDeadband(
                filteredRoll,
                orientation.rollDeg,
                0.20
            )

            val tau = 0.20
            val alpha = 1.0 - kotlin.math.exp(-dt.toDouble() / tau)

            filteredYaw =
                lerpAngle(filteredYaw, yawTarget, alpha)

            filteredPitch +=
                (pitchTarget - filteredPitch) * alpha

            filteredRoll +=
                (rollTarget - filteredRoll) * alpha
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
                fill(90, 160, 255)
                rect(0, 0, width, height)
            }

            set3DView(
                0,
                phoneAlt,
                0,
                filteredPitch,
                filteredYaw  - 90,
                filteredRoll,
                width,
                height
            )

            val metersPerDegLat = 111_320.0
            val metersPerDegLon = 111_320.0 * cos(Math.toRadians(phoneLat))

            val yaw = Math.toRadians((filteredYaw - 90))
            val pitch = Math.toRadians(filteredPitch)

            val fx = cos(pitch) * cos(yaw)
            val fy = sin(pitch)
            val fz = cos(pitch) * sin(yaw)

            val flen = sqrt(fx*fx + fy*fy + fz*fz).toFloat()

            val cx = (fx / flen).toFloat()
            val cy = (fy / flen).toFloat()
            val cz = (fz / flen).toFloat()

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
                val compassOffX = -(cardHalfH / 1.9 * cardAspect) + compassSize * 0.05f
                compass(
                    fx * 0.99,
                    fy * 0.99,
                    fz * 0.99,
                    compassSize,
                    compassOffX,
                    -compassSize * 0.5,
                    headingRad,
                    1f
                )

                // ── Text: callsign above separator line, distance below ────────
                // Offset rightward to leave room for the compass on the left.
                val textRadius = displayRadius - layerStep * 0.7f
                val tx = nx / displayRadius * textRadius
                val ty = ny / displayRadius * textRadius
                val tz = nz / displayRadius * textRadius

                // Shift text to the right half of the card (compass occupies left).
                // cardHalfH * 0.7 ≈ comfortable inset from left edge.
                val textRightShift = cardHalfH * 0.5f

                val rx = cz
                val ry = 0f
                val rz = -cx

                val textX = tx + rx * textRightShift
                val textY = ty + cardHalfH * 0.5f
                val textZ = tz + rz * textRightShift

                val distScale = 0.97

                textFont("roboto", 16);
                fill(230, 232, 240)
                text3D(p.label, textX * distScale, textY * distScale, textZ * distScale)

                textSize(14);
                fill(160, 165, 185)
                text3D(
                    distStr,
                    (tx - rx * textRightShift * 1.2f) * distScale,
                    (ty - cardHalfH * 0.25f) * distScale,
                    (tz - rz * textRightShift * 1.2f) * distScale
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

            fill(0)
            textFont("roboto", 18)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            text("AR Scene", width / 2, 40)

            val cardinal = orientation.getCardinalDirection()

            textAlign(TextAlignH.LEFT, TextAlignV.TOP)
            textFont("roboto", 30)

            fill(0)
            text("Yaw: ${orientation.azimuthDeg.toInt()}° ($cardinal)", 50, 300)
            text("Pitch: ${orientation.pitchDeg.toInt()}°", 50, 400)
            text("Roll: ${orientation.rollDeg.toInt()}°", 50, 500)
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

    private fun lerpAngle(
        current: Double,
        target: Double,
        alpha: Double
    ): Double {
        var delta = target - current

        while (delta > 180.0) delta -= 360.0
        while (delta < -180.0) delta += 360.0

        return current + delta * alpha
    }

    private fun applyAngleDeadband(
        current: Double,
        target: Double,
        thresholdDeg: Double
    ): Double {
        var delta = target - current

        while (delta > 180.0) delta -= 360.0
        while (delta < -180.0) delta += 360.0

        return if (kotlin.math.abs(delta) < thresholdDeg)
            current
        else
            target
    }

    private fun applyDeadband(
        current: Double,
        target: Double,
        threshold: Double
    ): Double {
        return if (kotlin.math.abs(target - current) < threshold)
            current
        else
            target
    }
}
