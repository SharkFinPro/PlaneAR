package edu.osu.t22.planear.geo

import kotlin.math.*

data class ScreenPoint(
    val x: Float,
    val y: Float,
    val visible: Boolean,
    val distance: Float,
    val camX: Float = 0f,
    val camY: Float = 0f,
    val camZ: Float = 0f
)

data class EdgeIndicator(
    val x: Float,
    val y: Float,
    val angleDeg: Float
)

object Planeprojector {

    private fun rotateToCameraSpace(
        wX: Float, wY: Float, wZ: Float,
        azimuthDeg: Double,
        pitchDeg: Double,
        @Suppress("UNUSED_PARAMETER") rollDeg: Double
    ): Triple<Float, Float, Float> {

        val yaw_r   = Math.toRadians(azimuthDeg)
        val pitch_r = Math.toRadians(pitchDeg)

        val fx = (cos(yaw_r) * cos(pitch_r)).toFloat()
        val fy =  sin(pitch_r).toFloat()
        val fz = (sin(yaw_r) * cos(pitch_r)).toFloat()
        val fLen = sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(1e-6f)
        val fxN = fx / fLen;  val fyN = fy / fLen;  val fzN = fz / fLen


        var rX = -fzN;  val rY = 0f;  var rZ = fxN
        val rLen = sqrt(rX * rX + rZ * rZ).coerceAtLeast(1e-6f)
        rX /= rLen;  rZ /= rLen

        val uX = rY * fzN - rZ * fyN
        val uY = rZ * fxN - rX * fzN
        val uZ = rX * fyN - rY * fxN

        val camX =  rX * wX + rY * wY + rZ * wZ
        val camY =  uX * wX + uY * wY + uZ * wZ
        val camZ =  fxN * wX + fyN * wY + fzN * wZ

        return Triple(camX, camY, camZ)
    }
    private fun perspectiveProject(
        camX: Float, camY: Float, camZ: Float,
        distance: Float,
        screenWidth: Int, screenHeight: Int
    ): ScreenPoint {

        val tanHalfV = tan(Math.toRadians(25.0)).toFloat()
        val tanHalfH = tanHalfV * screenWidth.toFloat() / screenHeight.toFloat()

        if (camZ <= 0f) {
            val safeDenom = max(abs(camZ), 0.01f)
            val ndcX = -(camX / (safeDenom * tanHalfH))
            val ndcY = -(camY / (safeDenom * tanHalfV))
            val screenX = ((ndcX.coerceIn(-10f, 10f) + 1f) / 2f) * screenWidth
            val screenY = ((1f - ndcY.coerceIn(-10f, 10f)) / 2f) * screenHeight
            return ScreenPoint(screenX, screenY, visible = false, distance = distance,
                camX = camX, camY = camY, camZ = camZ)  // ← add this
        }

        val ndcX = camX / (camZ * tanHalfH)
        val ndcY = camY / (camZ * tanHalfV)

        if (!ndcX.isFinite() || !ndcY.isFinite()) {
            return ScreenPoint(screenWidth / 2f, screenHeight / 2f,
                visible = false, distance = distance,
                camX = camX, camY = camY, camZ = camZ)  // ← and this
        }

        val visible = ndcX in -1f..1f && ndcY in -1f..1f
        val screenX = ((ndcX + 1f) / 2f) * screenWidth
        val screenY = ((1f - ndcY) / 2f) * screenHeight

        return ScreenPoint(screenX, screenY, visible, distance,
            camX = camX, camY = camY, camZ = camZ)  // ← and this
    }

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
        val cx = screenWidth / 2f
        val cy = screenHeight / 2f

        val dx: Float
        val dy: Float

        if (point.camZ <= 0f) {
            val safeCamZ = max(abs(point.camZ), 0.01f)
            val tanHalfV = tan(Math.toRadians(25.0)).toFloat()
            val tanHalfH = tanHalfV * screenWidth / screenHeight.toFloat()
            dx = -(point.camX / (safeCamZ * tanHalfH))
            dy =  (point.camY / (safeCamZ * tanHalfV))
        } else {
            dx = point.x - cx
            dy = point.y - cy
        }

        if (dx == 0f && dy == 0f) {
            return EdgeIndicator(screenWidth - inset, cy, 0f)
        }

        if (!dx.isFinite() || !dy.isFinite()) {
            return EdgeIndicator(screenWidth - inset, cy, 0f)
        }

        val left = inset
        val right = screenWidth  - inset
        val top = inset
        val bottom = screenHeight - inset

        var t = Float.POSITIVE_INFINITY
        if (dx > 0f) t = minOf(t, (right - cx) / dx)
        if (dx < 0f) t = minOf(t, (left - cx) / dx)
        if (dy > 0f) t = minOf(t, (bottom - cy) / dy)
        if (dy < 0f) t = minOf(t, (top - cy) / dy)

        if (!t.isFinite() || t <= 0f) {
            return EdgeIndicator(screenWidth - inset, cy, 0f)
        }

        val edgeX = (cx + dx * t).coerceIn(left, right)
        val edgeY = (cy + dy * t).coerceIn(top,  bottom)
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        return EdgeIndicator(edgeX, edgeY, angle)
    }
}