package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertRecordDao {
    @Query("SELECT * FROM alert_records ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertRecord>>

    @Query("SELECT * FROM alert_records WHERE symbol = :symbol ORDER BY timestamp DESC")
    fun getAlertsForSymbol(symbol: String): Flow<List<AlertRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertRecord)

    @Query("DELETE FROM alert_records")
    suspend fun clearAllAlerts()

    @Query("DELETE FROM alert_records WHERE id = :id")
    suspend fun deleteAlertById(id: Long)
}
