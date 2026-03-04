package dev.spyglass.android.connect

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spyglass.android.connect.client.*
import dev.spyglass.android.connect.gear.GearAnalysis
import dev.spyglass.android.connect.gear.GearAnalyzer
import dev.spyglass.android.core.CrashReporter
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private var reconnectJob: Job? = null

    /** Whether we've been connected at least once (to know if reconnect makes sense). */
    private var wasConnected = false

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

    private val _playerSkin = MutableStateFlow<Bitmap?>(null)
    val playerSkin: StateFlow<Bitmap?> = _playerSkin

    private val _playerBodySkin = MutableStateFlow<Bitmap?>(null)
    val playerBodySkin: StateFlow<Bitmap?> = _playerBodySkin

    private val _playerName = MutableStateFlow<String?>(null)
    val playerName: StateFlow<String?> = _playerName

    private val _gearAnalysis = MutableStateFlow<GearAnalysis?>(null)
    val gearAnalysis: StateFlow<GearAnalysis?> = _gearAnalysis

    private val _playerStats = MutableStateFlow<PlayerStatsPayload?>(null)
    val playerStats: StateFlow<PlayerStatsPayload?> = _playerStats

    private val _playerAdvancements = MutableStateFlow<PlayerAdvancementsPayload?>(null)
    val playerAdvancements: StateFlow<PlayerAdvancementsPayload?> = _playerAdvancements

    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated

    private val repo by lazy { GameDataRepository.get(getApplication()) }

    init {
        // Load cached data on startup
        viewModelScope.launch(Dispatchers.IO) {
            val meta = ConnectCache.loadMeta(getApplication())
            if (meta != null) {
                _worlds.value = meta.worlds
                _selectedWorld.value = meta.selectedWorld
                val world = meta.selectedWorld
                if (world != null) {
                    loadCachedWorldData(world)
                }
            }
        }

        // Listen for incoming messages
        viewModelScope.launch {
            client.messages.collect { message ->
                handleMessage(message)
            }
        }

        // Compute gear analysis immediately when player data arrives
        viewModelScope.launch {
            _playerData.collectLatest { data ->
                if (data != null) {
                    try {
                        _gearAnalysis.value = GearAnalyzer.analyze(data, repo)
                    } catch (e: Exception) {
                        Timber.w(e, "Gear analysis failed")
                        _gearAnalysis.value = null
                    }
                } else {
                    _gearAnalysis.value = null
                }
            }
        }

        // Fetch player skin + name when UUID is available (separate coroutine — doesn't block gear analysis)
        viewModelScope.launch {
            _playerData.collectLatest { data ->
                val uuid = data?.playerUuid
                if (uuid != null) {
                    _playerName.value = data.playerName
                    _playerSkin.value = SkinManager.fetchSkin(uuid)
                    // Fall back to Mojang API if desktop didn't provide name
                    if (_playerName.value == null) {
                        _playerName.value = SkinManager.fetchPlayerName(uuid)
                    }
                    // Fetch body render using player name (Starlight SkinAPI)
                    val name = _playerName.value
                    if (name != null) {
                        _playerBodySkin.value = SkinManager.fetchBodyRender(name)
                    }
                    // Cache skins after fetching
                    val world = _selectedWorld.value
                    if (world != null) {
                        _playerSkin.value?.let { ConnectCache.saveSkinAvatar(getApplication(), world, it) }
                        _playerBodySkin.value?.let { ConnectCache.saveSkinBody(getApplication(), world, it) }
                    }
                } else {
                    _playerSkin.value = null
                    _playerBodySkin.value = null
                    _playerName.value = data?.playerName
                }
            }
        }

        // Monitor connection state — start/stop foreground service + auto-reconnect
        viewModelScope.launch {
            client.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        wasConnected = true
                        reconnectManager.reset()
                        reconnectJob?.cancel()
                        ConnectService.start(getApplication(), state.deviceName)
                    }
                    is ConnectionState.Error -> {
                        // Auto-reconnect on unexpected disconnect
                        if (wasConnected && !client.wasUserDisconnect) {
                            startAutoReconnect()
                        } else {
                            ConnectService.stop(getApplication())
                        }
                    }
                    is ConnectionState.Disconnected -> {
                        if (wasConnected && !client.wasUserDisconnect) {
                            startAutoReconnect()
                        } else {
                            ConnectService.stop(getApplication())
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /** Load cached data for a world into StateFlows. */
    private suspend fun loadCachedWorldData(worldFolder: String) {
        val ctx = getApplication<Application>()
        ConnectCache.loadPlayerData(ctx, worldFolder)?.let { _playerData.value = it }
        ConnectCache.loadStructures(ctx, worldFolder)?.let { _structures.value = it }
        ConnectCache.loadMapData(ctx, worldFolder)?.let { _mapTiles.value = it }
        ConnectCache.loadStats(ctx, worldFolder)?.let { _playerStats.value = it }
        ConnectCache.loadAdvancements(ctx, worldFolder)?.let { _playerAdvancements.value = it }
        ConnectCache.loadSkinAvatar(ctx, worldFolder)?.let { _playerSkin.value = it }
        ConnectCache.loadSkinBody(ctx, worldFolder)?.let { _playerBodySkin.value = it }
        ConnectCache.loadLastUpdated(ctx, worldFolder)?.let { _lastUpdated.value = it }
    }

    /** Connect via QR pairing data. */
    fun connectFromQr(pairingData: QrPairingData) {
        reconnectJob?.cancel()
        CrashReporter.log("QR pair started")
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

    /** Auto-reconnect with exponential backoff using stored pairing info. */
    private fun startAutoReconnect() {
        // Don't start if already reconnecting
        if (reconnectJob?.isActive == true) return

        CrashReporter.log("Auto-reconnect started")
        reconnectJob = viewModelScope.launch(Dispatchers.IO) {
            val device = PairingStore.load(getApplication()) ?: run {
                Timber.d("No paired device stored, can't auto-reconnect")
                ConnectService.stop(getApplication())
                return@launch
            }

            reconnectManager.reset()
            while (reconnectManager.waitForNextRetry()) {
                val attempt = reconnectManager.currentAttempt
                Timber.d("Auto-reconnect attempt $attempt to ${device.ip}:${device.port}")
                client.setReconnecting(attempt)

                client.connect(device.ip, device.port)

                // Wait for result
                val result = client.connectionState.first {
                    it is ConnectionState.Pairing || it is ConnectionState.Connected || it is ConnectionState.Error
                }

                if (result is ConnectionState.Pairing) {
                    // Re-pair with stored key
                    val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                    client.sendPairRequest(deviceName, device.publicKey)

                    // Wait for pairing result
                    val paired = client.connectionState.first {
                        it is ConnectionState.Connected || it is ConnectionState.Error
                    }
                    if (paired is ConnectionState.Connected) {
                        Timber.d("Auto-reconnect succeeded on attempt $attempt")
                        CrashReporter.log("Auto-reconnect succeeded attempt $attempt")
                        return@launch
                    }
                } else if (result is ConnectionState.Connected) {
                    Timber.d("Auto-reconnect succeeded on attempt $attempt")
                    CrashReporter.log("Auto-reconnect succeeded attempt $attempt")
                    return@launch
                }

                Timber.d("Auto-reconnect attempt $attempt failed")
            }

            // Exhausted all attempts
            Timber.d("Auto-reconnect exhausted after ${reconnectManager.currentAttempt} attempts")
            CrashReporter.log("Auto-reconnect exhausted")
            client.setError("Lost connection to PC")
            ConnectService.stop(getApplication())
        }
    }

    /** Try reconnecting to a previously paired device (user-initiated). */
    fun tryReconnect() {
        wasConnected = false // Reset so auto-reconnect doesn't fire on first failure
        reconnectJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            val device = PairingStore.load(getApplication()) ?: return@launch

            client.connect(device.ip, device.port)

            // Wait for result
            val result = client.connectionState.first {
                it is ConnectionState.Pairing || it is ConnectionState.Error
            }

            if (result is ConnectionState.Pairing) {
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                client.sendPairRequest(deviceName, device.publicKey)
            } else {
                // Try mDNS discovery as fallback
                mdnsDiscovery = MdnsDiscovery(getApplication())
                mdnsDiscovery?.startDiscovery { ip, port ->
                    viewModelScope.launch(Dispatchers.IO) {
                        client.connect(ip, port)
                    }
                }
            }
        }
    }

    /** Select a world on the desktop. */
    fun selectWorld(folderName: String) {
        _selectedWorld.value = folderName
        // Save meta with new selection
        viewModelScope.launch(Dispatchers.IO) {
            ConnectCache.saveMeta(
                getApplication(),
                ConnectCache.CacheMeta(selectedWorld = folderName, worlds = _worlds.value),
            )
            // Load cached data for newly selected world (shows instantly while fresh data loads)
            loadCachedWorldData(folderName)
        }
        if (connectionState.value.isConnected) {
            val payload = json.encodeToJsonElement(SelectWorldPayload(folderName))
            client.sendRequest(MessageType.SELECT_WORLD, payload)
        }
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

    /** Request player statistics. */
    fun requestStats() {
        client.sendRequest(MessageType.REQUEST_STATS)
    }

    /** Request player advancements. */
    fun requestAdvancements() {
        client.sendRequest(MessageType.REQUEST_ADVANCEMENTS)
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

    /** Resolve whether an inventory item ID belongs to Blocks (tab 0) or Items (tab 1). */
    suspend fun resolveBrowseTab(id: String): Int {
        return if (repo.blockById(id) != null) 0 else 1
    }

    /** Disconnect and clean up. */
    fun disconnect() {
        CrashReporter.log("User disconnect")
        wasConnected = false
        reconnectJob?.cancel()
        ConnectService.stop(getApplication())
        mdnsDiscovery?.stopDiscovery()
        client.disconnect()
        // Only clear transient data — keep cached data in StateFlows for offline viewing
        SkinManager.clear()
        _searchResults.value = null
    }

    /** Unpair (disconnect + clear stored device + clear cache). */
    fun unpair() {
        CrashReporter.log("User unpair")
        wasConnected = false
        reconnectJob?.cancel()
        ConnectService.stop(getApplication())
        mdnsDiscovery?.stopDiscovery()
        client.disconnect()
        // Clear all StateFlows
        _worlds.value = emptyList()
        _playerData.value = null
        _playerSkin.value = null
        _playerBodySkin.value = null
        _playerName.value = null
        _gearAnalysis.value = null
        _playerStats.value = null
        _playerAdvancements.value = null
        SkinManager.clear()
        _searchResults.value = null
        _structures.value = emptyList()
        _mapTiles.value = null
        _selectedWorld.value = null
        _lastUpdated.value = null
        viewModelScope.launch {
            PairingStore.clear(getApplication())
            ConnectCache.deleteAll(getApplication())
        }
    }

    /** Handle an incoming message from the desktop. */
    private fun handleMessage(message: SpyglassMessage) {
        Timber.d("Received ${message.type}")
        try {
            when (message.type) {
                MessageType.WORLD_LIST -> {
                    val payload = json.decodeFromJsonElement(WorldListPayload.serializer(), message.payload)
                    Timber.d("World list: ${payload.worlds.size} worlds")
                    _worlds.value = payload.worlds
                    // Cache meta
                    viewModelScope.launch(Dispatchers.IO) {
                        ConnectCache.saveMeta(
                            getApplication(),
                            ConnectCache.CacheMeta(selectedWorld = _selectedWorld.value, worlds = payload.worlds),
                        )
                    }
                }
                MessageType.PLAYER_DATA -> {
                    val payload = json.decodeFromJsonElement(PlayerData.serializer(), message.payload)
                    val changed = _playerData.value != payload
                    _playerData.value = payload
                    // Cache only if data changed
                    if (changed) {
                        val world = _selectedWorld.value
                        if (world != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                ConnectCache.savePlayerData(getApplication(), world, payload)
                                _lastUpdated.value = System.currentTimeMillis()
                            }
                        }
                    }
                }
                MessageType.SEARCH_RESULTS -> {
                    val payload = json.decodeFromJsonElement(SearchResultsPayload.serializer(), message.payload)
                    _searchResults.value = payload
                }
                MessageType.STRUCTURE_LOCATIONS -> {
                    val payload = json.decodeFromJsonElement(StructureLocationsPayload.serializer(), message.payload)
                    val changed = _structures.value != payload.structures
                    _structures.value = payload.structures
                    if (changed) {
                        val world = _selectedWorld.value
                        if (world != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                ConnectCache.saveStructures(getApplication(), world, payload.structures)
                            }
                        }
                    }
                }
                MessageType.MAP_RENDER -> {
                    val payload = json.decodeFromJsonElement(MapRenderPayload.serializer(), message.payload)
                    val changed = _mapTiles.value != payload
                    _mapTiles.value = payload
                    if (changed) {
                        val world = _selectedWorld.value
                        if (world != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                ConnectCache.saveMapData(getApplication(), world, payload)
                            }
                        }
                    }
                }
                MessageType.PLAYER_STATS -> {
                    val payload = json.decodeFromJsonElement(PlayerStatsPayload.serializer(), message.payload)
                    val changed = _playerStats.value != payload
                    _playerStats.value = payload
                    if (changed) {
                        val world = _selectedWorld.value
                        if (world != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                ConnectCache.saveStats(getApplication(), world, payload)
                                _lastUpdated.value = System.currentTimeMillis()
                            }
                        }
                    }
                }
                MessageType.PLAYER_ADVANCEMENTS -> {
                    val payload = json.decodeFromJsonElement(PlayerAdvancementsPayload.serializer(), message.payload)
                    val changed = _playerAdvancements.value != payload
                    _playerAdvancements.value = payload
                    if (changed) {
                        val world = _selectedWorld.value
                        if (world != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                ConnectCache.saveAdvancements(getApplication(), world, payload)
                                _lastUpdated.value = System.currentTimeMillis()
                            }
                        }
                    }
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
            CrashReporter.recordException(e, "Handle message failed: ${message.type}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        reconnectJob?.cancel()
        mdnsDiscovery?.stopDiscovery()
        client.disconnect()
    }
}
