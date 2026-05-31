package edu.osu.t22.planear.adsb

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface AdsbApi {
    @GET("v2/closest/{lat}/{lon}/{radius}")
    suspend fun getClosestAircraft(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("radius") radius: Int
    ): AdsbResponse

    @GET("v2/point/{lat}/{lon}/{radius}")
    suspend fun getNearbyAircraft(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("radius") radius: Int
    ): AdsbResponse

    @GET("v2/callsign/{callsign}")
    suspend fun getCallsignAircraft(
        @Path("callsign") callsign: String
    ): AdsbResponse

    companion object {
        private const val BASE_URL = "https://api.adsb.lol/"
        fun create() : AdsbApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AdsbApi::class.java)
        }
    }
}