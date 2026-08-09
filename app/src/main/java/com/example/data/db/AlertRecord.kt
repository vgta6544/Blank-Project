package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_records")
data class AlertRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val alertType: String, // "VOLATILITY_UP", "VOLATILITY_DOWN", "PROFIT_TARGET", "LOSS_LIMIT"
    val currentPrice: Double,
    val entryPrice: Double,
    val percentChange: Double,
    val message: String,
    val suggestion: String,
    val timestamp: Long = System.currentTimeMillis()
)
