package edu.osu.t22.planear.adsb

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AdsbModule {
    fun provideApi(): AdsbApi {
        return Retrofit.Builder()
            .baseUrl("https://api.adsb.lol/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdsbApi::class.java)
    }
}
