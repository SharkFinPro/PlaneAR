package edu.osu.t22.planear.geo
import edu.osu.t22.planear.adsb.AdsbAircraft
import kotlin.math.*



data class ScreenPoint (
    val x: Float,
    val y: Float,
    val visible: Boolean,
    val distance: Float
)

object Planeprojector {
    fun project(
        user: GeoPoint, // user geo pos
        aircraft: GeoPoint, // plane geo pos
        azimuthDeg: Double, // compass direction camera is pointing (north = 0, east 90, south 180, west,270)
        pitchDeg: Double, // camera tilt 0 is horizon +90 straight up
        rollDeg: Double, // 0 is level
        hFovDeg: Double, // fov of camera horizontal
        vFovDeg: Double, // for of camera vertical
        screenWidth: Int, // width in pixels
        screenHeight: Int // height in pixels

    ): ScreenPoint {
        // ENH vector in meters from user to aircraft
        val enh = GeoUtils.enhVector(user, aircraft)
        val wX = enh.east.toFloat()
        val wY = enh.height.toFloat()
        val wZ = enh.north.toFloat()

        val dist = sqrt((wX * wX + wY * wY + wZ * wZ))

        val az = Math.toRadians(azimuthDeg)
        val pit = Math.toRadians(pitchDeg)
        val rol = Math.toRadians(rollDeg)

        val cosAz = cos(-az).toFloat()
        val sinAz = sin(-az).toFloat()
        val rx1 =  cosAz * wX + sinAz * wZ
        val ry1 =  wY
        val rz1 = -sinAz * wX + cosAz * wZ

        val cosPit = cos(-pit).toFloat(); val sinPit = sin(-pit).toFloat()
        val rx2 =  rx1
        val ry2 =  cosPit * ry1 - sinPit * rz1
        val rz2 =  sinPit * ry1 + cosPit * rz1

        val cosRol = cos(-rol).toFloat()
        val sinRol = sin(-rol).toFloat()
        val camX =  cosRol * rx2 - sinRol * ry2
        val camY =  sinRol * rx2 + cosRol * ry2
        val camZ =  rz2

        if (camZ <= 0f) {
            return ScreenPoint(0f, 0f, visible = false, distance = dist)
        }

        val tanHalfH = tan(Math.toRadians(hFovDeg / 2.0)).toFloat()
        val tanHalfV = tan(Math.toRadians(vFovDeg / 2.0)).toFloat()

        val ndcX = camX / (camZ * tanHalfH)
        val ndcY = camY / (camZ * tanHalfV)

        val margin = 1.0f
        val visible = (ndcX >= -margin && ndcX <= margin && ndcY >= -margin && ndcY <= margin)

        val screenX = ((ndcX + 1f) / 2f) * screenWidth
        val screenY = ((1f - ndcY) / 2f) * screenHeight

        return ScreenPoint(
            x = screenX,
            y = screenY,
            visible = visible,
            distance = dist
        )
    }

    fun projectAll(
        user: GeoPoint,
        aircraft: List<GeoPoint>,
        azimuthDeg: Double,
        pitchDeg: Double,
        rollDeg: Double,
        hFovDeg: Double,
        vFovDeg: Double,
        screenWidth: Int,
        screenHeight: Int
    ): List<ScreenPoint> = aircraft.map { ac ->
        project(user, ac, azimuthDeg, pitchDeg, rollDeg, hFovDeg, vFovDeg, screenWidth, screenHeight)
    }

    fun toNativeArray(points: List<ScreenPoint>): FloatArray {
        val visible = points.filter { it.visible }
        val arr = FloatArray(visible.size * 3)
        visible.forEachIndexed {  i, p ->
            arr[i * 3 + 0] = p.x
            arr[i * 3 + 1] = p.y
            arr[i * 3 + 2] = p.distance
        }
        return arr
    }
}