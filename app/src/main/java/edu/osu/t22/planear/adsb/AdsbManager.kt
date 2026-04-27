package edu.osu.t22.planear.adsb

import android.location.Location
import android.util.Log
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.geo.GeoPoint
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

class AdsbManager(private val appLocationManager: AppLocationManager) {

    private val api = AdsbModule.provideApi()

    companion object {
        const val H_FOV_DEG = 54.8
        const val V_FOV_DEG = 42.5
    }

    /**
     * Fetch aircraft data and store raw positions (without projecting to screen).
     * Call this on a slower interval (e.g., every 5 seconds).
     */
    suspend fun fetchAircraftData(location: Location?): Boolean {
        val loc = location ?: appLocationManager.lastKnownLocation
        if (loc == null) {
            Log.w("ADSB", "No location available")
            AircraftOverlayStore.aircraftData = emptyList()
            return false
        }

        val lat = loc.latitude
        val lon = loc.longitude

        val nearbyData = try {
            api.getNearbyAircraft(lat, lon, AppSettings.searchRadiusNm)
        } catch (e: Exception) {
            Log.e("ADSB", "Failed to fetch aircraft data", e)
            AircraftOverlayStore.aircraftData = emptyList()
            return false
        }

        val projectableAircraft = nearbyData.ac.filter { it.isProjectable }

        AircraftOverlayStore.aircraftData = projectableAircraft.map { ac ->
            AircraftPosition(
                position = GeoPoint(
                    latDeg = ac.lat!!,
                    lonDeg = ac.lon!!,
                    altM = ac.altitudeMeters!!
                ),
                label = when {
                    !ac.flight.isNullOrBlank() -> ac.flight.trim()
                    !ac.r.isNullOrBlank() -> ac.r.trim()
                    !ac.hex.isNullOrBlank() -> ac.hex.trim()
                    else -> "UNKNOWN"
                }
            )
        }

        Log.d("ADSB", "Fetched ${AircraftOverlayStore.aircraftData.size} aircraft")
        return AircraftOverlayStore.aircraftData.isNotEmpty()
    }

    /**
     * Project stored aircraft positions to screen coordinates using current orientation.
     * Call this at high frequency (e.g., every frame).
     */
    fun projectToScreen(
        location: Location?,
        azimuthDeg: Double,
        pitchDeg: Double,
        rollDeg: Double,
        screenW: Int,
        screenH: Int
    ): AircraftOverlayResult? {
        val loc = location ?: appLocationManager.lastKnownLocation
        if (loc == null) {
            AircraftOverlayStore.points = emptyList()
            return null
        }

        val aircraftData = AircraftOverlayStore.aircraftData
        if (aircraftData.isEmpty()) {
            AircraftOverlayStore.points = emptyList()
            return null
        }

        val userPoint = GeoPoint(loc.latitude, loc.longitude, loc.altitude)
        val acPoints = aircraftData.map { it.position }

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
        val edgeIndicators = mutableListOf<AircraftEdgeIndicator>()


        screenPoints.forEachIndexed { i, point ->
            val label = aircraftData[i].label

            if (point.visible) {
                xs += point.x
                ys += point.y
                labels += label
            } else {
                val edge = Planeprojector.getEdgeIndicator(
                    point = point,
                    screenWidth = screenW,
                    screenHeight = screenH
                )

                edgeIndicators += AircraftEdgeIndicator(
                    x = edge.x,
                    y = edge.y,
                    angleDeg = edge.angleDeg,
                    label = label
                )
            }
        }

        AircraftOverlayStore.edgeIndicators = edgeIndicators

        if (dots.isEmpty() && edgeIndicators.isEmpty()) {
            AircraftOverlayStore.points = emptyList()
            AircraftOverlayStore.edgeIndicators = emptyList()
            return null
        }

        AircraftOverlayStore.points = xs.indices.map { i ->
            AircraftScreenPoint(x = xs[i], y = ys[i], label = labels[i])
        }

        return AircraftOverlayResult(
            dots = dots,
            xs = xs.toFloatArray(),
            ys = ys.toFloatArray(),
            labels = labels.toTypedArray()
        )
    }

    suspend fun pollAndProject(
        location: Location?,
        azimuthDeg: Double,
        pitchDeg: Double,
        rollDeg: Double,
        screenW: Int,
        screenH: Int
    ): AircraftOverlayResult? = coroutineScope {

        val loc = location ?: appLocationManager.lastKnownLocation
        if (loc == null) {
            Log.w("ADSB", "No location available")
            return@coroutineScope null
        }

        val lat = loc.latitude
        val lon = loc.longitude
        val alt = loc.altitude

        val nearby = async(Dispatchers.IO) {
            api.getNearbyAircraft(lat, lon, AppSettings.searchRadiusNm)
        }

        val closest = async(Dispatchers.IO) {
            api.getClosestAircraft(lat, lon, AppSettings.searchRadiusNm * 3)
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