package edu.osu.t22.planear.geo

import kotlin.math.*

data class ScreenPoint(
    val x: Float,
    val y: Float,
    val visible: Boolean,
    val distance: Float
)

data class EdgeIndicator(
    val x: Float,
    val y: Float,
    val angleDeg: Float
)

/**
 * Coordinate conventions used throughout this file
 * ─────────────────────────────────────────────────
 * Unified world-space convention (used by BOTH entry points):
 *   +X = East
 *   +Y = Up
 *   +Z = –North  (forward when azimuth offset = 0)
 *
 * This matches the 3D engine (set3DView) and ArPage, where north is –Z.
 *
 * project()            → receives ENH from GeoUtils (+north), negates north internally.
 * projectFromArVector() → caller (ArPage) already provides –north, passed straight through.
 *
 * Both paths feed identical (wX, wY, wZ) into rotateToCameraSpace() and must
 * therefore produce identical screen positions for the same physical aircraft.
 *
 * Camera space (after rotation):
 *   +X = right
 *   +Y = up
 *   +Z = forward (into the scene)   ← glm::lookAt convention matched here
 *
 * Rotation order: Yaw → Pitch
 * Caller passes azimuthDeg – 90 so 0° aligns with +X (East).
 *
 * glm::lookAt convention (matched exactly):
 *   forward = normalise(direction)
 *   right   = normalise(cross(UP, -forward))  =  normalise(cross(UP, -fwd))
 *   up      = cross(right, forward)
 *   camX    = dot(right,   p)
 *   camY    = dot(up,      p)
 *   camZ    = dot(forward, p)          ← +Z is INTO the scene
 *
 * glm::perspective in set3DView flips Y (projMatrix[1][1] *= -1) and maps
 * +camZ to depth, so points in front of the camera have camZ > 0.
 */
object Planeprojector {

