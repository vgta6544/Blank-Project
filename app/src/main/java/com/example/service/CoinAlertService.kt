package com.example.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.data.api.WsConnectionState
import com.example.data.repository.CoinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CoinAlertService : Service() {

    private val TAG = "CoinAlertService"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var repository: CoinRepository
    private lateinit var notificationHelper: NotificationHelper

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val NOTIFICATION_ID = 9001
    }

    override fun onCreate() {
        super.onCreate()
        repository = CoinRepository(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            stopMonitoringService()
            return START_NOT_STICKY
        }

        startMonitoringService()
        return START_STICKY
    }

    private fun startMonitoringService() {
        Log.d(TAG, "Starting CoinAlertService foreground monitoring")

        val notification = notificationHelper.buildForegroundServiceNotification(
            activeCoinsCount = 0,
            isWsConnected = false
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting foreground service: ${e.localizedMessage}")
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        repository.startPriceMonitoring()

        // Observe connection state and coin count to update foreground notification
        scope.launch {
            combine(
                repository.allWatchedCoins,
                repository.wsConnectionState
            ) { coins, wsState ->
                val count = coins.count { it.isAlertEnabled }
                val isWsConnected = wsState == WsConnectionState.CONNECTED
                Pair(count, isWsConnected)
            }.collect { (count, isConnected) ->
                val updatedNotification = notificationHelper.buildForegroundServiceNotification(
                    activeCoinsCount = count,
                    isWsConnected = isConnected
                )
                val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NOTIFICATION_ID, updatedNotification)
            }
        }
    }

    private fun stopMonitoringService() {
        Log.d(TAG, "Stopping CoinAlertService")
        repository.stopPriceMonitoring()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.stopPriceMonitoring()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
