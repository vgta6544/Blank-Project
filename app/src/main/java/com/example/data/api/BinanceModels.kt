package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Binance24hTickerResponse(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "priceChangePercent") val priceChangePercent: String?,
    @Json(name = "lastPrice") val lastPrice: String?,
    @Json(name = "highPrice") val highPrice: String?,
    @Json(name = "lowPrice") val lowPrice: String?,
    @Json(name = "volume") val volume: String?
)

@JsonClass(generateAdapter = true)
data class BinancePriceTickerResponse(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "price") val price: String
)

@JsonClass(generateAdapter = true)
data class BinanceWsStreamWrapper(
    @Json(name = "stream") val stream: String?,
    @Json(name = "data") val data: BinanceWsTickerPayload?
)

@JsonClass(generateAdapter = true)
data class BinanceWsTickerPayload(
    @Json(name = "e") val eventType: String? = null,
    @Json(name = "E") val eventTime: Long? = null,
    @Json(name = "s") val symbol: String,
    @Json(name = "c") val lastPrice: String,
    @Json(name = "P") val priceChangePercent: String,
    @Json(name = "h") val highPrice: String,
    @Json(name = "l") val lowPrice: String,
    @Json(name = "v") val volume: String
)
