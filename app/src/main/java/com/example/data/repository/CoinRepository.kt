package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.Binance24hTickerResponse
import com.example.data.api.BinanceApiService
import com.example.data.api.BinanceWebSocketManager
import com.example.data.api.WsConnectionState
import com.example.data.db.AlertRecord
import com.example.data.db.AlertRecordDao
import com.example.data.db.AppDatabase
import com.example.data.db.WatchedCoin
import com.example.data.db.WatchedCoinDao
import com.example.engine.CoinAlertEngine
import com.example.service.NotificationHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class CoinRepository(private val context: Context) {

    private val TAG = "CoinRepository"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val db = AppDatabase.getDatabase(context)
    val coinDao: WatchedCoinDao = db.watchedCoinDao()
    val alertDao: AlertRecordDao = db.alertRecordDao()

    val allWatchedCoins: Flow<List<WatchedCoin>> = coinDao.getAllCoins()
    val allAlertRecords: Flow<List<AlertRecord>> = alertDao.getAllAlerts()

    val wsManager = BinanceWebSocketManager()
    val wsConnectionState: StateFlow<WsConnectionState> = wsManager.connectionState

    private val alertEngine = CoinAlertEngine()
    private val notificationHelper = NotificationHelper(context)

    private var monitoringJob: Job? = null
    private var restPollingJob: Job? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.binance.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: BinanceApiService = retrofit.create(BinanceApiService::class.java)

    init {
        scope.launch {
            initDefaultWatchlistIfNeeded()
        }
    }

    private suspend fun initDefaultWatchlistIfNeeded() {
        val existing = coinDao.getAllCoinsList()
        if (existing.isEmpty()) {
            val defaults = listOf(
                WatchedCoin(
                    symbol = "BTCUSDT",
                    baseAsset = "BTC",
                    quoteAsset = "USDT",
                    entryPrice = 62000.0,
                    quantity = 0.25,
                    volatilityAlertUpPercent = 10.0,
                    volatilityAlertDownPercent = -10.0,
                    profitTargetPercent = 15.0,
                    lossLimitPercent = -10.0
                ),
                WatchedCoin(
                    symbol = "ETHUSDT",
                    baseAsset = "ETH",
                    quoteAsset = "USDT",
                    entryPrice = 3100.0,
                    quantity = 2.0,
                    volatilityAlertUpPercent = 10.0,
                    volatilityAlertDownPercent = -10.0,
                    profitTargetPercent = 15.0,
                    lossLimitPercent = -10.0
                ),
                WatchedCoin(
                    symbol = "SOLUSDT",
                    baseAsset = "SOL",
                    quoteAsset = "USDT",
                    entryPrice = 145.0,
                    quantity = 15.0,
                    volatilityAlertUpPercent = 12.0,
                    volatilityAlertDownPercent = -12.0,
                    profitTargetPercent = 20.0,
                    lossLimitPercent = -10.0
                ),
                WatchedCoin(
                    symbol = "BNBUSDT",
                    baseAsset = "BNB",
                    quoteAsset = "USDT",
                    entryPrice = 570.0,
                    quantity = 4.0,
                    volatilityAlertUpPercent = 10.0,
                    volatilityAlertDownPercent = -10.0,
                    profitTargetPercent = 15.0,
                    lossLimitPercent = -10.0
                )
            )
            defaults.forEach { coinDao.insertCoin(it) }
        }
    }

    fun startPriceMonitoring() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = scope.launch {
            // Observe coin list changes and update WS subscriptions
            launch {
                allWatchedCoins.collect { coins ->
                    val symbols = coins.map { it.symbol }
                    if (symbols.isNotEmpty()) {
                        wsManager.connect(symbols)
                    } else {
                        wsManager.disconnect()
                    }
                }
            }

            // Collect WebSocket ticker stream
            launch {
                wsManager.tickerFlow.collect { payload ->
                    processTickerPayload(
                        symbol = payload.symbol.uppercase(),
                        priceStr = payload.lastPrice,
                        change24hStr = payload.priceChangePercent,
                        highStr = payload.highPrice,
                        lowStr = payload.lowPrice,
                        volumeStr = payload.volume
                    )
                }
            }
        }

        // Setup REST polling fallback every 15s
        startRestPollingFallback()
    }

    private fun startRestPollingFallback() {
        if (restPollingJob?.isActive == true) return
        restPollingJob = scope.launch {
            while (true) {
                try {
                    // Poll if WS is not connected or as periodic sync
                    if (wsManager.connectionState.value != WsConnectionState.CONNECTED) {
                        pollRestTickers()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in REST polling fallback: ${e.localizedMessage}")
                }
                delay(15000)
            }
        }
    }

    private suspend fun pollRestTickers() {
        val watchedList = coinDao.getAllCoinsList()
        if (watchedList.isEmpty()) return

        try {
            val response = apiService.get24hTickerAll()
            if (response.isSuccessful) {
                val allTickers = response.body() ?: emptyList()
                val tickerMap = allTickers.associateBy { it.symbol.uppercase() }

                watchedList.forEach { coin ->
                    tickerMap[coin.symbol.uppercase()]?.let { ticker ->
                        processTickerPayload(
                            symbol = coin.symbol,
                            priceStr = ticker.lastPrice ?: "0",
                            change24hStr = ticker.priceChangePercent ?: "0",
                            highStr = ticker.highPrice ?: "0",
                            lowStr = ticker.lowPrice ?: "0",
                            volumeStr = ticker.volume ?: "0"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed REST polling call: ${e.localizedMessage}")
        }
    }

    private suspend fun processTickerPayload(
        symbol: String,
        priceStr: String,
        change24hStr: String,
        highStr: String,
        lowStr: String,
        volumeStr: String
    ) {
        val price = priceStr.toDoubleOrNull() ?: return
        val change24h = change24hStr.toDoubleOrNull() ?: 0.0
        val high24h = highStr.toDoubleOrNull() ?: price
        val low24h = lowStr.toDoubleOrNull() ?: price
        val volume24h = volumeStr.toDoubleOrNull() ?: 0.0

        val coin = coinDao.getCoinBySymbol(symbol) ?: return

        // Update database with live metrics
        coinDao.updatePriceMetrics(
            symbol = symbol,
            price = price,
            change24h = change24h,
            high24h = high24h,
            low24h = low24h,
            volume24h = volume24h
        )

        // Evaluate alert engine rules
        val updatedCoin = coin.copy(
            lastPrice = price,
            change24h = change24h,
            high24h = high24h,
            low24h = low24h,
            volume24h = volume24h
        )

        val triggeredAlerts = alertEngine.checkAlerts(updatedCoin, price, change24h)

        triggeredAlerts.forEach { alert ->
            alertDao.insertAlert(alert)
            notificationHelper.sendCoinAlertNotification(alert)

            if (alert.alertType.startsWith("VOLATILITY")) {
                coinDao.updateLastVolatilityAlertTime(symbol, alert.timestamp)
            } else if (alert.alertType.startsWith("PROFIT") || alert.alertType.startsWith("LOSS")) {
                coinDao.updateLastPositionAlertTime(symbol, alert.timestamp)
            }
        }
    }

    fun stopPriceMonitoring() {
        monitoringJob?.cancel()
        restPollingJob?.cancel()
        wsManager.disconnect()
    }

    suspend fun addOrUpdateCoin(coin: WatchedCoin) = withContext(Dispatchers.IO) {
        coinDao.insertCoin(coin)
        // Fetch immediate 24h ticker for new coin
        try {
            val response = apiService.get24hTickerSingle(coin.symbol)
            if (response.isSuccessful) {
                response.body()?.let { ticker ->
                    processTickerPayload(
                        symbol = coin.symbol,
                        priceStr = ticker.lastPrice ?: "0",
                        change24hStr = ticker.priceChangePercent ?: "0",
                        highStr = ticker.highPrice ?: "0",
                        lowStr = ticker.lowPrice ?: "0",
                        volumeStr = ticker.volume ?: "0"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching initial ticker for ${coin.symbol}: ${e.localizedMessage}")
        }
    }

    suspend fun deleteCoin(symbol: String) = withContext(Dispatchers.IO) {
        coinDao.deleteCoinBySymbol(symbol)
    }

    suspend fun clearAlertHistory() = withContext(Dispatchers.IO) {
        alertDao.clearAllAlerts()
    }

    suspend fun triggerTestAlert(coin: WatchedCoin) = withContext(Dispatchers.IO) {
        val simulatedPrice = if (coin.lastPrice > 0) coin.lastPrice * 1.16 else 72500.0
        val simulatedChange = 12.5
        val testAlert = AlertRecord(
            symbol = coin.symbol,
            alertType = "PROFIT_TARGET",
            currentPrice = simulatedPrice,
            entryPrice = coin.entryPrice,
            percentChange = 16.0,
            message = "${coin.symbol} is up +16.0% above entry — Current: $${String.format("%,.2f", simulatedPrice)} | Entry: $${String.format("%,.2f", coin.entryPrice)}.",
            suggestion = "Profit target breached! Consider securing profit on Binance.",
            timestamp = System.currentTimeMillis()
        )
        alertDao.insertAlert(testAlert)
        notificationHelper.sendCoinAlertNotification(testAlert)
    }

    suspend fun fetchPriceHistory(symbol: String): List<Double> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getKlines(symbol, "1h", 24)
            if (response.isSuccessful) {
                val klines = response.body() ?: emptyList()
                return@withContext klines.mapNotNull { item ->
                    if (item.size > 4) {
                        item[4].toString().toDoubleOrNull()
                    } else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching klines: ${e.localizedMessage}")
        }
        return@withContext emptyList()
    }
}
