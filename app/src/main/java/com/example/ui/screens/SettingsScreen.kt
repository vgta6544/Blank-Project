package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BinanceCardBg
import com.example.ui.theme.BinanceDarkBg
import com.example.ui.theme.BinanceYellow
import com.example.ui.theme.CryptoGreen
import com.example.ui.theme.CryptoRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isServiceRunning: Boolean,
    binanceApiKey: String,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onSaveApiKey: (String) -> Unit
) {
    var apiKeyText by remember { mutableStateOf(binanceApiKey) }
    var keySavedMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Settings & Security", fontWeight = FontWeight.Bold) },
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
            // Background Monitoring Agent Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BinanceCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "BACKGROUND MONITORING AGENT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BinanceYellow
                    )

                    Text(
                        text = "The agent runs a Kotlin foreground service connected to Binance WebSockets, evaluating price volatility, profit target, and loss limits even when the app is closed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isServiceRunning) Icons.Default.PlayArrow else Icons.Default.Stop,
                                contentDescription = null,
                                tint = if (isServiceRunning) CryptoGreen else CryptoRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServiceRunning) "Service Active (WebSocket + Push)" else "Service Stopped",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { checked ->
                                if (checked) onStartService() else onStopService()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = BinanceYellow
                            ),
                            modifier = Modifier.testTag("service_toggle_switch")
                        )
                    }
                }
            }

            // Read-Only Binance API Key Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BinanceCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = BinanceYellow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "READ-ONLY BINANCE API KEY (OPTIONAL)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "You may supply an optional READ-ONLY Binance API Key for higher rate limits or REST queries. Never use trade or withdrawal permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = {
                            apiKeyText = it
                            keySavedMessage = false
                        },
                        label = { Text("Binance Read-Only API Key") },
                        placeholder = { Text("e.g. 64-character public key") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("binance_api_key_field"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            onSaveApiKey(apiKeyText)
                            keySavedMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BinanceYellow, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Read-Only API Key", fontWeight = FontWeight.Bold)
                    }

                    if (keySavedMessage) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = CryptoGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "API Key saved securely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CryptoGreen
                            )
                        }
                    }
                }
            }

            // Security & Decision Support Disclaimer Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = CryptoGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURITY & DECISION-SUPPORT NOTICE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "• NOT AN AUTO-TRADING BOT: This agent sends decision-support notifications. You make all buy/sell trade decisions manually on Binance.\n• NO TRADE/WITHDRAWAL KEYS: Never enter keys with trade or withdrawal permissions.\n• COOLDOWN ENGINE: Alerts respect per-coin cooldowns (default 60m) to prevent alert fatigue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
