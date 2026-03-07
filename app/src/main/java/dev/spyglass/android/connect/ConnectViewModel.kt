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
    private val logTree = ConnectLogTree(client)

    /** Whether we've been connected at least once (to know if reconnect makes sense). */
    private var wasConnected = false

    /** Whether we've already re-sent select_world for this session (prevents loop). */
    private var worldReselected = false

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

    private val _playerList = MutableStateFlow<List<PlayerSummary>>(emptyList())
    val playerList: StateFlow<List<PlayerSummary>> = _playerList

    private val _selectedPlayerUuid = MutableStateFlow<String?>(null)
    val selectedPlayerUuid: StateFlow<String?> = _selectedPlayerUuid

    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated

    private val _pets = MutableStateFlow<List<PetData>>(emptyList())
    val pets: StateFlow<List<PetData>> = _pets

    private val _chestContents = MutableStateFlow<ChestContentsPayload?>(null)
    val chestContents: StateFlow<ChestContentsPayload?> = _chestContents

    private val _comparePlayerData = MutableStateFlow<PlayerData?>(null)
    val comparePlayerData: StateFlow<PlayerData?> = _comparePlayerData

    private val repo by lazy { GameDataRepository.get(getApplication()) }

    init {
        // Plant the log tree so warnings/errors/crashes are captured and sent to desktop
        Timber.plant(logTree)

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
                        worldReselected = false
                        reconnectManager.reset()
                        reconnectJob?.cancel()
                        ConnectService.start(getApplication(), state.deviceName)
                        // Send any crash logs saved from previous sessions
                        viewModelScope.launch(Dispatchers.IO) { sendPendingCrashLogs() }
                        // Flush any buffered log entries to desktop
                        logTree.flush()
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
        ConnectCache.loadPets(ctx, worldFolder)?.let { _pets.value = it }
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
        _selectedPlayerUuid.value = null // Reset player selection on world change
        _playerList.value = emptyList()
        if (connectionState.value.isConnected) {
            val payload = json.encodeToJsonElement(SelectWorldPayload(folderName))
            client.sendRequest(MessageType.SELECT_WORLD, payload)
            requestPlayerList()
        }
    }

    /** Request player data for the selected world (optionally for a specific player). */
    fun requestPlayerData() {
        val uuid = _selectedPlayerUuid.value
        if (uuid != null) {
            val payload = json.encodeToJsonElement(RequestPlayerPayload(uuid))
            client.sendRequest(MessageType.REQUEST_PLAYER, payload)
        } else {
            client.sendRequest(MessageType.REQUEST_PLAYER)
        }
    }

    /** Request the list of all players in the selected world. */
    fun requestPlayerList() {
        client.sendRequest(MessageType.REQUEST_PLAYER_LIST)
    }

    /** Select a specific player by UUID and refresh their data. */
    fun selectPlayer(uuid: String?) {
        _selectedPlayerUuid.value = uuid
        if (connectionState.value.isConnected) {
            requestPlayerData()
        }
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

    /** Request pets list. */
    fun requestPets() {
        client.sendRequest(MessageType.REQUEST_PETS)
    }

    /** Request a second player's data for comparison. */
    private var pendingCompareUuid: String? = null

    fun requestComparePlayer(uuid: String) {
        pendingCompareUuid = uuid
        val payload = json.encodeToJsonElement(RequestPlayerPayload(uuid))
        client.sendRequest(MessageType.REQUEST_PLAYER, payload)
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

    /** Clear cached data without unpairing. */
    fun clearCachedData() {
        _worlds.value = emptyList()
        _playerData.value = null
        _playerSkin.value = null
        _playerBodySkin.value = null
        _playerName.value = null
        _gearAnalysis.value = null
        _playerStats.value = null
        _playerAdvancements.value = null
        _playerList.value = emptyList()
        _selectedPlayerUuid.value = null
        _searchResults.value = null
        _chestContents.value = null
        _structures.value = emptyList()
        _mapTiles.value = null
        _selectedWorld.value = null
        _lastUpdated.value = null
        _pets.value = emptyList()
        _comparePlayerData.value = null
        SkinManager.clear()
        viewModelScope.launch(Dispatchers.IO) {
            ConnectCache.deleteAll(getApplication())
        }
    }

    /** Send any crash logs saved to disk from previous sessions. */
    private fun sendPendingCrashLogs() {
        val entries = CrashLogStore.loadAndClear(getApplication())
        if (entries.isEmpty()) return
        Timber.i("Sending ${entries.size} pending crash log(s) to desktop")
        try {
            val payload = json.encodeToJsonElement(DeviceLogPayload(entries))
            client.sendRequest(MessageType.DEVICE_LOG, payload)
        } catch (e: Exception) {
            Timber.w(e, "Failed to send pending crash logs")
        }
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
        _playerList.value = emptyList()
        _selectedPlayerUuid.value = null
        SkinManager.clear()
        _searchResults.value = null
        _chestContents.value = null
        _structures.value = emptyList()
        _mapTiles.value = null
        _selectedWorld.value = null
        _lastUpdated.value = null
        _pets.value = emptyList()
        _comparePlayerData.value = null
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
                    // Re-select world after reconnect (desktop forgets selection on new session)
                    val currentWorld = _selectedWorld.value
                    if (currentWorld != null && connectionState.value.isConnected && !worldReselected) {
                        worldReselected = true
                        Timber.d("Re-selecting world after reconnect: $currentWorld")
                        val selectPayload = json.encodeToJsonElement(SelectWorldPayload(currentWorld))
                        client.sendRequest(MessageType.SELECT_WORLD, selectPayload)
                        requestPlayerList()
                    }
                }
                MessageType.PLAYER_LIST -> {
                    val payload = json.decodeFromJsonElement(PlayerListPayload.serializer(), message.payload)
                    Timber.d("Player list: ${payload.players.size} players")
                    _playerList.value = payload.players
                }
                MessageType.PLAYER_DATA -> {
                    val payload = json.decodeFromJsonElement(PlayerData.serializer(), message.payload)
                    // Route to compare if this is the compare player's data
                    val compareUuid = pendingCompareUuid
                    if (compareUuid != null && payload.playerUuid.equals(compareUuid, ignoreCase = true)) {
                        _comparePlayerData.value = payload
                        pendingCompareUuid = null
                    } else {
                        val changed = _playerData.value != payload
                        _playerData.value = payload
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
                }
                MessageType.CHEST_CONTENTS -> {
                    val payload = json.decodeFromJsonElement(ChestContentsPayload.serializer(), message.payload)
                    _chestContents.value = payload
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
                MessageType.PETS_LIST -> {
                    val payload = json.decodeFromJsonElement(PetsListPayload.serializer(), message.payload)
                    val changed = _pets.value != payload.pets
                    _pets.value = payload.pets
                    if (changed) {
                        val world = _selectedWorld.value
                        if (world != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                ConnectCache.savePets(getApplication(), world, payload.pets)
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
        Timber.uproot(logTree)
        reconnectJob?.cancel()
        mdnsDiscovery?.stopDiscovery()
        client.disconnect()
    }
}
