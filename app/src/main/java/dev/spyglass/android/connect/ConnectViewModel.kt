package dev.spyglass.android.connect

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spyglass.android.connect.client.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import timber.log.Timber

/**
 * ViewModel managing the full Connect lifecycle:
 * connection, pairing, world selection, data requests, and reconnection.
 */
class ConnectViewModel(application: Application) : AndroidViewModel(application) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val client = SpyglassClient()
    private val reconnectManager = ReconnectManager()
    private var mdnsDiscovery: MdnsDiscovery? = null

    // ── Observable state ─────────────────────────────────────────────────────

    val connectionState: StateFlow<ConnectionState> = client.connectionState

    private val _worlds = MutableStateFlow<List<WorldInfo>>(emptyList())
    val worlds: StateFlow<List<WorldInfo>> = _worlds

    private val _playerData = MutableStateFlow<PlayerData?>(null)
    val playerData: StateFlow<PlayerData?> = _playerData

    private val _searchResults = MutableStateFlow<SearchResultsPayload?>(null)
    val searchResults: StateFlow<SearchResultsPayload?> = _searchResults

    private val _structures = MutableStateFlow<List<StructureLocation>>(emptyList())
    val structures: StateFlow<List<StructureLocation>> = _structures

    private val _mapTiles = MutableStateFlow<MapRenderPayload?>(null)
    val mapTiles: StateFlow<MapRenderPayload?> = _mapTiles

    private val _selectedWorld = MutableStateFlow<String?>(null)
    val selectedWorld: StateFlow<String?> = _selectedWorld

    init {
        // Listen for incoming messages
        viewModelScope.launch {
            client.messages.collect { message ->
                handleMessage(message)
            }
        }

        // Monitor connection state — start/stop foreground service
        viewModelScope.launch {
            client.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        reconnectManager.reset()
                        ConnectService.start(getApplication(), state.deviceName)
                    }
                    is ConnectionState.Error, ConnectionState.Disconnected -> {
                        ConnectService.stop(getApplication())
                    }
                    else -> {}
                }
            }
        }
    }

    /** Connect via QR pairing data. */
    fun connectFromQr(pairingData: QrPairingData) {
        viewModelScope.launch(Dispatchers.IO) {
            client.connect(pairingData.ip, pairingData.port)

            // Wait for WebSocket to open
            client.connectionState.first { it is ConnectionState.Pairing || it is ConnectionState.Error }

            if (client.connectionState.value is ConnectionState.Pairing) {
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                client.sendPairRequest(deviceName, pairingData.pubkey)

                // Save pairing info for reconnection
                PairingStore.save(
                    getApplication(),
                    PairingStore.PairedDevice(
                        ip = pairingData.ip,
                        port = pairingData.port,
                        deviceName = "Spyglass Connect",
                        publicKey = pairingData.pubkey,
                    ),
                )
            }
        }
    }

    /** Try reconnecting to a previously paired device. */
    fun tryReconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            val device = PairingStore.load(getApplication()) ?: return@launch

            // Try stored IP first
            client.connect(device.ip, device.port)

            // If that fails, try mDNS discovery
            client.connectionState
                .filter { it is ConnectionState.Error }
                .first()

            // Start mDNS discovery as fallback
            mdnsDiscovery = MdnsDiscovery(getApplication())
            mdnsDiscovery?.startDiscovery { ip, port ->
                viewModelScope.launch(Dispatchers.IO) {
                    client.connect(ip, port)
                }
            }
        }
    }

    /** Select a world on the desktop. */
    fun selectWorld(folderName: String) {
        _selectedWorld.value = folderName
        val payload = json.encodeToJsonElement(SelectWorldPayload(folderName))
        client.sendRequest(MessageType.SELECT_WORLD, payload)
    }

    /** Request player data for the selected world. */
    fun requestPlayerData() {
        client.sendRequest(MessageType.REQUEST_PLAYER)
    }

    /** Request chest contents scan. */
    fun requestChests() {
        client.sendRequest(MessageType.REQUEST_CHESTS)
    }

    /** Request structure locations. */
    fun requestStructures() {
        client.sendRequest(MessageType.REQUEST_STRUCTURES)
    }

    /** Request map tiles around a position. */
    fun requestMap(centerX: Int = 0, centerZ: Int = 0, radius: Int = 8, dimension: String = "overworld") {
        val payload = json.encodeToJsonElement(
            RequestMapPayload(centerX, centerZ, radius, dimension),
        )
        client.sendRequest(MessageType.REQUEST_MAP, payload)
    }

    /** Search for items in chests. */
    fun searchItems(query: String) {
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }
        val payload = json.encodeToJsonElement(SearchItemsPayload(query))
        client.sendRequest(MessageType.SEARCH_ITEMS, payload)
    }

    /** Disconnect and clean up. */
    fun disconnect() {
        ConnectService.stop(getApplication())
        mdnsDiscovery?.stopDiscovery()
        client.disconnect()
        _worlds.value = emptyList()
        _playerData.value = null
        _searchResults.value = null
        _structures.value = emptyList()
        _mapTiles.value = null
        _selectedWorld.value = null
    }

    /** Unpair (disconnect + clear stored device). */
    fun unpair() {
        disconnect()
        viewModelScope.launch {
            PairingStore.clear(getApplication())
        }
    }

    /** Handle an incoming message from the desktop. */
    private fun handleMessage(message: SpyglassMessage) {
        try {
            when (message.type) {
                MessageType.WORLD_LIST -> {
                    val payload = json.decodeFromJsonElement(WorldListPayload.serializer(), message.payload)
                    _worlds.value = payload.worlds
                }
                MessageType.PLAYER_DATA -> {
                    val payload = json.decodeFromJsonElement(PlayerData.serializer(), message.payload)
                    _playerData.value = payload
                }
                MessageType.SEARCH_RESULTS -> {
                    val payload = json.decodeFromJsonElement(SearchResultsPayload.serializer(), message.payload)
                    _searchResults.value = payload
                }
                MessageType.STRUCTURE_LOCATIONS -> {
                    val payload = json.decodeFromJsonElement(StructureLocationsPayload.serializer(), message.payload)
                    _structures.value = payload.structures
                }
                MessageType.MAP_RENDER -> {
                    val payload = json.decodeFromJsonElement(MapRenderPayload.serializer(), message.payload)
                    _mapTiles.value = payload
                }
                MessageType.WORLD_CHANGED -> {
                    // Re-request data for the selected world
                    requestPlayerData()
                }
                MessageType.ERROR -> {
                    val payload = json.decodeFromJsonElement(ErrorPayload.serializer(), message.payload)
                    Timber.w("Server error: ${payload.code} — ${payload.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to handle message: ${message.type}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        mdnsDiscovery?.stopDiscovery()
        client.disconnect()
    }
}
