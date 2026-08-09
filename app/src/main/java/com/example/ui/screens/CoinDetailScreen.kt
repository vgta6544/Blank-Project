package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.WatchedCoin
import com.example.ui.theme.BinanceCardBg
import com.example.ui.theme.BinanceDarkBg
import com.example.ui.theme.BinanceSurface
import com.example.ui.theme.BinanceYellow
import com.example.ui.theme.CryptoGreen
import com.example.ui.theme.CryptoRed
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(
    coin: WatchedCoin,
    priceHistoryKlines: List<Double>,
    onBack: () -> Unit,
    onEditThresholds: () -> Unit,
    onTestAlert: () -> Unit
) {
    val is24hUp = coin.change24h >= 0
    val changeColor = if (is24hUp) CryptoGreen else CryptoRed

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${coin.symbol} Live Alert Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BinanceDarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BinanceDarkBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Price Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BinanceCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinAvatar(baseAsset = coin.baseAsset)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = coin.symbol,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${coin.baseAsset} / ${coin.quoteAsset} • Realtime Binance Ticker",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = changeColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (is24hUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = changeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${if (is24hUp) "+" else ""}${String.format("%.2f", coin.change24h)}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = changeColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = formatPriceDisplay(coin.lastPrice),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PriceMetricItem(label = "24h HIGH", value = formatPriceDisplay(coin.high24h))
                        PriceMetricItem(label = "24h LOW", value = formatPriceDisplay(coin.low24h))
                        PriceMetricItem(label = "24h VOLUME", value = String.format("%,.0f %s", coin.volume24h, coin.baseAsset))
                    }
                }
            }

            // 24h Price History Sparkline Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BinanceCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "24H PRICE TREND",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (priceHistoryKlines.isNotEmpty()) {
                        SparklineChart(
                            prices = priceHistoryKlines,
                            lineColor = changeColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading live price chart from Binance...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Decision Support Advice Banner
            DecisionSupportBanner(coin = coin)

            // Position & P&L Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BinanceCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "YOUR POSITION & P&L",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BinanceYellow
                        )

                        IconButton(onClick = onEditThresholds) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Position")
                        }
                    }

                    if (coin.entryPrice > 0) {
                        val isProfit = coin.profitLossAmount >= 0
                        val pnlColor = if (isProfit) CryptoGreen else CryptoRed

                        DetailRow(label = "Entry Buy Price", value = formatPriceDisplay(coin.entryPrice))
                        DetailRow(label = "Holding Quantity", value = "${coin.quantity} ${coin.baseAsset}")
                        DetailRow(label = "Total Position Cost", value = "$${String.format("%,.2f", coin.totalCost)}")
                        DetailRow(label = "Current Position Value", value = "$${String.format("%,.2f", coin.totalValue)}")

                        Divider(color = BinanceSurface)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Unrealized P&L",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${if (isProfit) "+" else ""}$${String.format("%,.2f", coin.profitLossAmount)} (${String.format("%.2f", coin.profitLossPercent)}%)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = pnlColor
                            )
                        }
                    } else {
                        Text(
                            text = "No entry price set for this coin. Tap Edit to enter your buy price and holdings quantity to track position P&L.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Active Alert Threshold Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BinanceCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ALERT THRESHOLDS & COOLDOWN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    DetailRow(
                        label = "Profit Target Alert (+%)",
                        value = "+${String.format("%.1f", coin.profitTargetPercent)}% vs Entry"
                    )
                    DetailRow(
                        label = "Loss Limit Alert (-%)",
                        value = "-${String.format("%.1f", abs(coin.lossLimitPercent))}% vs Entry"
                    )
                    DetailRow(
                        label = "Market Spike Trigger",
                        value = "+${String.format("%.1f", coin.volatilityAlertUpPercent)}% in 24h"
                    )
                    DetailRow(
                        label = "Market Drop Trigger",
                        value = "-${String.format("%.1f", abs(coin.volatilityAlertDownPercent))}% in 24h"
                    )
                    DetailRow(
                        label = "Cooldown Period",
                        value = "${coin.cooldownMinutes} minutes"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onEditThresholds,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BinanceSurface, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configure Alert Thresholds")
                    }
                }
            }

            // Test Alert & Action Buttons
            Button(
                onClick = onTestAlert,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_alert_push_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BinanceYellow, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Push Notification Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PriceMetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DecisionSupportBanner(coin: WatchedCoin) {
    val (title, text, color) = when {
        coin.entryPrice > 0 && coin.profitLossPercent >= coin.profitTargetPercent -> {
            Triple(
                "💰 Profit Target Breached (+${String.format("%.1f", coin.profitLossPercent)}%)",
                "Current price $${String.format("%,.2f", coin.lastPrice)} has reached your +${String.format("%.1f", coin.profitTargetPercent)}% profit goal vs entry $${String.format("%,.2f", coin.entryPrice)}. Consider securing partial or full profits on Binance.",
                CryptoGreen
            )
        }
        coin.entryPrice > 0 && coin.profitLossPercent <= -abs(coin.lossLimitPercent) -> {
            Triple(
                "🔻 Loss Limit Breached (${String.format("%.1f", coin.profitLossPercent)}%)",
                "Current price $${String.format("%,.2f", coin.lastPrice)} is below your -${String.format("%.1f", abs(coin.lossLimitPercent))}% risk limit vs entry $${String.format("%,.2f", coin.entryPrice)}. Consider reviewing position risk management.",
                CryptoRed
            )
        }
        coin.change24h >= coin.volatilityAlertUpPercent -> {
            Triple(
                "🚀 High Market Volatility (+${String.format("%.1f", coin.change24h)}% in 24h)",
                "${coin.symbol} is up sharply today. Price may be overheated. Review market orderbook before executing manual orders.",
                BinanceYellow
            )
        }
        else -> {
            Triple(
                "ℹ️ Agent Status: Monitoring Position",
                "Price is within normal parameters ($${String.format("%,.2f", coin.lastPrice)}). P&L is ${String.format("%.1f", coin.profitLossPercent)}%. The agent will notify you when thresholds are breached.",
                MaterialTheme.colorScheme.primary
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SparklineChart(
    prices: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (prices.size < 2) return

    val minPrice = prices.minOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 1.0
    val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (prices.size - 1)

        val path = Path()
        val fillPath = Path()

        prices.forEachIndexed { index, price ->
            val x = index * stepX
            val normalizedY = (price - minPrice) / priceRange
            val y = height - (normalizedY * height).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (index == prices.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
