package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AddEditCoinDialog
import com.example.ui.screens.AlertHistoryScreen
import com.example.ui.screens.CoinDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.BinanceDarkBg
import com.example.ui.theme.BinanceYellow
import com.example.ui.theme.CoinAlertTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted for alerts", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkNotificationPermission()
        handleNotificationIntent(intent)

        setContent {
            CoinAlertTheme {
                MainAppScreen(
                    viewModel = viewModel,
                    onHandleIntentSymbol = { symbol ->
                        viewModel.selectCoinForDetail(symbol)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val targetSymbol = intent?.getStringExtra("EXTRA_TARGET_SYMBOL")
        if (!targetSymbol.isNullOrEmpty()) {
            viewModel.selectCoinForDetail(targetSymbol)
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    onHandleIntentSymbol: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // If detail coin is selected, show detail screen!
    val selectedCoin = uiState.selectedCoinForDetail
    if (selectedCoin != null) {
        CoinDetailScreen(
            coin = selectedCoin,
            priceHistoryKlines = uiState.priceHistoryKlines,
            onBack = { viewModel.selectCoinForDetail(null) },
            onEditThresholds = { viewModel.openAddCoinDialog(selectedCoin) },
            onTestAlert = { viewModel.triggerTestAlert(selectedCoin) }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = BinanceDarkBg,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = "Watchlist") },
                    label = { Text("Watchlist", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = BinanceYellow,
                        indicatorColor = BinanceYellow
                    ),
                    modifier = Modifier.testTag("watchlist_tab")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alert Log") },
                    label = { Text("Alert Log", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = BinanceYellow,
                        indicatorColor = BinanceYellow
                    ),
                    modifier = Modifier.testTag("alert_log_tab")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = BinanceYellow,
                        indicatorColor = BinanceYellow
                    ),
                    modifier = Modifier.testTag("settings_tab")
                )
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> WatchlistScreen(
                    uiState = uiState,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onOpenAddDialog = { coin -> viewModel.openAddCoinDialog(coin) },
                    onSelectCoinForDetail = { symbol -> viewModel.selectCoinForDetail(symbol) },
                    onToggleCoinAlert = { coin -> viewModel.toggleCoinAlert(coin) },
                    onDeleteCoin = { symbol -> viewModel.deleteCoin(symbol) },
                    onTriggerTestAlert = { coin -> viewModel.triggerTestAlert(coin) }
                )
                1 -> AlertHistoryScreen(
                    alertRecords = uiState.alertRecords,
                    onClearHistory = { viewModel.clearAlertHistory() },
                    onSelectSymbol = { symbol -> viewModel.selectCoinForDetail(symbol) }
                )
                2 -> SettingsScreen(
                    isServiceRunning = uiState.isServiceRunning,
                    binanceApiKey = uiState.binanceApiKey,
                    onStartService = { viewModel.startService() },
                    onStopService = { viewModel.stopService() },
                    onSaveApiKey = { key -> viewModel.saveBinanceApiKey(key) }
                )
            }

            if (uiState.isAddCoinDialogOpen) {
                AddEditCoinDialog(
                    coinToEdit = uiState.coinToEdit,
                    onDismiss = { viewModel.closeAddCoinDialog() },
                    onSave = { symbol, entryPrice, quantity, volUp, volDown, profitTarget, lossLimit, cooldown ->
                        viewModel.saveCoin(
                            symbol,
                            entryPrice,
                            quantity,
                            volUp,
                            volDown,
                            profitTarget,
                            lossLimit,
                            cooldown
                        )
                    }
                )
            }
        }
    }
}
