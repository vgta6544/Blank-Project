package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.WsConnectionState
import com.example.data.db.AlertRecord
import com.example.data.db.WatchedCoin
import com.example.data.repository.CoinRepository
import com.example.service.CoinAlertService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val watchedCoins: List<WatchedCoin> = emptyList(),
    val filteredCoins: List<WatchedCoin> = emptyList(),
    val alertRecords: List<AlertRecord> = emptyList(),
    val wsConnectionState: WsConnectionState = WsConnectionState.DISCONNECTED,
    val selectedCoinForDetail: WatchedCoin? = null,
    val priceHistoryKlines: List<Double> = emptyList(),
    val isAddCoinDialogOpen: Boolean = false,
    val coinToEdit: WatchedCoin? = null,
    val isServiceRunning: Boolean = true,
    val searchQuery: String = "",
    val binanceApiKey: String = "",
    val userMessage: String? = null,
    val totalPortfolioValue: Double = 0.0,
    val totalPortfolioCost: Double = 0.0,
    val totalPortfolioProfitLoss: Double = 0.0,
    val totalPortfolioProfitLossPercent: Double = 0.0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = CoinRepository(application)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCoinSymbol = MutableStateFlow<String?>(null)
    private val _priceHistory = MutableStateFlow<List<Double>>(emptyList())
    private val _isAddCoinDialogOpen = MutableStateFlow(false)
    private val _coinToEdit = MutableStateFlow<WatchedCoin?>(null)
    private val _isServiceRunning = MutableStateFlow(true)
    private val _binanceApiKey = MutableStateFlow("")
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        repository.allWatchedCoins,
        repository.allAlertRecords,
        repository.wsConnectionState,
        _searchQuery,
        _selectedCoinSymbol,
        _priceHistory,
        _isAddCoinDialogOpen,
        _coinToEdit,
        _isServiceRunning,
        _binanceApiKey,
        _userMessage
    ) { args ->
        val coins = args[0] as List<WatchedCoin>
        val alerts = args[1] as List<AlertRecord>
        val wsState = args[2] as WsConnectionState
        val query = args[3] as String
        val selectedSymbol = args[4] as String?
        val klines = args[5] as List<Double>
        val isAddOpen = args[6] as Boolean
        val editCoin = args[7] as WatchedCoin?
        val isServiceOn = args[8] as Boolean
        val apiKey = args[9] as String
        val msg = args[10] as String?

        val filtered = if (query.isBlank()) {
            coins
        } else {
            coins.filter {
                it.symbol.contains(query, ignoreCase = true) ||
                it.baseAsset.contains(query, ignoreCase = true)
            }
        }

        val selectedCoin = coins.find { it.symbol == selectedSymbol }

        var totalVal = 0.0
        var totalCost = 0.0
        coins.forEach { coin ->
            totalVal += coin.totalValue
            totalCost += coin.totalCost
        }
        val totalPnl = totalVal - totalCost
        val totalPnlPct = if (totalCost > 0) (totalPnl / totalCost) * 100.0 else 0.0

        MainUiState(
            watchedCoins = coins,
            filteredCoins = filtered,
            alertRecords = alerts,
            wsConnectionState = wsState,
            selectedCoinForDetail = selectedCoin,
            priceHistoryKlines = klines,
            isAddCoinDialogOpen = isAddOpen,
            coinToEdit = editCoin,
            isServiceRunning = isServiceOn,
            searchQuery = query,
            binanceApiKey = apiKey,
            userMessage = msg,
            totalPortfolioValue = totalVal,
            totalPortfolioCost = totalCost,
            totalPortfolioProfitLoss = totalPnl,
            totalPortfolioProfitLossPercent = totalPnlPct
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        startService()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openAddCoinDialog(coin: WatchedCoin? = null) {
        _coinToEdit.value = coin
        _isAddCoinDialogOpen.value = true
    }

    fun closeAddCoinDialog() {
        _isAddCoinDialogOpen.value = false
        _coinToEdit.value = null
    }

    fun selectCoinForDetail(symbol: String?) {
        _selectedCoinSymbol.value = symbol
        if (symbol != null) {
            viewModelScope.launch {
                val klines = repository.fetchPriceHistory(symbol)
                _priceHistory.value = klines
            }
        } else {
            _priceHistory.value = emptyList()
        }
    }

    fun saveCoin(
        rawSymbol: String,
        entryPrice: Double,
        quantity: Double,
        volUp: Double,
        volDown: Double,
        profitTarget: Double,
        lossLimit: Double,
        cooldownMins: Int
    ) {
        val symbol = rawSymbol.uppercase().trim().run {
            if (!endsWith("USDT") && !endsWith("BUSD") && !endsWith("BTC")) {
                "${this}USDT"
            } else this
        }

        val baseAsset = symbol.replace("USDT", "").replace("BUSD", "").replace("BTC", "")

        val newCoin = WatchedCoin(
            symbol = symbol,
            baseAsset = if (baseAsset.isNotEmpty()) baseAsset else symbol,
            quoteAsset = "USDT",
            entryPrice = entryPrice,
            quantity = quantity,
            volatilityAlertUpPercent = volUp,
            volatilityAlertDownPercent = if (volDown > 0) -volDown else volDown,
            profitTargetPercent = profitTarget,
            lossLimitPercent = if (lossLimit > 0) -lossLimit else lossLimit,
            cooldownMinutes = cooldownMins,
            isAlertEnabled = true
        )

        viewModelScope.launch {
            repository.addOrUpdateCoin(newCoin)
            _userMessage.value = "Saved watchlist coin $symbol"
            closeAddCoinDialog()
        }
    }

    fun deleteCoin(symbol: String) {
        viewModelScope.launch {
            repository.deleteCoin(symbol)
            if (_selectedCoinSymbol.value == symbol) {
                _selectedCoinSymbol.value = null
            }
            _userMessage.value = "Removed $symbol from watchlist"
        }
    }

    fun toggleCoinAlert(coin: WatchedCoin) {
        viewModelScope.launch {
            val updated = coin.copy(isAlertEnabled = !coin.isAlertEnabled)
            repository.addOrUpdateCoin(updated)
        }
    }

    fun clearAlertHistory() {
        viewModelScope.launch {
            repository.clearAlertHistory()
            _userMessage.value = "Alert history cleared"
        }
    }

    fun triggerTestAlert(coin: WatchedCoin) {
        viewModelScope.launch {
            repository.triggerTestAlert(coin)
            _userMessage.value = "Triggered test alert for ${coin.symbol}!"
        }
    }

    fun saveBinanceApiKey(key: String) {
        _binanceApiKey.value = key
        _userMessage.value = "Binance Read-Only API key saved securely."
    }

    fun startService() {
        val context = getApplication<Application>()
        val intent = Intent(context, CoinAlertService::class.java).apply {
            action = CoinAlertService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _isServiceRunning.value = true
    }

    fun stopService() {
        val context = getApplication<Application>()
        val intent = Intent(context, CoinAlertService::class.java).apply {
            action = CoinAlertService.ACTION_STOP
        }
        context.startService(intent)
        _isServiceRunning.value = false
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
