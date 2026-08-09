package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.db.WatchedCoin
import com.example.ui.theme.BinanceYellow
import com.example.ui.theme.CryptoGreen
import com.example.ui.theme.CryptoRed

val TOP_BINANCE_SYMBOLS = listOf(
    "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT",
    "ADAUSDT", "DOGEUSDT", "AVAXUSDT", "LINKUSDT", "SHIBUSDT",
    "NEARUSDT", "SUIUSDT", "PEPEUSDT", "DOTUSDT", "MATICUSDT"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCoinDialog(
    coinToEdit: WatchedCoin?,
    onDismiss: () -> Unit,
    onSave: (
        symbol: String,
        entryPrice: Double,
        quantity: Double,
        volUp: Double,
        volDown: Double,
        profitTarget: Double,
        lossLimit: Double,
        cooldownMins: Int
    ) -> Unit
) {
    var symbol by remember { mutableStateOf(coinToEdit?.symbol ?: "BTCUSDT") }
    var entryPriceText by remember { mutableStateOf(coinToEdit?.entryPrice?.takeIf { it > 0 }?.toString() ?: "") }
    var quantityText by remember { mutableStateOf(coinToEdit?.quantity?.takeIf { it > 0 }?.toString() ?: "") }
    var volUpText by remember { mutableStateOf(coinToEdit?.volatilityAlertUpPercent?.toString() ?: "10.0") }
    var volDownText by remember { mutableStateOf(coinToEdit?.volatilityAlertDownPercent?.let { kotlin.math.abs(it) }?.toString() ?: "10.0") }
    var profitTargetText by remember { mutableStateOf(coinToEdit?.profitTargetPercent?.toString() ?: "15.0") }
    var lossLimitText by remember { mutableStateOf(coinToEdit?.lossLimitPercent?.let { kotlin.math.abs(it) }?.toString() ?: "10.0") }
    var cooldownText by remember { mutableStateOf(coinToEdit?.cooldownMinutes?.toString() ?: "60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_edit_coin_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (coinToEdit == null) "Add Coin to Watchlist" else "Edit Alert Thresholds (${coinToEdit.symbol})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (coinToEdit == null) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.uppercase() },
                        label = { Text("Coin Symbol (e.g. BTCUSDT)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("symbol_input_field"),
                        singleLine = true
                    )

                    Text(
                        text = "Popular Symbols:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(TOP_BINANCE_SYMBOLS) { item ->
                            FilterChip(
                                selected = symbol == item,
                                onClick = { symbol = item },
                                label = { Text(item) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BinanceYellow,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Position / Buy Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BinanceYellow
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = entryPriceText,
                        onValueChange = { entryPriceText = it },
                        label = { Text("Entry Price ($)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("entry_price_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quantity_field"),
                        singleLine = true
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "A. Volatility Alerts (24h Market Change)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = volUpText,
                        onValueChange = { volUpText = it },
                        label = { Text("Price Spike (+%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = volDownText,
                        onValueChange = { volDownText = it },
                        label = { Text("Price Drop (-%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "B. Position P&L Alerts (vs Entry)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = profitTargetText,
                        onValueChange = { profitTargetText = it },
                        label = { Text("Profit Target (+%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = lossLimitText,
                        onValueChange = { lossLimitText = it },
                        label = { Text("Loss Limit (-%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = cooldownText,
                    onValueChange = { cooldownText = it },
                    label = { Text("Alert Cooldown (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entryPrice = entryPriceText.toDoubleOrNull() ?: 0.0
                    val quantity = quantityText.toDoubleOrNull() ?: 0.0
                    val volUp = volUpText.toDoubleOrNull() ?: 10.0
                    val volDown = volDownText.toDoubleOrNull() ?: 10.0
                    val profitTarget = profitTargetText.toDoubleOrNull() ?: 15.0
                    val lossLimit = lossLimitText.toDoubleOrNull() ?: 10.0
                    val cooldownMins = cooldownText.toIntOrNull() ?: 60

                    onSave(
                        symbol,
                        entryPrice,
                        quantity,
                        volUp,
                        volDown,
                        profitTarget,
                        lossLimit,
                        cooldownMins
                    )
                },
                modifier = Modifier.testTag("save_coin_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BinanceYellow, contentColor = Color.Black)
            ) {
                Text("Save Watchlist Coin", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
