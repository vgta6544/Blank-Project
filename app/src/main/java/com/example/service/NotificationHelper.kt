package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AlertRecord

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_ALERTS = "binance_coin_alerts"
        const val CHANNEL_NAME_ALERTS = "Binance Price & Position Alerts"
        const val CHANNEL_ID_SERVICE = "binance_alert_service"
        const val CHANNEL_NAME_SERVICE = "Monitoring Agent Service"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                CHANNEL_NAME_ALERTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications for coin threshold breaches"
                enableVibration(true)
                enableLights(true)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                CHANNEL_NAME_SERVICE,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background Binance price monitoring status"
            }

            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun sendCoinAlertNotification(alert: AlertRecord) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TARGET_SYMBOL", alert.symbol)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.symbol.hashCode() + alert.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (alert.alertType) {
            "VOLATILITY_UP" -> "🚀 ${alert.symbol} Price Spike (+${String.format("%.1f", alert.percentChange)}%)"
            "VOLATILITY_DOWN" -> "⚠️ ${alert.symbol} Rapid Drop (${String.format("%.1f", alert.percentChange)}%)"
            "PROFIT_TARGET" -> "💰 ${alert.symbol} Profit Target Hit (+${String.format("%.1f", alert.percentChange)}%)"
            "LOSS_LIMIT" -> "🔻 ${alert.symbol} Loss Limit Breached (${String.format("%.1f", alert.percentChange)}%)"
            else -> "🔔 ${alert.symbol} Alert Triggered"
        }

        val body = "${alert.message}\n${alert.suggestion}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationId = (alert.timestamp % 100000).toInt() + alert.symbol.hashCode()
        notificationManager.notify(notificationId, builder.build())
    }

    fun buildForegroundServiceNotification(activeCoinsCount: Int, isWsConnected: Boolean): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isWsConnected) "WebSocket Connected (Realtime)" else "Polling Binance REST API"
        val contentText = "Monitoring $activeCoinsCount coin(s) • $statusText"

        return NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Binance Alert Agent Running")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