    // ─────────────────────────────────────────────────────────────────────────
    // Internal rotation helper – exact reimplementation of glm::lookAt so
    // Kotlin screen coords match where the C++ engine actually draws points.
    //
    // set3DView() in C++ builds:
    //   forward = (cos(yaw)*cos(pitch), sin(pitch), sin(yaw)*cos(pitch))
    //   right   = normalize(cross(UP, -forward))   [glm::lookAt internal]
    //   up      = cross(right, forward)
    //   camX=dot(right,p), camY=dot(up,p), camZ=dot(forward,p)
    //
    // roll is NOT applied by set3DView (the UP vector is always (0,1,0)).
    // We intentionally omit roll here to match that behaviour.
    //
    // Input convention:
    //   wX = East, wY = Up, wZ = –North  (same as ArPage / engine world space)
    //   azimuthDeg = orientation.azimuthDeg – 90  (0° → camera faces East)
    // ─────────────────────────────────────────────────────────────────────────
    private fun rotateToCameraSpace(
        wX: Float, wY: Float, wZ: Float,
        azimuthDeg: Double,
        pitchDeg: Double,
        @Suppress("UNUSED_PARAMETER") rollDeg: Double   // engine ignores roll
    ): Triple<Float, Float, Float> {

        val yaw_r   = Math.toRadians(azimuthDeg)
        val pitch_r = Math.toRadians(pitchDeg)

        // ── Engine forward vector (identical to C++ set3DView) ───────────────
        val fx = (cos(yaw_r) * cos(pitch_r)).toFloat()
        val fy =  sin(pitch_r).toFloat()
        val fz = (sin(yaw_r) * cos(pitch_r)).toFloat()
        val fLen = sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(1e-6f)
        val fxN = fx / fLen;  val fyN = fy / fLen;  val fzN = fz / fLen

        // ── right = normalize(cross(UP=(0,1,0), -forward)) ──────────────────
        // cross((0,1,0), (-fxN,-fyN,-fzN))
        //   x: 1*(-fzN) - 0*(-fyN) = -fzN
        //   y: 0*(-fxN) - 0*(-fzN) =  0
        //   z: 0*(-fyN) - 1*(-fxN) =  fxN
        // → right = (-fzN, 0, fxN)  (same as before – this was correct)
        var rX = -fzN;  val rY = 0f;  var rZ = fxN
        val rLen = sqrt(rX * rX + rZ * rZ).coerceAtLeast(1e-6f)
        rX /= rLen;  rZ /= rLen

        // ── up = cross(right, forward) ───────────────────────────────────────
        val uX = rY * fzN - rZ * fyN    // = -rZ*fyN
        val uY = rZ * fxN - rX * fzN
        val uZ = rX * fyN - rY * fxN    // =  rX*fyN

        // ── glm::lookAt: camX=dot(right,p), camY=dot(up,p), camZ=dot(fwd,p) ─
        // KEY FIX: glm::lookAt stores -forward in its Z column but the
        // resulting camZ = dot(-forward, p) is then negated by the projection
        // matrix so that forward-facing points map to positive depth.
        // Net effect: camZ = dot(forward, p)  →  camZ > 0 means in front.
        val camX =  rX * wX + rY * wY + rZ * wZ
        val camY =  uX * wX + uY * wY + uZ * wZ
        val camZ =  fxN * wX + fyN * wY + fzN * wZ   // ← was: -fxN,-fyN,-fzN

        return Triple(camX, camY, camZ)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Project (camX, camY, camZ) → ScreenPoint using perspective division.
    //
    // FOV is derived from the C++ engine (Renderer2D.cpp → set3DView):
    //   glm::perspective(glm::radians(50.0f), screenWidth/screenHeight, 500, 20000)
    //   → vFOV = 50°  →  tanHalfV = tan(25°)
    //   → hFOV derived from aspect ratio: tanHalfH = tanHalfV * (width/height)
    //
    // glm::perspective with projMatrix[1][1] *= -1 maps:
    //   NDC x = camX / (camZ * tanHalfH)
    //   NDC y = camY / (camZ * tanHalfV)    (Y is already un-flipped by the *= -1)
    // Points in front of camera have camZ > 0.
    // ─────────────────────────────────────────────────────────────────────────
    private fun perspectiveProject(
        camX: Float, camY: Float, camZ: Float,
        distance: Float,
        screenWidth: Int, screenHeight: Int
    ): ScreenPoint {

        // Engine vFOV = 50° (hardcoded in C++). tan(25°) never changes.
        val tanHalfV = tan(Math.toRadians(25.0)).toFloat()
        val tanHalfH = tanHalfV * screenWidth.toFloat() / screenHeight.toFloat()

        // camZ <= 0 → aircraft is behind the camera.
        // Flip and clamp so the edge indicator still points sensibly.
        if (camZ <= 0f) {
            val safeDenom = max(abs(camZ), 0.01f)

            val ndcX = -(camX / (safeDenom * tanHalfH))
            val ndcY = -(camY / (safeDenom * tanHalfV))

            val screenX = ((ndcX.coerceIn(-10f, 10f) + 1f) / 2f) * screenWidth
            val screenY = ((1f - ndcY.coerceIn(-10f, 10f)) / 2f) * screenHeight

            return ScreenPoint(screenX, screenY, visible = false, distance = distance)
        }

        val ndcX = camX / (camZ * tanHalfH)
        val ndcY = camY / (camZ * tanHalfV)

        if (!ndcX.isFinite() || !ndcY.isFinite()) {
            return ScreenPoint(
                screenWidth / 2f, screenHeight / 2f,
                visible = false, distance = distance
            )
        }

        val visible = ndcX in -1f..1f && ndcY in -1f..1f

        val screenX = ((ndcX + 1f) / 2f) * screenWidth
        val screenY = ((1f - ndcY) / 2f) * screenHeight

        return ScreenPoint(screenX, screenY, visible, distance)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API – geo-based projection
    // ─────────────────────────────────────────────────────────────────────────
    fun project(
        user: GeoPoint,
        aircraft: GeoPoint,
        azimuthDeg: Double,
        pitchDeg: Double,
        rollDeg: Double,
        screenWidth: Int,
        screenHeight: Int
    ): ScreenPoint {

        val enh = GeoUtils.enhVector(user, aircraft)

        val wX =  enh.east.toFloat()
        val wY =  enh.height.toFloat()
        val wZ = -enh.north.toFloat()   // negate north → unified –north convention

        val dist = sqrt(wX * wX + wY * wY + wZ * wZ)

        if (dist < 1f) {
            return ScreenPoint(
                screenWidth / 2f, screenHeight / 2f,
                visible = false, distance = dist
            )
        }

        val (camX, camY, camZ) = rotateToCameraSpace(wX, wY, wZ, azimuthDeg, pitchDeg, rollDeg)

        return perspectiveProject(camX, camY, camZ, dist, screenWidth, screenHeight)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API – vector-based projection (ArPage convention)
    //
    // Caller already provides:
    //   rawX =  dLon * metersPerDegLon   (East)
    //   rawY =  dAlt                     (Up)
    //   rawZ = -(dLat * metersPerDegLat) (negative North = unified convention)
    // ─────────────────────────────────────────────────────────────────────────
    fun projectFromArVector(
        rawX: Float,
        rawY: Float,
        rawZ: Float,
        distance: Float,
        azimuthDeg: Double,
        pitchDeg: Double,
        rollDeg: Double,
        screenWidth: Int,
        screenHeight: Int
    ): ScreenPoint {

        val (camX, camY, camZ) = rotateToCameraSpace(rawX, rawY, rawZ, azimuthDeg, pitchDeg, rollDeg)

        return perspectiveProject(camX, camY, camZ, distance, screenWidth, screenHeight)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convenience helpers
    // ─────────────────────────────────────────────────────────────────────────

    fun projectAll(
        user: GeoPoint,
        aircraft: List<GeoPoint>,
        azimuthDeg: Double,
        pitchDeg: Double,
        rollDeg: Double,
        screenWidth: Int,
        screenHeight: Int
    ): List<ScreenPoint> = aircraft.map { ac ->
        project(user, ac, azimuthDeg, pitchDeg, rollDeg, screenWidth, screenHeight)
    }

    fun toNativeArray(points: List<ScreenPoint>): FloatArray {
        val visible = points.filter { it.visible }
        val arr = FloatArray(visible.size * 3)
        visible.forEachIndexed { i, p ->
            arr[i * 3 + 0] = p.x
            arr[i * 3 + 1] = p.y
            arr[i * 3 + 2] = p.distance
        }
        return arr
    }

    fun getEdgeIndicator(
        point: ScreenPoint,
        screenWidth: Int,
        screenHeight: Int,
        inset: Float = 40f
    ): EdgeIndicator {

        val cx = screenWidth  / 2f
        val cy = screenHeight / 2f

        val dx = point.x - cx
        val dy = point.y - cy

        if (dx == 0f && dy == 0f) {
            return EdgeIndicator(screenWidth - inset, cy, 0f)
        }

        if (!dx.isFinite() || !dy.isFinite()) {
            return EdgeIndicator(screenWidth - inset, cy, 0f)
        }

        val left   = inset
        val right  = screenWidth  - inset
        val top    = inset
        val bottom = screenHeight - inset

        var t = Float.POSITIVE_INFINITY
        if (dx > 0f) t = minOf(t, (right  - cx) / dx)
        if (dx < 0f) t = minOf(t, (left   - cx) / dx)
        if (dy > 0f) t = minOf(t, (bottom - cy) / dy)
        if (dy < 0f) t = minOf(t, (top    - cy) / dy)

        if (!t.isFinite() || t <= 0f) {
            return EdgeIndicator(screenWidth - inset, cy, 0f)
        }

        val edgeX = (cx + dx * t).coerceIn(left, right)
        val edgeY = (cy + dy * t).coerceIn(top,  bottom)
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        return EdgeIndicator(edgeX, edgeY, angle)
    }
}