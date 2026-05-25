package edu.osu.t22.planear.achievements

import android.location.Location
import android.util.Log
import edu.osu.t22.planear.adsb.AdsbAircraft
import edu.osu.t22.planear.adsb.Aircraft
import java.util.Calendar
import kotlin.math.*

/**
 * Evaluates live ADS-B data against all achievement conditions.
 *
 * Called from AdsbManager.fetchAircraftData() after each successful API response.
 */
object AchievementTracker {

    private const val TAG = "AchievementTracker"

    /** Max entries per ICAO in heading/position history to prevent memory bloat. */
    private const val MAX_HISTORY_SIZE = 30

    /**
     * Main entry point — check all achievement conditions against the current batch
     * of aircraft from the ADS-B API.
     *
     * @param aircraft  Full list of aircraft from the API response (not just projectable).
     * @param userLocation  Current user GPS location, or null.
     */
    fun checkAchievements(aircraft: List<Aircraft>, userLocation: Location?) {
        val store = AchievementStore
        val now   = System.currentTimeMillis()
        val hour  = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Count valid (with ICAO) aircraft
        val validAircraft = aircraft.filter { !it.id.isNullOrBlank() }

        // Record each ICAO
        for (ac in validAircraft) {
            store.recordIcao(ac.id!!)
        }

        // If we have at least one aircraft, record today as a tracking day
        if (validAircraft.isNotEmpty()) {
            store.recordTodayTracking()
        }

        // --- Unique ICAO milestones ---
        checkAndUnlock("first_contact")   { store.uniqueIcaosSeen.size >= 1 }
        checkAndUnlock("sky_watcher")     { store.uniqueIcaosSeen.size >= 50 }
        checkAndUnlock("century_club")    { store.uniqueIcaosSeen.size >= 100 }
        checkAndUnlock("icao_collector")  { store.uniqueIcaosSeen.size >= 500 }
        checkAndUnlock("the_obsessed")    { store.uniqueIcaosSeen.size >= 1000 }

        // --- Simultaneous aircraft count ---
        checkAndUnlock("tower_of_signals") { validAircraft.size >= 10 }
        checkAndUnlock("rush_hour")        { validAircraft.size >= 20 }

        // --- Speed ---
        checkAndUnlock("speed_demon") {
            validAircraft.any { (it.groundSpeed ?: 0.0) > 550.0 }
        }

        // --- Altitude ---
        checkAndUnlock("stratosphere_club") {
            validAircraft.any { (it.altitudeSeaLevel) > 40_000.0 }
        }
        checkAndUnlock("low_rider") {
            validAircraft.any {
                val alt = it.altitudeSeaLevel
                alt in 1.0..1_000.0
            }
        }

        // --- Vertical rate ---
        checkAndUnlock("climber") {
            validAircraft.any { (it.verticalRate) > 3_000 }
        }
        checkAndUnlock("nose_diver") {
            validAircraft.any { (it.verticalRate) < -3_000 }
        }

        // --- Slow flyer ---
        checkAndUnlock("easy_rider") {
            validAircraft.any {
                val gs  = it.groundSpeed ?: Double.MAX_VALUE
                val alt = it.altitudeSeaLevel
                gs < 150.0 && alt > 5_000.0
            }
        }

        // --- Flat and fast (cruising level at high speed) ---
        checkAndUnlock("flat_and_fast") {
            validAircraft.any {
                val alt  = it.altitudeSeaLevel
                val rate = it.verticalRate
                val gs   = it.groundSpeed ?: 0.0
                alt > 30_000.0 && rate == 0 && gs > 400.0
            }
        }

        // --- Ghost signal (no callsign) ---
        checkAndUnlock("ghost_signal") {
            validAircraft.any { it.callsign.isNullOrBlank() }
        }

        // --- Time-based ---
        if (validAircraft.isNotEmpty()) {
            checkAndUnlock("night_owl")   { hour in 0..3 }
            checkAndUnlock("dawn_patrol") { hour < 5 }
        }

        // --- Streak-based ---
        checkAndUnlock("loyal_spotter") { store.getCurrentStreak() >= 7 }
        checkAndUnlock("dedicated")     { store.getCurrentStreak() >= 30 }

        // --- Tracking time ---
        checkAndUnlock("marathon_spotter") {
            store.totalTrackingMs >= 24L * 3600L * 1000L
        }

        // --- Frequent flyer (same ICAO on 5+ separate days) ---
        checkAndUnlock("frequent_flyer") {
            store.icaoDayCounts.any { it.value.size >= 5 }
        }

        // --- Continental divide (5 major prefix chars) ---
        checkAndUnlock("continental_divide") {
            val target = setOf('A', 'C', 'E', '7', '9')
            target.all { it in store.continentalPrefixes }
        }

        // --- Heading home (directly overhead, elevation >= 85°) ---
        if (userLocation != null) {
            checkAndUnlock("heading_home") {
                validAircraft.any { ac ->
                    val acLat = ac.latitude
                    val acLon = ac.longitude
                    val acAltM = ac.altitudeMeters
                    val userAltM = userLocation.altitude

                    val elevDeg = computeElevationAngle(
                        userLocation.latitude, userLocation.longitude, userAltM,
                        acLat, acLon, acAltM
                    )
                    elevDeg >= 85.0
                }
            }
        }

        // --- Sharp turn (heading change > 90° within 30 seconds) ---
        for (ac in validAircraft) {
            val heading = ac.headingDegrees ?: continue
            val hex     = ac.id

            val history = store.headingHistory.getOrPut(hex) { mutableListOf() }
            history.add(Pair(now, heading))

            // Trim old entries (keep last MAX_HISTORY_SIZE)
            if (history.size > MAX_HISTORY_SIZE) {
                history.removeAt(0)
            }

            // Check for > 90° change within any 30-second window
            if (!store.isUnlocked("sharp_turn")) {
                for (old in history) {
                    if (now - old.first <= 30_000L) {
                        val delta = abs(heading - old.second).let {
                            if (it > 180.0) 360.0 - it else it
                        }
                        if (delta > 90.0) {
                            store.unlock("sharp_turn")
                            break
                        }
                    }
                }
            }
        }

        // --- Holding pattern (circling same position for > 3 minutes) ---
        for (ac in validAircraft) {
            val lat = ac.latitude
            val lon = ac.longitude
            val hex = ac.id

            val history = store.positionHistory.getOrPut(hex) { mutableListOf() }
            history.add(Triple(now, lat, lon))

            if (history.size > MAX_HISTORY_SIZE) {
                history.removeAt(0)
            }

            if (!store.isUnlocked("holding_pattern") && history.size >= 3) {
                val oldest = history.first()
                val timeSpan = now - oldest.first

                if (timeSpan >= 3 * 60 * 1000L) {
                    // Check if all positions are within ~1 nm (1852 m) of the average
                    val avgLat = history.map { it.second }.average()
                    val avgLon = history.map { it.third }.average()

                    val allClose = history.all { (_, pLat, pLon) ->
                        haversineMeters(avgLat, avgLon, pLat, pLon) < 1852.0
                    }

                    if (allClose) {
                        store.unlock("holding_pattern")
                    }
                }
            }
        }

        // Clean up stale heading/position history (aircraft no longer in range)
        val currentHexes = validAircraft.map { it.id }.toSet()
        store.headingHistory.keys.removeAll { it !in currentHexes }
        store.positionHistory.keys.removeAll { it !in currentHexes }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private inline fun checkAndUnlock(id: String, condition: () -> Boolean) {
        if (!AchievementStore.isUnlocked(id) && condition()) {
            AchievementStore.unlock(id)
            Log.i(TAG, "Achievement unlocked: $id")
        }
    }

    /**
     * Compute the elevation angle from the user to an aircraft.
     * Returns degrees above the horizon (0=horizon, 90=directly overhead).
     */
    private fun computeElevationAngle(
        userLat: Double, userLon: Double, userAltM: Double,
        acLat: Double, acLon: Double, acAltM: Double
    ): Double {
        val horizontalDist = haversineMeters(userLat, userLon, acLat, acLon)
        val verticalDist   = acAltM - userAltM
        if (horizontalDist < 1.0) return 90.0  // essentially right on top
        return Math.toDegrees(atan2(verticalDist, horizontalDist))
    }

    /**
     * Haversine distance between two lat/lon points in meters.
     */
    private fun haversineMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6_371_000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        return R * 2.0 * atan2(sqrt(a), sqrt(1 - a))
    }
}
