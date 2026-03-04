package dev.spyglass.android.connect.client

import dev.spyglass.android.connect.*
import dev.spyglass.android.core.CrashReporter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.*
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * OkHttp WebSocket client for Spyglass Connect.
 * Handles connection, pairing, message send/receive, and heartbeat monitoring.
 */
class SpyglassClient {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MINUTES) // WebSocket keep-alive
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    val encryption = EncryptionHelper()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messages = MutableSharedFlow<SpyglassMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<SpyglassMessage> = _messages

    /** Whether the last disconnect was user-initiated. */
    private var userDisconnect = false

    /** Connect to the desktop WebSocket server. */
    fun connect(ip: String, port: Int) {
        userDisconnect = false
        _connectionState.value = ConnectionState.Connecting(ip, port)
        CrashReporter.setKey("connect_state", "connecting")
        CrashReporter.setKey("connect_ip", "$ip:$port")

        val request = Request.Builder()
            .url("ws://$ip:$port/ws")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket connected to $ip:$port")
                _connectionState.value = ConnectionState.Pairing
                CrashReporter.setKey("connect_state", "pairing")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val messageText = if (encryption.isReady) {
                    try {
                        encryption.decrypt(text)
                    } catch (e: Exception) {
                        CrashReporter.recordException(e, "Decrypt failed")
                        text
                    }
                } else {
                    text
                }

                try {
                    val message = json.decodeFromString<SpyglassMessage>(messageText)

                    // Handle pair acceptance
                    if (message.type == MessageType.PAIR_ACCEPT) {
                        val accept = json.decodeFromJsonElement(PairAcceptPayload.serializer(), message.payload)
                        if (accept.accepted) {
                            // Re-derive shared key if desktop sent its actual public key
                            // (handles case where desktop restarted with a new/persisted key pair)
                            if (accept.pubkey != null) {
                                Timber.d("Re-deriving shared key from desktop's actual pubkey")
                                encryption.deriveSharedKey(accept.pubkey)
                            }
                            _connectionState.value = ConnectionState.Connected(accept.deviceName)
                            CrashReporter.setKey("connect_state", "connected")
                            CrashReporter.setKey("connect_device", accept.deviceName)
                        } else {
                            _connectionState.value = ConnectionState.Error("Pairing rejected")
                        }
                        return
                    }

                    // Emit to message flow for UI consumption
                    _messages.tryEmit(message)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse message")
                    CrashReporter.recordException(e, "Message parse failed")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closing: $code $reason")
                webSocket.close(1000, null)
                if (!userDisconnect) {
                    _connectionState.value = ConnectionState.Error("Connection closed: $reason")
                } else {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.w(t, "WebSocket failure")
                _connectionState.value = ConnectionState.Error(t.message ?: "Connection failed")
                CrashReporter.recordException(t, "WebSocket failure")
            }
        })
    }

    /** Send a pairing request with our ECDH public key. */
    fun sendPairRequest(deviceName: String, desktopPublicKey: String) {
        // Derive shared key from desktop's public key
        encryption.deriveSharedKey(desktopPublicKey)

        val message = SpyglassMessage(
            type = MessageType.PAIR_REQUEST,
            requestId = UUID.randomUUID().toString(),
            payload = json.encodeToJsonElement(
                PairRequestPayload(
                    deviceName = deviceName,
                    pubkey = encryption.getPublicKeyBase64(),
                ),
            ),
        )

        // Send unencrypted (pairing handshake)
        send(message, encrypted = false)
    }

    /** Send a message to the desktop. */
    fun send(message: SpyglassMessage, encrypted: Boolean = true) {
        val messageJson = json.encodeToString(message)
        val text = if (encrypted && encryption.isReady) {
            encryption.encrypt(messageJson)
        } else {
            messageJson
        }
        webSocket?.send(text)
    }

    /** Send a request and generate a unique request ID. */
    fun sendRequest(type: String, payload: kotlinx.serialization.json.JsonElement = kotlinx.serialization.json.JsonNull): String {
        val requestId = UUID.randomUUID().toString()
        send(SpyglassMessage(type = type, requestId = requestId, payload = payload))
        return requestId
    }

    /** Disconnect from the desktop (user-initiated). */
    fun disconnect() {
        userDisconnect = true
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        CrashReporter.setKey("connect_state", "disconnected")
    }

    /** Check if currently connected. */
    val isConnected: Boolean
        get() = _connectionState.value is ConnectionState.Connected

    /** Whether the last disconnect was user-initiated (not an unexpected drop). */
    val wasUserDisconnect: Boolean get() = userDisconnect

    /** Set a reconnecting state (called by ViewModel during auto-reconnect). */
    fun setReconnecting(attempt: Int) {
        _connectionState.value = ConnectionState.Reconnecting(attempt)
        CrashReporter.setKey("connect_state", "reconnecting")
    }

    /** Set an error state (called by ViewModel when reconnect is exhausted). */
    fun setError(message: String) {
        _connectionState.value = ConnectionState.Error(message)
        CrashReporter.setKey("connect_state", "error")
    }
}
