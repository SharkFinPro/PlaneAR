package edu.osu.t22.planear.adsb

import android.location.Location
import android.util.Log
import edu.osu.t22.planear.AppSettings
import edu.osu.t22.planear.achievements.AchievementStore
import edu.osu.t22.planear.achievements.AchievementTracker
import edu.osu.t22.planear.geo.GeoPoint
import edu.osu.t22.planear.geo.Planeprojector
import edu.osu.t22.planear.location.AppLocationManager
import kotlinx.coroutines.CoroutineScope
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

class AdsbManager(
    private val appLocationManager: AppLocationManager,
    private val scope: CoroutineScope
) {
    private val repository: AdsbRepository = AdsbRepository(AdsbApi.create(), HexDbApi.create(), scope)

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
            return false
        }

        try {
            repository.refreshAircraft(
                lat = loc.latitude,
                lon = loc.longitude,
                radius = AppSettings.searchRadiusNm
            )
        } catch (e: Exception) {
            Log.e("ADSB", "Failed to fetch aircraft data", e)
            return false
        }

        // Check achievement conditions — only when user is on the AR page
        if (AchievementStore.isOnArPage) {
            try {
                AchievementTracker.checkAchievements(repository.getAircraft(), loc)
            } catch (e: Exception) {
                Log.e("ADSB", "Achievement check failed", e)
            }
        }
        return true //TODO check return
    }

    fun getRepository(): AdsbRepository {
        return repository
    }
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
            return null
        }

        val aircraftData = repository.getAircraft()
        if (aircraftData.isEmpty()) {
            return null
        }

        val userPoint = GeoPoint(loc.latitude, loc.longitude, loc.altitude)
        val acPoints = aircraftData.map { ac -> ac.getPosition() }

        val screenPoints = Planeprojector.projectAll(
            user = userPoint,
            aircraft = acPoints,
            azimuthDeg = azimuthDeg,
            pitchDeg = pitchDeg,
            rollDeg = rollDeg,
            screenWidth = screenW,
            screenHeight = screenH
        )

        val dots = Planeprojector.toNativeArray(screenPoints)

        val xs = mutableListOf<Float>()
        val ys = mutableListOf<Float>()
        val labels = mutableListOf<String>()

        screenPoints.forEachIndexed { i, point ->
            if (point.visible) {
                xs += point.x
                ys += point.y
                labels += aircraftData[i].label
            }
        }

        if (dots.isEmpty()) {
            return null
        }

        return AircraftOverlayResult(
            dots = dots,
            xs = xs.toFloatArray(),
            ys = ys.toFloatArray(),
            labels = labels.toTypedArray()
        )
    }
}