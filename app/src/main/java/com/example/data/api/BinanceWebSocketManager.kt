package com.example.data.api

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

enum class WsConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

class BinanceWebSocketManager {

    private val TAG = "BinanceWS"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val wrapperAdapter = moshi.adapter(BinanceWsStreamWrapper::class.java)
    private val singleTickerAdapter = moshi.adapter(BinanceWsTickerPayload::class.java)

    private var webSocket: WebSocket? = null
    private var currentSymbols: List<String> = emptyList()

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _tickerFlow = MutableSharedFlow<BinanceWsTickerPayload>(
        replay = 1,
        extraBufferCapacity = 128
    )
    val tickerFlow: SharedFlow<BinanceWsTickerPayload> = _tickerFlow.asSharedFlow()

    fun connect(symbols: List<String>) {
        val normalizedSymbols = symbols.map { it.uppercase().trim() }.distinct().filter { it.isNotEmpty() }
        if (normalizedSymbols.isEmpty()) {
            disconnect()
            return
        }

        if (normalizedSymbols == currentSymbols && _connectionState.value == WsConnectionState.CONNECTED) {
            return // Already connected to identical symbols
        }

        currentSymbols = normalizedSymbols
        disconnectWebSocketOnly()

        _connectionState.value = WsConnectionState.CONNECTING

        val url = if (normalizedSymbols.size == 1) {
            "wss://stream.binance.com:9443/ws/${normalizedSymbols[0].lowercase()}@ticker"
        } else {
            val streams = normalizedSymbols.joinToString("/") { "${it.lowercase()}@ticker" }
            "wss://stream.binance.com:9443/stream?streams=$streams"
        }

        Log.d(TAG, "Connecting to Binance WS: $url")

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully")
                _connectionState.value = WsConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseAndEmitMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.localizedMessage}")
                _connectionState.value = WsConnectionState.ERROR
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason ($code)")
                if (_connectionState.value != WsConnectionState.DISCONNECTED) {
                    _connectionState.value = WsConnectionState.DISCONNECTED
                    scheduleReconnect()
                }
            }
        })
    }

    private fun parseAndEmitMessage(json: String) {
        scope.launch {
            try {
                // Multi-stream payload wrapper
                if (json.contains("\"stream\"")) {
                    val wrapper = wrapperAdapter.fromJson(json)
                    wrapper?.data?.let { payload ->
                        _tickerFlow.emit(payload)
                    }
                } else {
                    // Single stream direct payload
                    val payload = singleTickerAdapter.fromJson(json)
                    payload?.let {
                        _tickerFlow.emit(it)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing ticker payload: ${e.localizedMessage}")
            }
        }
    }

    private fun scheduleReconnect() {
        if (currentSymbols.isEmpty()) return
        scope.launch {
            _connectionState.value = WsConnectionState.RECONNECTING
            delay(5000) // Wait 5s before reconnecting
            if (currentSymbols.isNotEmpty()) {
                connect(currentSymbols)
            }
        }
    }

    private fun disconnectWebSocketOnly() {
        try {
            webSocket?.close(1000, "Switching streams")
            webSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing websocket: ${e.localizedMessage}")
        }
    }

    fun disconnect() {
        currentSymbols = emptyList()
        disconnectWebSocketOnly()
        _connectionState.value = WsConnectionState.DISCONNECTED
    }
}
