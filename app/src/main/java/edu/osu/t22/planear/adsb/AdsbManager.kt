package edu.osu.t22.planear.adsb

import android.util.Log
import edu.osu.t22.planear.geo.GeoPoint
import edu.osu.t22.planear.geo.GeoUtils
import edu.osu.t22.planear.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AdsbManager(private val locationManager: LocationManager) {
    private val api = AdsbModule.provideApi()

    suspend fun poll() {
        val loc = locationManager.lastKnownLocation
        if (loc == null) {
            Log.w("AdsbManager", "No location available")
            return
        }

        val lat = loc.latitude
        val lon = loc.longitude

        coroutineScope {
            val nearby = async(Dispatchers.IO) {
                api.getNearbyAircraft(lat, lon, 50)
            }

            val closest = async(Dispatchers.IO) {
                api.getClosestAircraft(lat, lon, 250)
            }

            val nearbyData = nearby.await()
            val closestData = closest.await()

            Log.d("AdsbManager", "Got ${nearbyData.total} aircraft")
            Log.d("AdsbManager", "Timing data: (now: ${nearbyData.now}, cTime: ${nearbyData.cTime}, pTime: ${nearbyData.pTime})")

            val closestAircraft = closestData.ac.firstOrNull() ?: return@coroutineScope
            Log.d("AdsbManager", "Closest aircraft: $closestAircraft")

            // will be replaced with arcore geolocation
            val userAltM = 0.0
            val userHeadingDeg = 90.0 //facing east

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
}