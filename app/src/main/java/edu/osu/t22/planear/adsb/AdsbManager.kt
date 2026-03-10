package edu.osu.t22.planear.adsb

import android.location.Location
import android.util.Log
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.geo.GeoPoint
import edu.osu.t22.planear.geo.GeoUtils
import edu.osu.t22.planear.geo.Planeprojector
import edu.osu.t22.planear.location.AppLocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.*

data class AircraftOverlayResult(
    val dots: FloatArray,
    val xs: FloatArray,
    val ys: FloatArray,
    val labels: Array<String>
)

class AdsbManager {

    private val api = AdsbModule.provideApi()

    companion object {
        private const val H_FOV_DEG = 54.8
        private const val V_FOV_DEG = 42.5
    }

    suspend fun pollAndProject(
        location: Location,
        azimuthDeg: Double,
        pitchDeg: Double,
        rollDeg: Double,
        screenW: Int,
        screenH: Int
    ): AircraftOverlayResult? = coroutineScope {

        val lat = location.latitude
        val lon = location.longitude
        val alt = location.altitude

        val nearby = async(Dispatchers.IO) {
            api.getNearbyAircraft(lat, lon, 50)
        }

        val closest = async(Dispatchers.IO) {
            api.getClosestAircraft(lat, lon, 250)
        }

        val nearbyData = nearby.await()
        val closestData = closest.await()

        Log.d("ADSB", "Aircraft count=${nearbyData.total}")

        val userPoint = GeoPoint(lat, lon, alt)

        val projectableAircraft = nearbyData.ac.filter { it.isProjectable }

        val acPoints = projectableAircraft.map {
            GeoPoint(
                latDeg = it.lat!!,
                lonDeg = it.lon!!,
                altM = it.altitudeMeters!!
            )
        }

        val screenPoints = Planeprojector.projectAll(
            user = userPoint,
            aircraft = acPoints,
            azimuthDeg = azimuthDeg,
            pitchDeg = pitchDeg,
            rollDeg = rollDeg,
            hFovDeg = H_FOV_DEG,
            vFovDeg = V_FOV_DEG,
            screenWidth = screenW,
            screenHeight = screenH
        )

        val dots = Planeprojector.toNativeArray(screenPoints)

        val xs = mutableListOf<Float>()
        val ys = mutableListOf<Float>()
        val labels = mutableListOf<String>()

        screenPoints.forEachIndexed { i, point ->
            if (point.visible) {

                val ac = projectableAircraft[i]

                xs += point.x.toFloat()
                ys += point.y.toFloat()

                val label = when {
                    !ac.flight.isNullOrBlank() -> ac.flight!!.trim()
                    !ac.r.isNullOrBlank() -> ac.r!!.trim()
                    !ac.hex.isNullOrBlank() -> ac.hex!!.trim()
                    else -> "UNKNOWN"
                }

                labels += label

                Log.d(
                    "PROJECTION",
                    "${ac.label} x=${point.x} y=${point.y} dist=${point.distance}"
                )
            }
        }

        if (dots.isEmpty()) return@coroutineScope null

        AircraftOverlayResult(
            dots = dots,
            xs = xs.toFloatArray(),
            ys = ys.toFloatArray(),
            labels = labels.toTypedArray()
        )
    }
}
/*

    suspend fun poll() {
        val loc = appLocationManager.lastKnownLocation
        if (loc == null) {
            Log.w("AdsbManager", "No location available")
            return
        }

        val lat = loc.latitude
        val lon = loc.longitude

        coroutineScope {
            val nearby = async(Dispatchers.IO) {
                api.getNearbyAircraft(lat, lon, AppSettings.searchRadiusNm)
            }

            val closest = async(Dispatchers.IO) {
                // Use a larger bubble for "closest" so we always get at least one result
                api.getClosestAircraft(lat, lon, AppSettings.searchRadiusNm * 5)
            }

            val nearbyData  = nearby.await()
            val closestData = closest.await()

            Log.d("AdsbManager", "Radius: ${AppSettings.searchRadiusNm} nm (${AppSettings.searchRadiusNm} km)")
            Log.d("AdsbManager", "Got ${nearbyData.total} aircraft")
            Log.d("AdsbManager", "Timing data: (now: ${nearbyData.now}, cTime: ${nearbyData.cTime}, pTime: ${nearbyData.pTime})")

            val closestAircraft = closestData.ac.firstOrNull() ?: return@coroutineScope
            Log.d("AdsbManager", "Closest aircraft: $closestAircraft")

            // will be replaced with arcore geolocation
            val userAltM = 0.0
            val userHeadingDeg = 90.0 // facing east

            val acLat = closestAircraft.lat
            val acLon = closestAircraft.lon
            val acAltFeet = closestAircraft.alt_baro.toDoubleOrNull() ?: 0.0
            val acAltM = acAltFeet * 0.3048

            val userPoint = GeoPoint(lat, lon, userAltM)
            val acPoint = GeoPoint(acLat, acLon, acAltM)

            val dir = GeoUtils.relativeDirection(
                user = userPoint,
                userHeadingDeg = userHeadingDeg,
                aircraft = acPoint
            )

            Log.d("AdsbManager",
                "distance=${"%.0f".format(dir.distanceMeters)} m, " +
                        "bearing=${"%.1f".format(dir.bearingToAircraft)}°" +
                        "relative=${"%.1f".format(dir.relativeBearingDeg)}°, " +
                        "elevation=${"%.1f".format(dir.elevationDeg)}°"
            )

            val enh = GeoUtils.enhVector(userPoint, acPoint)
            Log.d("AdsbManager",
                "east=${"%.1f".format(enh.east)}, " +
                        "north=${"%.1f".format(enh.north)} m, " +
                        "height=${"%.1f".format(enh.height)} m"
            )
        }
    }
}*/