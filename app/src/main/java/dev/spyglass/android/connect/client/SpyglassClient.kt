package dev.spyglass.android.connect.client

import dev.spyglass.android.connect.*
import dev.spyglass.android.BuildConfig
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

    private val _negotiatedCapabilities = MutableStateFlow<Set<String>>(emptySet())
    val negotiatedCapabilities: StateFlow<Set<String>> = _negotiatedCapabilities

    /** Whether the last disconnect was user-initiated. */
    private var userDisconnect = false

    /** Returns true if the IP is a private/LAN address (RFC 1918 + loopback). */
    private fun isPrivateIp(ip: String): Boolean {
        return ip.startsWith("10.") ||
            ip.startsWith("192.168.") ||
            ip.startsWith("172.") && ip.split(".").getOrNull(1)?.toIntOrNull()?.let { it in 16..31 } == true ||
            ip.startsWith("127.") ||
            ip == "localhost"
    }

    /** Connect to the desktop WebSocket server. Only allows private/LAN IPs. */
    fun connect(ip: String, port: Int) {
        if (!isPrivateIp(ip)) {
            Timber.w("Connect rejected: $ip is not a private/LAN IP")
            _connectionState.value = ConnectionState.Error("Connection rejected: only local network IPs allowed")
            return
        }
        userDisconnect = false
        Timber.i("Connecting to $ip:$port")
        _connectionState.value = ConnectionState.Connecting(ip, port)
        CrashReporter.setKey("connect_state", "connecting")
        CrashReporter.setKey("connect_ip", "$ip:$port")

        val request = Request.Builder()
            .url("ws://$ip:$port/ws")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.i("WebSocket opened to $ip:$port — starting pairing")
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
                        if (!accept.accepted) {
                            val reason = accept.rejectionReason ?: "Pairing rejected"
                            Timber.w("Pairing rejected by desktop: $reason")
                            _connectionState.value = ConnectionState.Error(reason)
                            webSocket.close(1000, "Pairing rejected")
                            return
                        }
                        // Protocol version compatibility check
                        if (accept.protocolVersion < ProtocolInfo.MIN_COMPATIBLE_VERSION) {
                            Timber.w("Desktop protocol v${accept.protocolVersion} too old (need v${ProtocolInfo.MIN_COMPATIBLE_VERSION}+)")
                            _connectionState.value = ConnectionState.Error(
                                "Update Spyglass Connect on your PC. Desktop protocol v${accept.protocolVersion} is not compatible (v${ProtocolInfo.MIN_COMPATIBLE_VERSION}+ required)."
                            )
                            webSocket.close(1000, "Incompatible protocol version")
                            return
                        }
                        if (ProtocolInfo.PROTOCOL_VERSION < accept.minCompatibleVersion) {
                            Timber.w("App protocol v${ProtocolInfo.PROTOCOL_VERSION} too old (desktop requires v${accept.minCompatibleVersion}+)")
                            _connectionState.value = ConnectionState.Error(
                                "Update the Spyglass app to the latest version. Desktop requires protocol v${accept.minCompatibleVersion}+."
                            )
                            webSocket.close(1000, "Incompatible protocol version")
                            return
                        }
                        Timber.i("Pairing accepted by '${accept.deviceName}' (protocol v${accept.protocolVersion})")
                        // Derive shared encryption key from desktop's ECDH public key
                        if (accept.pubkey != null) {
                            encryption.deriveSharedKey(accept.pubkey)
                            Timber.i("ECDH encryption established")
                        } else {
                            Timber.w("Desktop did not provide pubkey — connection is UNENCRYPTED")
                        }
                        // Negotiate capabilities
                        val desktopCaps = accept.capabilities.toSet()
                        _negotiatedCapabilities.value = if (desktopCaps.isEmpty()) {
                            // Legacy v2 desktop — assume all supported
                            Timber.i("Legacy desktop (no capabilities list) — assuming all supported")
                            Capability.ALL
                        } else {
                            desktopCaps.intersect(Capability.ALL)
                        }
                        val caps = _negotiatedCapabilities.value
                        Timber.i("Capabilities (${caps.size}): ${caps.joinToString()}")
                        _connectionState.value = ConnectionState.Connected(accept.deviceName)
                        CrashReporter.setKey("connect_state", "connected")
                        CrashReporter.setKey("connect_device", accept.deviceName)
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
                Timber.i("WebSocket closing: code=$code reason=$reason userDisconnect=$userDisconnect")
                webSocket.close(1000, null)
                if (!userDisconnect) {
                    _connectionState.value = ConnectionState.Error("Connection closed: $reason")
                } else {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val isConnRefused = t is java.net.ConnectException
                if (isConnRefused) {
                    Timber.i("Connection refused by $ip:$port (desktop probably offline)")
                } else {
                    Timber.w(t, "WebSocket failure")
                    CrashReporter.recordException(t, "WebSocket failure")
                }
                _connectionState.value = ConnectionState.Error(t.message ?: "Connection failed")
            }
        })
    }

    /** Send a pairing request with our ECDH public key and protocol version. */
    fun sendPairRequest(deviceName: String, desktopPublicKey: String) {
        val message = SpyglassMessage(
            type = MessageType.PAIR_REQUEST,
            requestId = UUID.randomUUID().toString(),
            payload = json.encodeToJsonElement(
                PairRequestPayload(
                    deviceName = deviceName,
                    pubkey = encryption.getPublicKeyBase64(),
                    protocolVersion = ProtocolInfo.PROTOCOL_VERSION,
                    minCompatibleVersion = ProtocolInfo.MIN_COMPATIBLE_VERSION,
                    appVersion = BuildConfig.VERSION_NAME,
                    platform = "android",
                    capabilities = Capability.ALL.toList(),
                ),
            ),
        )

        // Send unencrypted (pairing handshake completes before encryption begins)
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
        if (type != MessageType.DEVICE_LOG) { // Don't log the log messages themselves
            Timber.i("→ Sending $type")
        }
        send(SpyglassMessage(type = type, requestId = requestId, payload = payload))
        return requestId
    }

    /** Disconnect from the desktop (user-initiated). */
    fun disconnect() {
        Timber.i("Disconnecting (user-initiated)")
        userDisconnect = true
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        _negotiatedCapabilities.value = emptySet()
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
