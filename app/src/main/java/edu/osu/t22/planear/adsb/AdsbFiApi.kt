package edu.osu.t22.planear.adsb

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

class AdsbFiApi : AdsbApi {

    private interface RetrofitApi {
        @GET("v3/lat/{lat}/lon/{lon}/dist/{radius}")
        suspend fun getNearbyAircraft(
            @Path("lat") lat: Double,
            @Path("lon") lon: Double,
            @Path("radius") radius: Int
        ): AdsbResponse
    }

    private val inner: RetrofitApi = Retrofit.Builder()
        .baseUrl("https://opendata.adsb.fi/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RetrofitApi::class.java)

    override suspend fun getNearbyAircraft(lat: Double, lon: Double, radius: Int): AdsbResponse =
        inner.getNearbyAircraft(lat, lon, radius)
}