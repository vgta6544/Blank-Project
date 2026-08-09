package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched_coins")
data class WatchedCoin(
    @PrimaryKey
    val symbol: String, // e.g. "BTCUSDT"
    val baseAsset: String = "BTC", // e.g. "BTC"
    val quoteAsset: String = "USDT", // e.g. "USDT"
    val entryPrice: Double = 0.0,
    val quantity: Double = 0.0,
    val volatilityAlertUpPercent: Double = 10.0, // e.g. +10% 24h change
    val volatilityAlertDownPercent: Double = -10.0, // e.g. -10% 24h change
    val profitTargetPercent: Double = 15.0, // e.g. +15% vs entry
    val lossLimitPercent: Double = -10.0, // e.g. -10% vs entry
    val cooldownMinutes: Int = 60, // 1 hour default cooldown
    val lastVolatilityAlertTime: Long = 0L,
    val lastPositionAlertTime: Long = 0L,
    val lastPrice: Double = 0.0,
    val change24h: Double = 0.0,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val volume24h: Double = 0.0,
    val isAlertEnabled: Boolean = true
) {
    val totalValue: Double get() = quantity * lastPrice
    val totalCost: Double get() = quantity * entryPrice
    val profitLossAmount: Double get() = totalValue - totalCost
    val profitLossPercent: Double
        get() = if (entryPrice > 0) ((lastPrice - entryPrice) / entryPrice) * 100.0 else 0.0
}
