package edu.osu.t22.planear.adsb

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface HexDbApi {
    @GET("api/v1/route/iata/{callsign}")
    suspend fun getRouteData(
        @Path("callsign") callsign: String
    ): HexDbRouteResponse

    @GET("api/v1/aircraft/{hex}")
    suspend fun getAircraftData(
        @Path("hex") hex: String
    ): HexDbAircraftResponse

    companion object {
        private const val BASE_URL = "https://hexdb.io/"
        fun create() : HexDbApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(HexDbApi::class.java)
        }
    }
}