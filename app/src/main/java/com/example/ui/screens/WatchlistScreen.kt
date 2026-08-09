package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.WsConnectionState
import com.example.data.db.WatchedCoin
import com.example.ui.theme.BinanceCardBg
import com.example.ui.theme.BinanceDarkBg
import com.example.ui.theme.BinanceSurface
import com.example.ui.theme.BinanceYellow
import com.example.ui.theme.CryptoGreen
import com.example.ui.theme.CryptoRed
import com.example.ui.viewmodel.MainUiState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    uiState: MainUiState,
    onSearchQueryChange: (String) -> Unit,
    onOpenAddDialog: (WatchedCoin?) -> Unit,
    onSelectCoinForDetail: (String) -> Unit,
    onToggleCoinAlert: (WatchedCoin) -> Unit,
    onDeleteCoin: (String) -> Unit,
    onTriggerTestAlert: (WatchedCoin) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenAddDialog(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Coin") },
                text = { Text("Add Coin", fontWeight = FontWeight.Bold) },
                containerColor = BinanceYellow,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_coin_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BinanceDarkBg)
        ) {
            // Portfolio Summary Card
            PortfolioHeaderCard(
                uiState = uiState,
                modifier = Modifier.padding(16.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search coin (e.g. BTC, SOL)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("search_coin_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BinanceYellow,
                    unfocusedContainerColor = BinanceSurface,
                    focusedContainerColor = BinanceSurface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.filteredCoins.isEmpty()) {
                EmptyWatchlistState(
                    onAddClick = { onOpenAddDialog(null) }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.filteredCoins,
                        key = { it.symbol }
                    ) { coin ->
                        WatchedCoinCard(
                            coin = coin,
                            onCardClick = { onSelectCoinForDetail(coin.symbol) },
                            onToggleAlert = { onToggleCoinAlert(coin) },
                            onEditClick = { onOpenAddDialog(coin) },
                            onTestAlertClick = { onTriggerTestAlert(coin) },
                            onDeleteClick = { onDeleteCoin(coin.symbol) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioHeaderCard(
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val isProfit = uiState.totalPortfolioProfitLoss >= 0
    val pnlColor = if (isProfit) CryptoGreen else CryptoRed

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BinanceCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Text(
                    text = "TOTAL WATCHLIST VALUE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                WsStatusChip(state = uiState.wsConnectionState)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$${String.format("%,.2f", uiState.totalPortfolioValue)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = pnlColor,
                    modifier = Modifier.size(20.dp)
                )

                val pnlSign = if (isProfit) "+" else ""
                Text(
                    text = "$pnlSign$${String.format("%,.2f", uiState.totalPortfolioProfitLoss)} ($pnlSign${String.format("%.2f", uiState.totalPortfolioProfitLossPercent)}%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = pnlColor
                )

                Text(
                    text = "Unrealized P&L",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WsStatusChip(state: WsConnectionState) {
    val (text, color) = when (state) {
        WsConnectionState.CONNECTED -> "WS REALTIME" to CryptoGreen
        WsConnectionState.CONNECTING -> "CONNECTING" to BinanceYellow
        WsConnectionState.RECONNECTING -> "RECONNECTING" to BinanceYellow
        WsConnectionState.DISCONNECTED -> "REST POLLING" to MaterialTheme.colorScheme.onSurfaceVariant
        WsConnectionState.ERROR -> "WS ERROR" to CryptoRed
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WatchedCoinCard(
    coin: WatchedCoin,
    onCardClick: () -> Unit,
    onToggleAlert: () -> Unit,
    onEditClick: () -> Unit,
    onTestAlertClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val is24hUp = coin.change24h >= 0
    val change24hColor = if (is24hUp) CryptoGreen else CryptoRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("coin_card_${coin.symbol}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BinanceCardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Symbol, Price, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoinAvatar(baseAsset = coin.baseAsset)

                    Column {
                        Text(
                            text = coin.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${coin.baseAsset} / ${coin.quoteAsset}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatPriceDisplay(coin.lastPrice),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = change24hColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${if (is24hUp) "+" else ""}${String.format("%.2f", coin.change24h)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = change24hColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Thresholds") },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Trigger Test Alert Push") },
                                onClick = {
                                    menuExpanded = false
                                    onTestAlertClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Chart & Details") },
                                onClick = {
                                    menuExpanded = false
                                    onCardClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove from Watchlist", color = CryptoRed) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = BinanceSurface)

            // Position & PnL Breakdown Row (if entry set)
            if (coin.entryPrice > 0) {
                val isPnlProfit = coin.profitLossAmount >= 0
                val pnlColor = if (isPnlProfit) CryptoGreen else CryptoRed

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ENTRY PRICE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatPriceDisplay(coin.entryPrice),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "HOLDINGS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${coin.quantity} ${coin.baseAsset}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "POSITION P&L",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (isPnlProfit) "+" else ""}$${String.format("%.2f", coin.profitLossAmount)} (${String.format("%.1f", coin.profitLossPercent)}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = pnlColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Alert Thresholds Badges & Active Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThresholdPill(
                        label = "Profit +${String.format("%.0f", coin.profitTargetPercent)}%",
                        color = CryptoGreen
                    )
                    ThresholdPill(
                        label = "Loss -${String.format("%.0f", abs(coin.lossLimitPercent))}%",
                        color = CryptoRed
                    )
                    ThresholdPill(
                        label = "Vol ±${String.format("%.0f", coin.volatilityAlertUpPercent)}%",
                        color = BinanceYellow
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (coin.isAlertEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (coin.isAlertEnabled) BinanceYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Switch(
                        checked = coin.isAlertEnabled,
                        onCheckedChange = { onToggleAlert() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = BinanceYellow
                        ),
                        modifier = Modifier.testTag("alert_switch_${coin.symbol}")
                    )
                }
            }
        }
    }
}

@Composable
fun CoinAvatar(baseAsset: String) {
    val bgColors = when (baseAsset) {
        "BTC" -> listOf(Color(0xFFF7931A), Color(0xFFFFB049))
        "ETH" -> listOf(Color(0xFF627EEA), Color(0xFF8CA0F3))
        "SOL" -> listOf(Color(0xFF14F195), Color(0xFF9945FF))
        "BNB" -> listOf(Color(0xFFF3BA2F), Color(0xFFFFD875))
        "XRP" -> listOf(Color(0xFF23292F), Color(0xFF00AAE4))
        else -> listOf(Color(0xFF2B313A), Color(0xFF474D57))
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(bgColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = baseAsset.take(3),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun ThresholdPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyWatchlistState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShowChart,
            contentDescription = null,
            tint = BinanceYellow,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Watchlist is Empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add coins to monitor Binance WebSocket live prices and receive profit/loss push alerts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = BinanceYellow, contentColor = Color.Black)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Coin Now", fontWeight = FontWeight.Bold)
        }
    }
}

fun formatPriceDisplay(price: Double): String {
    return when {
        price >= 1000.0 -> "$${String.format("%,.2f", price)}"
        price >= 1.0 -> "$${String.format("%.2f", price)}"
        price >= 0.01 -> "$${String.format("%.4f", price)}"
        else -> "$${String.format("%.6f", price)}"
    }
}
