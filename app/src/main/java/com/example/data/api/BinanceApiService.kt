package com.example.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface BinanceApiService {

    @GET("api/v3/ticker/24hr")
    suspend fun get24hTickerSingle(
        @Query("symbol") symbol: String,
        @Header("X-MBX-APIKEY") apiKey: String? = null
    ): Response<Binance24hTickerResponse>

    @GET("api/v3/ticker/24hr")
    suspend fun get24hTickerAll(
        @Header("X-MBX-APIKEY") apiKey: String? = null
    ): Response<List<Binance24hTickerResponse>>

    @GET("api/v3/ticker/price")
    suspend fun getPriceTickerSingle(
        @Query("symbol") symbol: String,
        @Header("X-MBX-APIKEY") apiKey: String? = null
    ): Response<BinancePriceTickerResponse>

    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "1h",
        @Query("limit") limit: Int = 24
    ): Response<List<List<Any>>>
}
