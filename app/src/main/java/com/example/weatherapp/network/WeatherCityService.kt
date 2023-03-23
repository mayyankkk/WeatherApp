package com.example.weatherapp.network

import com.example.weatherapp.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface WeatherCityService {
    @GET("2.5/weather")
    fun getWeather(
        @Query("q") q: String?,
        @Query("units")units:String?,
        @Query("appid") appid:String?
    ): Call<WeatherResponse>
}