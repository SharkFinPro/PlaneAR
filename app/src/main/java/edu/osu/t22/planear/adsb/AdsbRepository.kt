package edu.osu.t22.planear.adsb

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdsbRepository (
    private val service: AdsbApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private var cachedAircraft = mutableMapOf<String, Aircraft>()

    suspend fun refreshAircraft(lat: Double, lon: Double, radius: Int) {
        withContext(ioDispatcher) {
            val response = service.getNearbyAircraft(lat, lon, radius)

            val now = System.currentTimeMillis()

            response.ac.forEach { adsbAircraft ->
                val id = adsbAircraft.hex?.removePrefix("~") ?: return@forEach

                val aircraft = cachedAircraft.getOrPut(id) {
                    Aircraft(id)
                }

                aircraft.updateFrom(adsbAircraft, now)
            }

            //expire old aircraft
            cachedAircraft.entries.removeIf { it.value.lastRecieveTime < now - 30_000L }
        }
    }

    fun getAircraft(): List<Aircraft> {
        return cachedAircraft.values.toList()
    }
}