package edu.osu.t22.planear.adsb

interface AdsbApi {

    suspend fun getNearbyAircraft(lat: Double, lon: Double, radius: Int): AdsbResponse
}