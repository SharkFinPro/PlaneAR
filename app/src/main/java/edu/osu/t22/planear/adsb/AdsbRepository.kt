package edu.osu.t22.planear.adsb

import android.util.Log
import androidx.collection.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AdsbRepository (
    private val service: AdsbApi,
    private val hexDbApi: HexDbApi,
    private val externalScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private var cachedAircraft = ConcurrentHashMap<String, Aircraft>()
    private val routeCache = LruCache<String, HexDbRouteResponse>(500)
    private val aircraftInfoCache = LruCache<String, HexDbAircraftResponse>(500)

    private val routeCacheLock = Any()
    private val aircraftInfoCacheLock = Any()

    suspend fun refreshAircraft(lat: Double, lon: Double, radius: Int) {
        withContext(ioDispatcher) {
            val response = service.getNearbyAircraft(lat, lon, radius)

            val now = System.currentTimeMillis()

            Log.d("ADSB", "Fetching aircraft...")

            response.ac.forEach { adsbAircraft ->
                val id = adsbAircraft.hex?.removePrefix("~") ?: return@forEach
                val aircraft = cachedAircraft.computeIfAbsent(id) {
                    Aircraft(id)
                }

                aircraft.updateFrom(adsbAircraft, now)
                val needsTypeInfo = aircraft.enrichmentState == EnrichmentState.NONE &&
                        aircraft.longType == null
                val needsRoute = aircraft.callsign != null &&
                        aircraft.origin == null &&
                        aircraft.enrichmentState == EnrichmentState.NONE

                if (needsTypeInfo || needsRoute) {
                    aircraft.enrichmentState = EnrichmentState.IN_PROGRESS
                    externalScope.launch(ioDispatcher) {
                        enrichAircraft(aircraft)
                    }
                }
            }

            //expire old aircraft
            cachedAircraft.entries.removeIf { it.value.lastRecieveTime < now - 30_000L }
        }
    }

    private suspend fun enrichTypeInfo(aircraft: Aircraft) {
        val cached = synchronized(aircraftInfoCacheLock) { aircraftInfoCache[aircraft.id] }
        val info = cached ?: runCatching {
            hexDbApi.getAircraftData(aircraft.id)
        }.getOrNull()?.also {
            synchronized(aircraftInfoCacheLock) { aircraftInfoCache.put(aircraft.id, it) }
        }

        if (info != null) {
            aircraft.longType = listOfNotNull(info.Manufacturer, info.Type).joinToString(" ")
        }
    }

    private suspend fun enrichRouteInfo(aircraft: Aircraft) {
        val callsign = aircraft.callsign ?: return
        val cached = synchronized(routeCacheLock) { routeCache[callsign] }
        val route = cached ?: runCatching {
            hexDbApi.getRouteData(callsign)
        }.getOrNull()?.also {
            synchronized(routeCacheLock) { routeCache.put(callsign, it) }
        }

        if (route?.route != null) {
            aircraft.setRoute(route.route)
        }
    }

    private suspend fun enrichAircraft(aircraft: Aircraft) {
        if (aircraft.enrichmentState == EnrichmentState.DONE || aircraft.enrichmentState == EnrichmentState.FAILED) return

        runCatching {
            if (aircraft.longType == null) enrichTypeInfo(aircraft)
            if (aircraft.callsign != null && aircraft.origin == null) enrichRouteInfo(aircraft)
            aircraft.enrichmentState = EnrichmentState.DONE
        }.onFailure {
            aircraft.enrichmentState = EnrichmentState.FAILED
        }
    }

    fun getAircraft(): List<Aircraft> {
        return cachedAircraft.values.toList()
    }
}

enum class EnrichmentState {
    IN_PROGRESS,
    FAILED,
    DONE,
    NONE
}
