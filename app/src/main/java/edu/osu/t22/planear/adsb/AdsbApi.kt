package edu.osu.t22.planear.adsb

import retrofit2.http.GET
import retrofit2.http.Path

interface AdsbApi {
    @GET("v2/closest/{lat}/{lon}/{radius}")
    suspend fun getClosestAircraft(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("radius") radius: Int = 50
    ): AdsbResponse

    @GET("v2/point/{lat}/{lon}/{radius}")
    suspend fun getNearbyAircraft(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("radius") radius: Int = 50
    ): AdsbResponse

    @GET("v2/callsign/{callsign}")
    suspend fun getCallsignAircraft(
        @Path("callsign") callsign: String
    ): AdsbResponse
}