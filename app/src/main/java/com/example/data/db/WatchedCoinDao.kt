package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedCoinDao {
    @Query("SELECT * FROM watched_coins ORDER BY symbol ASC")
    fun getAllCoins(): Flow<List<WatchedCoin>>

    @Query("SELECT * FROM watched_coins ORDER BY symbol ASC")
    suspend fun getAllCoinsList(): List<WatchedCoin>

    @Query("SELECT * FROM watched_coins WHERE symbol = :symbol LIMIT 1")
    suspend fun getCoinBySymbol(symbol: String): WatchedCoin?

    @Query("SELECT * FROM watched_coins WHERE symbol = :symbol LIMIT 1")
    fun getCoinBySymbolFlow(symbol: String): Flow<WatchedCoin?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoin(coin: WatchedCoin)

    @Update
    suspend fun updateCoin(coin: WatchedCoin)

    @Query("DELETE FROM watched_coins WHERE symbol = :symbol")
    suspend fun deleteCoinBySymbol(symbol: String)

    @Query("UPDATE watched_coins SET lastPrice = :price, change24h = :change24h, high24h = :high24h, low24h = :low24h, volume24h = :volume24h WHERE symbol = :symbol")
    suspend fun updatePriceMetrics(symbol: String, price: Double, change24h: Double, high24h: Double, low24h: Double, volume24h: Double)

    @Query("UPDATE watched_coins SET lastVolatilityAlertTime = :timestamp WHERE symbol = :symbol")
    suspend fun updateLastVolatilityAlertTime(symbol: String, timestamp: Long)

    @Query("UPDATE watched_coins SET lastPositionAlertTime = :timestamp WHERE symbol = :symbol")
    suspend fun updateLastPositionAlertTime(symbol: String, timestamp: Long)
}
