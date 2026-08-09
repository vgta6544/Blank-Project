package com.example.engine

import com.example.data.db.AlertRecord
import com.example.data.db.WatchedCoin
import kotlin.math.abs

class CoinAlertEngine {

    fun checkAlerts(coin: WatchedCoin, currentPrice: Double, change24h: Double): List<AlertRecord> {
        if (!coin.isAlertEnabled || currentPrice <= 0) return emptyList()

        val triggeredAlerts = mutableListOf<AlertRecord>()
        val currentTime = System.currentTimeMillis()
        val cooldownMs = coin.cooldownMinutes * 60 * 1000L

        // A. Market Volatility Check (24h change)
        val canFireVolatility = (currentTime - coin.lastVolatilityAlertTime) >= cooldownMs

        if (canFireVolatility) {
            if (change24h >= coin.volatilityAlertUpPercent) {
                val formattedPrice = formatPrice(currentPrice)
                val msg = "${coin.symbol} is up ${String.format("%.1f", change24h)}% in 24h — Current: $formattedPrice."
                val suggestion = "Price up sharply and may be overheated — review before buying or selling."

                triggeredAlerts.add(
                    AlertRecord(
                        symbol = coin.symbol,
                        alertType = "VOLATILITY_UP",
                        currentPrice = currentPrice,
                        entryPrice = coin.entryPrice,
                        percentChange = change24h,
                        message = msg,
                        suggestion = suggestion,
                        timestamp = currentTime
                    )
                )
            } else if (change24h <= coin.volatilityAlertDownPercent) {
                val formattedPrice = formatPrice(currentPrice)
                val msg = "${coin.symbol} dropped ${String.format("%.1f", abs(change24h))}% in 24h — Current: $formattedPrice."
                val suggestion = "Market volatility detected — review position and market trend."

                triggeredAlerts.add(
                    AlertRecord(
                        symbol = coin.symbol,
                        alertType = "VOLATILITY_DOWN",
                        currentPrice = currentPrice,
                        entryPrice = coin.entryPrice,
                        percentChange = change24h,
                        message = msg,
                        suggestion = suggestion,
                        timestamp = currentTime
                    )
                )
            }
        }

        // B. Position P&L Check (vs Entry Price)
        if (coin.entryPrice > 0.0) {
            val canFirePosition = (currentTime - coin.lastPositionAlertTime) >= cooldownMs
            if (canFirePosition) {
                val pctVsEntry = ((currentPrice - coin.entryPrice) / coin.entryPrice) * 100.0

                if (pctVsEntry >= coin.profitTargetPercent) {
                    val formattedPrice = formatPrice(currentPrice)
                    val formattedEntry = formatPrice(coin.entryPrice)
                    val msg = "${coin.symbol} is up ${String.format("%.1f", pctVsEntry)}% above entry — Current: $formattedPrice | Entry: $formattedEntry."
                    val suggestion = "Target profit threshold reached — consider taking partial or full profit."

                    triggeredAlerts.add(
                        AlertRecord(
                            symbol = coin.symbol,
                            alertType = "PROFIT_TARGET",
                            currentPrice = currentPrice,
                            entryPrice = coin.entryPrice,
                            percentChange = pctVsEntry,
                            message = msg,
                            suggestion = suggestion,
                            timestamp = currentTime
                        )
                    )
                } else {
                    // Normalize loss limit threshold (e.g. user enters 10.0 or -10.0)
                    val targetLossPct = if (coin.lossLimitPercent > 0) -coin.lossLimitPercent else coin.lossLimitPercent
                    if (pctVsEntry <= targetLossPct) {
                        val formattedPrice = formatPrice(currentPrice)
                        val formattedEntry = formatPrice(coin.entryPrice)
                        val msg = "${coin.symbol} dropped ${String.format("%.1f", abs(pctVsEntry))}% below entry — Current: $formattedPrice | Entry: $formattedEntry."
                        val suggestion = "Loss limit threshold breached — consider managing your risk or setting stop-loss."

                        triggeredAlerts.add(
                            AlertRecord(
                                symbol = coin.symbol,
                                alertType = "LOSS_LIMIT",
                                currentPrice = currentPrice,
                                entryPrice = coin.entryPrice,
                                percentChange = pctVsEntry,
                                message = msg,
                                suggestion = suggestion,
                                timestamp = currentTime
                            )
                        )
                    }
                }
            }
        }

        return triggeredAlerts
    }

    private fun formatPrice(price: Double): String {
        return when {
            price >= 1000.0 -> "$${String.format("%,.2f", price)}"
            price >= 1.0 -> "$${String.format("%.2f", price)}"
            price >= 0.01 -> "$${String.format("%.4f", price)}"
            else -> "$${String.format("%.6f", price)}"
        }
    }
}
