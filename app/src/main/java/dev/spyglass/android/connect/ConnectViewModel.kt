package dev.spyglass.android.connect

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spyglass.android.connect.client.*
import dev.spyglass.android.connect.gear.GearAnalysis
import dev.spyglass.android.connect.gear.GearAnalyzer
import dev.spyglass.android.connect.waypoints.ConnectWaypoint
import dev.spyglass.android.core.CrashReporter
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * ViewModel managing the full Spyglass Connect lifecycle.
 *
 * Responsibilities:
 * - Connection (QR pairing, auto-reconnect with backoff, mDNS fallback)
 * - World selection + player selection (IGN-based auto-select)
 * - Data requests (player, chests, structures, map, stats, advancements, pets)
 * - Live refresh (10s polling on the active screen)
 * - Offline cache (load on startup, save on every data change)
 * - Waypoint management (auto-generated from player data + custom waypoints)
 * - Player comparison (request a second player's data side-by-side)
 * - Crash/log forwarding to desktop via DEVICE_LOG messages
 */
class ConnectViewModel(application: Application) : AndroidViewModel(application) {

    // ── Core infrastructure ──────────────────────────────────────────────

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val client = SpyglassClient(application)
    private val reconnectManager = ReconnectManager()
    private var mdnsDiscovery: MdnsDiscovery? = null
    private var reconnectJob: Job? = null
    private var liveRefreshJob: Job? = null
    private val logTree = ConnectLogTree(client)

    /** True after first successful connection — enables auto-reconnect on disconnect. */
    private var wasConnected = false

    /** Prevents re-sending select_world in a loop when WORLD_LIST arrives. */
    private var worldReselected = false

    /** UUID of the player being fetched for comparison (routed separately in PLAYER_DATA). */
    private var pendingCompareUuid: String? = null

    /** Which Connect sub-screen is visible — drives live refresh data selection. */
    private val _activeScreen = MutableStateFlow<String?>(null)
    fun setActiveScreen(screen: String?) { _activeScreen.value = screen }

    /** Transient loading status text shown below ChestDiamondLoader on Connect screens. */
    private val _loadingStatus = MutableStateFlow<String?>(null)
    val loadingStatus: StateFlow<String?> = _loadingStatus

    private val repo by lazy { GameDataRepository.get(getApplication()) }

    // ── Observable state ─────────────────────────────────────────────────
    //
    // Each piece of data has a private MutableStateFlow (written internally)
    // and a public StateFlow (read by Compose UI). Grouped by domain.

    // Connection
    val connectionState: StateFlow<ConnectionState> = client.connectionState
    val desktopCapabilities: StateFlow<Set<String>> = client.negotiatedCapabilities

    // World selection
    private val _worlds = MutableStateFlow<List<WorldInfo>>(emptyList())
    val worlds: StateFlow<List<WorldInfo>> = _worlds

    private val _selectedWorld = MutableStateFlow<String?>(null)
    val selectedWorld: StateFlow<String?> = _selectedWorld

    // Player selection
    private val _playerList = MutableStateFlow<List<PlayerSummary>>(emptyList())
    val playerList: StateFlow<List<PlayerSummary>> = _playerList

    private val _selectedPlayerUuid = MutableStateFlow<String?>(null)
    val selectedPlayerUuid: StateFlow<String?> = _selectedPlayerUuid

    // Player data (character, inventory, ender chest)
    private val _playerData = MutableStateFlow<PlayerData?>(null)
    val playerData: StateFlow<PlayerData?> = _playerData

    private val _comparePlayerData = MutableStateFlow<PlayerData?>(null)
    val comparePlayerData: StateFlow<PlayerData?> = _comparePlayerData

    // Player identity (skin, name — fetched from Mojang/Starlight APIs)
    private val _playerSkin = MutableStateFlow<Bitmap?>(null)
    val playerSkin: StateFlow<Bitmap?> = _playerSkin

    private val _playerBodySkin = MutableStateFlow<Bitmap?>(null)
    val playerBodySkin: StateFlow<Bitmap?> = _playerBodySkin

    private val _playerName = MutableStateFlow<String?>(null)
    val playerName: StateFlow<String?> = _playerName

    // Gear analysis (computed from player data + game data repo)
    private val _gearAnalysis = MutableStateFlow<GearAnalysis?>(null)
    val gearAnalysis: StateFlow<GearAnalysis?> = _gearAnalysis

    // World data (chests, structures, map, search, stats, advancements, pets)
    private val _chestContents = MutableStateFlow<ChestContentsPayload?>(null)
    val chestContents: StateFlow<ChestContentsPayload?> = _chestContents

    private val _searchResults = MutableStateFlow<SearchResultsPayload?>(null)
    val searchResults: StateFlow<SearchResultsPayload?> = _searchResults

    private val _structures = MutableStateFlow<List<StructureLocation>>(emptyList())
    val structures: StateFlow<List<StructureLocation>> = _structures

    /** Accumulated map tiles keyed by dimension → packed(chunkX,chunkZ) → tile. Survives navigation. */
    private val accumulatedMapTiles = ConcurrentHashMap<String, ConcurrentHashMap<Long, MapTile>>()

    /** Emits each incoming tile batch for the MapTileCache to accumulate. */
    private val _mapTileBatch = MutableSharedFlow<MapRenderPayload>(extraBufferCapacity = 1)
    val mapTileBatch: SharedFlow<MapRenderPayload> = _mapTileBatch

    private var mapSaveJob: Job? = null

    /** Build a full MapRenderPayload from all accumulated tiles (for seeding MapState on navigation). */
    fun getAllAccumulatedTiles(): MapRenderPayload? {
        val allTiles = accumulatedMapTiles.values.flatMap { it.values }
        if (allTiles.isEmpty()) return null
        val player = _playerData.value
        return MapRenderPayload(
            worldName = _selectedWorld.value ?: "",
            tiles = allTiles,
            playerX = player?.posX ?: 0.0,
            playerZ = player?.posZ ?: 0.0,
        )
    }

    private val _playerStats = MutableStateFlow<PlayerStatsPayload?>(null)
    val playerStats: StateFlow<PlayerStatsPayload?> = _playerStats

    private val _playerAdvancements = MutableStateFlow<PlayerAdvancementsPayload?>(null)
    val playerAdvancements: StateFlow<PlayerAdvancementsPayload?> = _playerAdvancements

    private val _pets = MutableStateFlow<List<PetData>>(emptyList())
    val pets: StateFlow<List<PetData>> = _pets

    // Waypoints (auto-generated + custom)
    private val _connectWaypoints = MutableStateFlow<List<ConnectWaypoint>>(emptyList())
    val connectWaypoints: StateFlow<List<ConnectWaypoint>> = _connectWaypoints

    // Metadata
    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated

    // ── Initialization ───────────────────────────────────────────────────

    init {
        // Forward warnings/errors/crashes to desktop via DEVICE_LOG messages
        Timber.plant(logTree)
        Timber.i("ConnectViewModel initialized — loading cache and checking for paired device")

        initCacheAndAutoConnect()
        initMessageListener()
        initIgnObserver()
        initPlayerDataObservers()
        initConnectionStateMonitor()
    }

    /** Load cached world data and auto-connect to last paired device. */
    private fun initCacheAndAutoConnect() {
        viewModelScope.launch(Dispatchers.IO) {
            val meta = ConnectCache.loadMeta(getApplication())
            if (meta != null) {
                Timber.i("Cache loaded: ${meta.worlds.size} worlds, selected=${meta.selectedWorld ?: "none"}")
                _worlds.value = meta.worlds
                _selectedWorld.value = meta.selectedWorld
                meta.selectedWorld?.let { loadCachedWorldData(it) }
            } else {
                Timber.i("No cached data found — fresh start")
            }

            val device = PairingStore.load(getApplication())
            if (device != null) {
                Timber.i("Found paired device: ${device.deviceName} at ${device.ip}:${device.port} — auto-reconnecting")
                tryReconnect()
            } else {
                Timber.i("No paired device stored — waiting for QR scan")
            }
        }
    }

    /** Collect incoming desktop messages on the main dispatcher. */
    private fun initMessageListener() {
        viewModelScope.launch {
            client.messages.collect { handleMessage(it) }
        }
    }

    /**
     * Watch for IGN preference changes and re-evaluate auto-select.
     * Handles the case where PLAYER_LIST arrived before the user set their IGN
     * (e.g., first-run IGN dialog after connection is already established).
     */
    private fun initIgnObserver() {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().dataStore.data
                .map { it[PreferenceKeys.PLAYER_IGN] ?: "" }
                .distinctUntilChanged()
                .collect { ign ->
                    val players = _playerList.value
                    if (ign.isNotBlank() && players.isNotEmpty() && _selectedPlayerUuid.value == null) {
                        val match = players.find { it.name.equals(ign, ignoreCase = true) }
                        if (match != null) {
                            Timber.i("IGN changed to '$ign' — auto-selecting player ${match.uuid}")
                            selectPlayer(match.uuid)
                        }
                    }
                }
        }
    }

    /**
     * React to player data changes: compute gear analysis, auto-populate
     * waypoints, and fetch skin/name from external APIs.
     *
     * Uses a single collectLatest to avoid duplicate coroutines competing
     * over the same data. The skin fetch is launched separately so it
     * doesn't block gear analysis (network latency).
     */
    private fun initPlayerDataObservers() {
        viewModelScope.launch {
            _playerData.collectLatest { data ->
                if (data != null) {
                    // Gear analysis (fast — local computation)
                    try {
                        _gearAnalysis.value = GearAnalyzer.analyze(data, repo)
                    } catch (e: Exception) {
                        Timber.w(e, "Gear analysis failed")
                        _gearAnalysis.value = null
                    }
                    // Waypoints (fast — merges spawn/death/respawn into list)
                    autoPopulateWaypoints(data)
                } else {
                    _gearAnalysis.value = null
                }
            }
        }

        // Skin + name fetch (separate coroutine — network calls shouldn't block gear analysis)
        viewModelScope.launch {
            _playerData.collectLatest { data ->
                val uuid = data?.playerUuid
                if (uuid != null) {
                    _playerName.value = data.playerName
                    _playerSkin.value = SkinManager.fetchSkin(uuid)
                    // Fall back to Mojang API if desktop didn't provide the player name
                    if (_playerName.value == null) {
                        _playerName.value = SkinManager.fetchPlayerName(uuid)
                    }
                    // Fetch full-body render using player name (Starlight SkinAPI)
                    _playerName.value?.let { name ->
                        _playerBodySkin.value = SkinManager.fetchBodyRender(name)
                    }
                    // Cache skins for offline viewing
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
    }

    /**
     * Monitor connection state changes to manage foreground service,
     * live refresh, and auto-reconnect on unexpected disconnects.
     */
    private fun initConnectionStateMonitor() {
        viewModelScope.launch {
            client.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        Timber.i("Connected to '${state.deviceName}' — starting foreground service + live refresh")
                        wasConnected = true
                        worldReselected = false
                        reconnectManager.reset()
                        reconnectJob?.cancel()
                        ConnectService.start(getApplication(), state.deviceName)
                        viewModelScope.launch(Dispatchers.IO) { sendPendingCrashLogs() }
                        logTree.flush()
                        startLiveRefresh()
                    }
                    is ConnectionState.Error, is ConnectionState.Disconnected -> {
                        val label = if (state is ConnectionState.Error) "error" else "disconnected"
                        Timber.i("Connection $label (wasConnected=$wasConnected, userDisconnect=${client.wasUserDisconnect})")
                        liveRefreshJob?.cancel()
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

    // ── Cache ────────────────────────────────────────────────────────────

    /**
     * Restore all cached data for a world into StateFlows.
     * Shows data instantly while fresh data loads from the desktop.
     *
     * Player data is only restored if the cached player name matches the
     * configured IGN (prevents showing stale data for the wrong player).
     */
    private suspend fun loadCachedWorldData(worldFolder: String) {
        val ctx = getApplication<Application>()

        // Load waypoints FIRST — autoPopulateWaypoints (triggered by playerData) merges into this list
        ConnectCache.loadWaypoints(ctx, worldFolder)?.let { _connectWaypoints.value = it }

        // Guard: only restore cached player data if it matches the IGN
        val cachedPlayer = ConnectCache.loadPlayerData(ctx, worldFolder)
        if (cachedPlayer != null) {
            val ign = try {
                ctx.dataStore.data.first()[PreferenceKeys.PLAYER_IGN] ?: ""
            } catch (_: Exception) { "" }
            if (ign.isBlank() || cachedPlayer.playerName.equals(ign, ignoreCase = true)) {
                _playerData.value = cachedPlayer
            } else {
                Timber.d("Skipping cached player data — cached '${cachedPlayer.playerName}' doesn't match IGN '$ign'")
            }
        }

        ConnectCache.loadStructures(ctx, worldFolder)?.let { _structures.value = it }
        ConnectCache.loadMapData(ctx, worldFolder)?.let { cached ->
            // Populate accumulator from disk cache
            for (tile in cached.tiles) {
                val dimMap = accumulatedMapTiles.getOrPut(tile.dimension) { ConcurrentHashMap() }
                val key = (tile.chunkX.toLong() shl 32) or (tile.chunkZ.toLong() and 0xFFFFFFFFL)
                dimMap[key] = tile
            }
            _mapTileBatch.tryEmit(cached)
        }
        ConnectCache.loadChests(ctx, worldFolder)?.let { _chestContents.value = it }
        ConnectCache.loadStats(ctx, worldFolder)?.let { _playerStats.value = it }
        ConnectCache.loadAdvancements(ctx, worldFolder)?.let { _playerAdvancements.value = it }
        ConnectCache.loadSkinAvatar(ctx, worldFolder)?.let { _playerSkin.value = it }
        ConnectCache.loadSkinBody(ctx, worldFolder)?.let { _playerBodySkin.value = it }
        ConnectCache.loadLastUpdated(ctx, worldFolder)?.let { _lastUpdated.value = it }
        ConnectCache.loadPets(ctx, worldFolder)?.let { _pets.value = it }
    }

    // ── Connection ───────────────────────────────────────────────────────

    /** Connect via QR code pairing data and save device info for future reconnections. */
    fun connectFromQr(pairingData: QrPairingData) {
        reconnectJob?.cancel()
        Timber.i("QR pairing started → ${pairingData.ip}:${pairingData.port}")
        CrashReporter.log("QR pair started")
        viewModelScope.launch(Dispatchers.IO) {
            client.connect(pairingData.ip, pairingData.port)
            client.connectionState.first { it is ConnectionState.Pairing || it is ConnectionState.Error }

            if (client.connectionState.value is ConnectionState.Pairing) {
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                Timber.i("WebSocket open, sending pair request as '$deviceName'")
                client.sendPairRequest(deviceName, pairingData.pubkey)

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

    /**
     * Periodically refresh data for the active Connect sub-screen.
     * Uses screen-aware intervals — player screens use long intervals (push handles
     * normal updates), while expensive operations like chest scanning poll infrequently.
     * Screen changes immediately reset the timer via collectLatest.
     */
    private fun startLiveRefresh() {
        liveRefreshJob?.cancel()
        liveRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            _activeScreen.collectLatest { screen ->
                while (true) {
                    val intervalMs = when (screen) {
                        "character", "inventory", "enderchest", "waypoints" -> 30_000L
                        "chestfinder"  -> 120_000L
                        "statistics"   -> 60_000L
                        "advancements" -> 60_000L
                        "pets"         -> 120_000L
                        "map"          -> Long.MAX_VALUE // no polling — on-demand only
                        else           -> 30_000L // null, "connect", or unknown
                    }
                    kotlinx.coroutines.delay(intervalMs)
                    if (!client.isConnected) break
                    when (screen) {
                        "character", "inventory", "enderchest", "waypoints" -> requestPlayerData()
                        "chestfinder"  -> requestChests()
                        "statistics"   -> requestStats()
                        "advancements" -> requestAdvancements()
                        "pets"         -> requestPets()
                        else           -> requestPlayerData() // default: keep player data fresh
                    }
                }
            }
        }
    }

    /**
     * Auto-reconnect with exponential backoff using stored pairing info.
     * Tries the stored IP first, then falls back to mDNS discovery.
     * Gives up after ReconnectManager's max attempts + 10s mDNS window.
     */
    private fun startAutoReconnect() {
        if (reconnectJob?.isActive == true) return

        CrashReporter.log("Auto-reconnect started")
        reconnectJob = viewModelScope.launch(Dispatchers.IO) {
            val device = PairingStore.load(getApplication()) ?: run {
                Timber.i("No paired device stored — cannot auto-reconnect")
                ConnectService.stop(getApplication())
                return@launch
            }

            Timber.i("Starting auto-reconnect to ${device.ip}:${device.port}")
            reconnectManager.reset()

            // Phase 1: Retry stored IP with exponential backoff
            while (reconnectManager.waitForNextRetry()) {
                val attempt = reconnectManager.currentAttempt
                Timber.i("Auto-reconnect attempt $attempt → ${device.ip}:${device.port}")
                client.setReconnecting(attempt)
                client.connect(device.ip, device.port)

                val result = client.connectionState.first {
                    it is ConnectionState.Pairing || it is ConnectionState.Connected || it is ConnectionState.Error
                }

                if (result is ConnectionState.Pairing) {
                    val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                    client.sendPairRequest(deviceName, device.publicKey)
                    val paired = client.connectionState.first {
                        it is ConnectionState.Connected || it is ConnectionState.Error
                    }
                    if (paired is ConnectionState.Connected) {
                        Timber.i("Auto-reconnect succeeded on attempt $attempt")
                        CrashReporter.log("Auto-reconnect succeeded attempt $attempt")
                        return@launch
                    }
                } else if (result is ConnectionState.Connected) {
                    Timber.i("Auto-reconnect succeeded on attempt $attempt")
                    CrashReporter.log("Auto-reconnect succeeded attempt $attempt")
                    return@launch
                }

                Timber.i("Auto-reconnect attempt $attempt failed")
            }

            // Phase 2: mDNS discovery as last resort (desktop IP may have changed)
            Timber.i("IP retries exhausted — trying mDNS discovery as fallback")
            CrashReporter.log("Auto-reconnect trying mDNS fallback")
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            mdnsDiscovery = MdnsDiscovery(getApplication())
            var mdnsHandled = false
            mdnsDiscovery?.startDiscovery { ip, port ->
                if (mdnsHandled) return@startDiscovery
                mdnsHandled = true
                viewModelScope.launch(Dispatchers.IO) {
                    client.connect(ip, port)
                    val mdnsResult = client.connectionState.first {
                        it is ConnectionState.Pairing || it is ConnectionState.Error
                    }
                    if (mdnsResult is ConnectionState.Pairing) {
                        client.sendPairRequest(deviceName, device.publicKey)
                        PairingStore.save(
                            getApplication(),
                            device.copy(ip = ip, port = port, lastConnected = System.currentTimeMillis()),
                        )
                        CrashReporter.log("Auto-reconnect succeeded via mDNS")
                    } else {
                        client.setError("Lost connection to PC")
                        ConnectService.stop(getApplication())
                    }
                    mdnsDiscovery?.stopDiscovery()
                }
            }

            // Give mDNS 10 seconds to find the desktop before giving up
            kotlinx.coroutines.delay(10_000)
            if (!mdnsHandled) {
                mdnsDiscovery?.stopDiscovery()
                Timber.i("Auto-reconnect exhausted — mDNS found nothing after 10s")
                CrashReporter.log("Auto-reconnect exhausted")
                wasConnected = false
                client.setError("Lost connection to PC")
                ConnectService.stop(getApplication())
            }
        }
    }

    /** Try reconnecting to a previously paired device (user-initiated from Connect Hub). */
    fun tryReconnect() {
        Timber.i("User-initiated reconnect")
        wasConnected = false
        reconnectJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            val device = PairingStore.load(getApplication()) ?: run {
                Timber.i("No paired device stored — nothing to reconnect to")
                return@launch
            }
            Timber.i("Reconnecting to stored device ${device.ip}:${device.port}")
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

            client.connect(device.ip, device.port)
            val result = client.connectionState.first {
                it is ConnectionState.Pairing || it is ConnectionState.Error
            }

            if (result is ConnectionState.Pairing) {
                client.sendPairRequest(deviceName, device.publicKey)
            } else {
                // Stored IP failed — try mDNS discovery as fallback
                Timber.i("Stored IP ${device.ip} failed — trying mDNS discovery")
                mdnsDiscovery = MdnsDiscovery(getApplication())
                mdnsDiscovery?.startDiscovery { ip, port ->
                    viewModelScope.launch(Dispatchers.IO) {
                        client.connect(ip, port)
                        val mdnsResult = client.connectionState.first {
                            it is ConnectionState.Pairing || it is ConnectionState.Error
                        }
                        if (mdnsResult is ConnectionState.Pairing) {
                            client.sendPairRequest(deviceName, device.publicKey)
                            PairingStore.save(
                                getApplication(),
                                device.copy(ip = ip, port = port, lastConnected = System.currentTimeMillis()),
                            )
                        }
                        mdnsDiscovery?.stopDiscovery()
                    }
                }
            }
        }
    }

    /** Disconnect gracefully — keeps cached data for offline viewing. */
    fun disconnect() {
        Timber.i("User disconnecting — keeping cached data for offline viewing")
        CrashReporter.log("User disconnect")
        wasConnected = false
        reconnectJob?.cancel()
        ConnectService.stop(getApplication())
        mdnsDiscovery?.stopDiscovery()
        client.disconnect()
        SkinManager.clear()
        _searchResults.value = null
    }

    /** Unpair — disconnect + clear ALL stored data and pairing info. */
    fun unpair() {
        Timber.i("User unpairing — clearing all cached data and stored device")
        CrashReporter.log("User unpair")
        disconnect()
        resetAllState()
        viewModelScope.launch {
            PairingStore.clear(getApplication())
            ConnectCache.deleteAll(getApplication())
        }
    }

    // ── Data requests ────────────────────────────────────────────────────

    /** Select a world on the desktop and request its player list. */
    fun selectWorld(folderName: String) {
        Timber.i("Selecting world: $folderName")
        _selectedWorld.value = folderName
        _playerList.value = emptyList()
        _selectedPlayerUuid.value = null
        accumulatedMapTiles.clear()
        viewModelScope.launch(Dispatchers.IO) {
            ConnectCache.saveMeta(
                getApplication(),
                ConnectCache.CacheMeta(selectedWorld = folderName, worlds = _worlds.value),
            )
            loadCachedWorldData(folderName)
            withContext(Dispatchers.Main) {
                if (connectionState.value.isConnected) {
                    val payload = json.encodeToJsonElement(SelectWorldPayload(folderName))
                    client.sendRequest(MessageType.SELECT_WORLD, payload)
                    requestPlayerList()
                }
            }
        }
    }

    /**
     * Request player data from the desktop.
     * Only sends if a player UUID is selected — prevents loading the wrong
     * player's data in multi-player worlds when no selection has been made.
     */
    fun requestPlayerData() {
        val uuid = _selectedPlayerUuid.value ?: return
        if (_playerData.value == null) _loadingStatus.value = "Requesting player data\u2026"
        val payload = json.encodeToJsonElement(RequestPlayerPayload(uuid))
        client.sendRequest(MessageType.REQUEST_PLAYER, payload)
    }

    /** Request the list of all players in the selected world. */
    fun requestPlayerList() {
        if (_playerList.value.isEmpty()) _loadingStatus.value = "Requesting player list\u2026"
        client.sendRequest(MessageType.REQUEST_PLAYER_LIST)
    }

    /**
     * Select a player by UUID and immediately request their data.
     * Called from IGN auto-select, the player picker UI, or PLAYER_LIST auto-select.
     */
    fun selectPlayer(uuid: String?) {
        _selectedPlayerUuid.value = uuid
        if (connectionState.value.isConnected) {
            requestPlayerData()
        }
    }

    /** Request chest contents scan for the selected world. */
    fun requestChests() {
        if (_chestContents.value == null) _loadingStatus.value = "Scanning containers\u2026"
        client.sendRequest(MessageType.REQUEST_CHESTS)
    }

    /** Request generated structure locations for the selected world. */
    fun requestStructures() {
        client.sendRequest(MessageType.REQUEST_STRUCTURES)
    }

    /** Request map tiles around a position (defaults to origin, overworld). */
    fun requestMap(centerX: Int = 0, centerZ: Int = 0, radius: Int = 8, dimension: String = "overworld") {
        if (accumulatedMapTiles.isEmpty()) _loadingStatus.value = "Requesting map data\u2026"
        val payload = json.encodeToJsonElement(RequestMapPayload(centerX, centerZ, radius, dimension))
        client.sendRequest(MessageType.REQUEST_MAP, payload)
    }

    /** Request player statistics for the selected player. */
    fun requestStats() {
        if (_playerStats.value == null) _loadingStatus.value = "Requesting statistics\u2026"
        client.sendRequest(MessageType.REQUEST_STATS)
    }

    /** Request advancement progress for the selected player. */
    fun requestAdvancements() {
        if (_playerAdvancements.value == null) _loadingStatus.value = "Requesting advancements\u2026"
        client.sendRequest(MessageType.REQUEST_ADVANCEMENTS)
    }

    /** Request tamed pets list for the selected world. */
    fun requestPets() {
        if (_pets.value.isEmpty()) _loadingStatus.value = "Requesting pets\u2026"
        client.sendRequest(MessageType.REQUEST_PETS)
    }

    /** Request a second player's data for side-by-side comparison. */
    fun requestComparePlayer(uuid: String) {
        _loadingStatus.value = "Requesting player data\u2026"
        pendingCompareUuid = uuid
        val payload = json.encodeToJsonElement(RequestPlayerPayload(uuid))
        client.sendRequest(MessageType.REQUEST_PLAYER, payload)
    }

    /** Search for items across all chests in the selected world. */
    fun searchItems(query: String) {
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }
        val payload = json.encodeToJsonElement(SearchItemsPayload(query))
        client.sendRequest(MessageType.SEARCH_ITEMS, payload)
    }

    /** Resolve whether an item ID is a Block (tab 0) or Item (tab 1) for browse navigation. */
    suspend fun resolveBrowseTab(id: String): Int {
        return if (repo.blockById(id) != null) 0 else 1
    }

    /** Clear cached data without unpairing (keeps device info for reconnection). */
    fun clearCachedData() {
        resetAllState()
        viewModelScope.launch(Dispatchers.IO) {
            ConnectCache.deleteAll(getApplication())
        }
    }

    // ── Message handling ─────────────────────────────────────────────────

    /** Route an incoming desktop message to the appropriate handler. */
    private fun handleMessage(message: SpyglassMessage) {
        Timber.i("← Received ${message.type}")
        try {
            when (message.type) {
                MessageType.WORLD_LIST      -> handleWorldList(message)
                MessageType.PLAYER_LIST     -> handlePlayerList(message)
                MessageType.PLAYER_DATA     -> handlePlayerData(message)
                MessageType.CHEST_CONTENTS  -> handleChestContents(message)
                MessageType.SEARCH_RESULTS  -> handleSearchResults(message)
                MessageType.STRUCTURE_LOCATIONS -> handleStructures(message)
                MessageType.MAP_RENDER      -> handleMapRender(message)
                MessageType.PLAYER_STATS    -> handlePlayerStats(message)
                MessageType.PLAYER_ADVANCEMENTS -> handlePlayerAdvancements(message)
                MessageType.PETS_LIST       -> handlePetsList(message)
                MessageType.SCAN_PROGRESS   -> handleScanProgress(message)
                MessageType.WORLD_CHANGED   -> handleWorldChanged(message)
                MessageType.ERROR           -> handleError(message)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to handle message: ${message.type}")
            CrashReporter.recordException(e, "Handle message failed: ${message.type}")
        }
    }

    /** World list received — cache it and re-select the previous world on reconnect. */
    private fun handleWorldList(message: SpyglassMessage) {
        val payload = json.decodeFromJsonElement(WorldListPayload.serializer(), message.payload)
        Timber.i("  ${payload.worlds.size} worlds: ${payload.worlds.joinToString { it.displayName }}")
        _worlds.value = payload.worlds

        viewModelScope.launch(Dispatchers.IO) {
            ConnectCache.saveMeta(
                getApplication(),
                ConnectCache.CacheMeta(selectedWorld = _selectedWorld.value, worlds = payload.worlds),
            )
        }

        // Re-select world after reconnect (desktop forgets selection on new WebSocket session)
        val currentWorld = _selectedWorld.value
        if (currentWorld != null && connectionState.value.isConnected && !worldReselected) {
            // Only re-select if the world is actually in the list the desktop sent
            val worldAvailable = payload.worlds.any { it.folderName == currentWorld }
            if (worldAvailable) {
                worldReselected = true
                Timber.i("  Re-selecting previously chosen world: $currentWorld")
                val selectPayload = json.encodeToJsonElement(SelectWorldPayload(currentWorld))
                client.sendRequest(MessageType.SELECT_WORLD, selectPayload)
                requestPlayerList()
            } else {
                Timber.w("  Previously selected world '$currentWorld' not in world list — clearing selection")
                _selectedWorld.value = null
            }
        }
    }

    /**
     * Player list received — auto-select the right player.
     *
     * Single player: auto-select the only player immediately.
     * Multi-player:  match against the configured IGN. If no match, wait
     *                for manual pick (never fall back to a random player).
     */
    private fun handlePlayerList(message: SpyglassMessage) {
        _loadingStatus.value = null
        val payload = json.decodeFromJsonElement(PlayerListPayload.serializer(), message.payload)
        Timber.i("  ${payload.players.size} players: ${payload.players.joinToString { it.name ?: it.uuid.take(8) }}")
        _playerList.value = payload.players

        if (payload.players.size == 1) {
            selectPlayer(payload.players.first().uuid)
        } else if (payload.players.isNotEmpty()) {
            tryAutoSelectByIgn(payload.players)
        }
    }

    /**
     * Attempt to auto-select a player by matching the configured IGN.
     * Runs on IO dispatcher since DataStore read is suspending.
     */
    private fun tryAutoSelectByIgn(players: List<PlayerSummary>) {
        viewModelScope.launch(Dispatchers.IO) {
            val ign = try {
                getApplication<Application>().dataStore.data.first()[PreferenceKeys.PLAYER_IGN] ?: ""
            } catch (_: Exception) { "" }

            if (ign.isBlank()) {
                Timber.w("  No IGN set and multiple players — waiting for manual pick")
                return@launch
            }

            val match = players.find { it.name.equals(ign, ignoreCase = true) }
            if (match != null) {
                Timber.i("  IGN '$ign' matched player ${match.uuid} — auto-selecting")
                selectPlayer(match.uuid)
            } else {
                Timber.w("  IGN '$ign' did not match any player — names: ${players.joinToString { "'${it.name}'" }}")
            }
        }
    }

    /**
     * Player data received — route to comparison if it matches a pending
     * compare request, otherwise update the main player data + cache.
     */
    private fun handlePlayerData(message: SpyglassMessage) {
        _loadingStatus.value = null
        val payload = json.decodeFromJsonElement(PlayerData.serializer(), message.payload)
        Timber.i("  Player: ${payload.playerName ?: payload.playerUuid?.take(8) ?: "owner"} — HP:${payload.health.toInt()} Food:${payload.foodLevel} XP:${payload.xpLevel} Dim:${payload.dimension}")

        // Route to comparison slot if this matches the pending compare UUID
        val compareUuid = pendingCompareUuid
        if (compareUuid != null && payload.playerUuid.equals(compareUuid, ignoreCase = true)) {
            _comparePlayerData.value = payload
            pendingCompareUuid = null
            return
        }

        // Persist UUID to DataStore so Settings can show it when disconnected
        payload.playerUuid?.let { uuid ->
            viewModelScope.launch(Dispatchers.IO) {
                getApplication<Application>().dataStore.edit { it[PreferenceKeys.PLAYER_UUID] = uuid }
            }
        }

        val changed = _playerData.value != payload
        _playerData.value = payload
        // Always auto-populate waypoints (StateFlow dedup may skip collectLatest)
        autoPopulateWaypoints(payload)
        if (changed) {
            cacheIfWorldSelected { world ->
                ConnectCache.savePlayerData(getApplication(), world, payload)
                _lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    /** Chest contents received — update state and cache if changed. */
    private fun handleChestContents(message: SpyglassMessage) {
        _loadingStatus.value = null
        val payload = json.decodeFromJsonElement(ChestContentsPayload.serializer(), message.payload)
        Timber.i("  ${payload.containers.size} containers (${payload.totalItemStacks} item stacks)")
        if (_chestContents.value != payload) {
            _chestContents.value = payload
            cacheIfWorldSelected { world ->
                ConnectCache.saveChests(getApplication(), world, payload)
            }
        }
    }

    /** Search results received. */
    private fun handleSearchResults(message: SpyglassMessage) {
        val payload = json.decodeFromJsonElement(SearchResultsPayload.serializer(), message.payload)
        Timber.i("  ${payload.results.size} search results")
        _searchResults.value = payload
    }

    /** Structure locations received — update state and cache if changed. */
    private fun handleStructures(message: SpyglassMessage) {
        val payload = json.decodeFromJsonElement(StructureLocationsPayload.serializer(), message.payload)
        Timber.i("  ${payload.structures.size} structures")
        if (_structures.value != payload.structures) {
            _structures.value = payload.structures
            cacheIfWorldSelected { world ->
                ConnectCache.saveStructures(getApplication(), world, payload.structures)
            }
        }
    }

    /** Map tiles received — accumulate, emit batch for live rendering, debounce full disk save. */
    private fun handleMapRender(message: SpyglassMessage) {
        _loadingStatus.value = null
        val payload = json.decodeFromJsonElement(MapRenderPayload.serializer(), message.payload)
        Timber.i("  Map tiles: ${payload.tiles.size} tiles")

        // Accumulate into ViewModel-scoped map (survives navigation)
        for (tile in payload.tiles) {
            val dimMap = accumulatedMapTiles.getOrPut(tile.dimension) { ConcurrentHashMap() }
            val key = (tile.chunkX.toLong() shl 32) or (tile.chunkZ.toLong() and 0xFFFFFFFFL)
            dimMap[key] = tile
        }

        // Emit individual batch for live progressive rendering
        _mapTileBatch.tryEmit(payload)

        // Debounce disk save — 5s after last batch, saves full accumulated set
        mapSaveJob?.cancel()
        mapSaveJob = viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(5000)
            val allTiles = accumulatedMapTiles.values.flatMap { it.values }
            if (allTiles.isNotEmpty()) {
                val player = _playerData.value
                val fullPayload = MapRenderPayload(
                    worldName = _selectedWorld.value ?: "",
                    tiles = allTiles,
                    playerX = player?.posX ?: 0.0,
                    playerZ = player?.posZ ?: 0.0,
                )
                cacheIfWorldSelected { world ->
                    ConnectCache.saveMapData(getApplication(), world, fullPayload)
                }
            }
        }
    }

    /** Player stats received — update state and cache if changed. */
    private fun handlePlayerStats(message: SpyglassMessage) {
        _loadingStatus.value = null
        val payload = json.decodeFromJsonElement(PlayerStatsPayload.serializer(), message.payload)
        if (_playerStats.value != payload) {
            _playerStats.value = payload
            cacheIfWorldSelected { world ->
                ConnectCache.saveStats(getApplication(), world, payload)
                _lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    /** Player advancements received — update state and cache if changed. */
    private fun handlePlayerAdvancements(message: SpyglassMessage) {
        _loadingStatus.value = null
        val payload = json.decodeFromJsonElement(PlayerAdvancementsPayload.serializer(), message.payload)
        if (_playerAdvancements.value != payload) {
            _playerAdvancements.value = payload
            cacheIfWorldSelected { world ->
                ConnectCache.saveAdvancements(getApplication(), world, payload)
                _lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    /** Pets list received — update state and cache if changed. */
    private fun handlePetsList(message: SpyglassMessage) {
        _loadingStatus.value = null
        val payload = json.decodeFromJsonElement(PetsListPayload.serializer(), message.payload)
        Timber.i("  ${payload.pets.size} pets")
        if (_pets.value != payload.pets) {
            _pets.value = payload.pets
            cacheIfWorldSelected { world ->
                ConnectCache.savePets(getApplication(), world, payload.pets)
                _lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    /** Scan progress update from desktop — update loading status with real-time feedback. */
    private fun handleScanProgress(message: SpyglassMessage) {
        if (_chestContents.value != null) return
        val payload = json.decodeFromJsonElement(ScanProgressPayload.serializer(), message.payload)
        val dim = payload.dimension.replace("_", " ").replaceFirstChar { it.uppercase() }
        val containers = if (payload.containersFound > 0) "\n${payload.containersFound} containers found" else ""
        _loadingStatus.value = "Scanning $dim (${payload.currentRegion} of ${payload.totalRegions})$containers"
    }

    /** World file changed on disk — refresh only data relevant to the active screen. */
    private fun handleWorldChanged(message: SpyglassMessage) {
        val payload = try {
            json.decodeFromJsonElement(WorldChangedPayload.serializer(), message.payload)
        } catch (_: Exception) {
            // Fallback: refresh player data if payload can't be decoded
            Timber.i("  World changed (no categories) — refreshing player data")
            requestPlayerData()
            return
        }
        val categories = payload.changedCategories.toSet()
        val screen = _activeScreen.value
        Timber.i("  World changed — categories=$categories, screen=$screen")

        // Player/level changes are handled by server push (Phase 1), but refresh
        // player list on the connect hub in case players joined/left
        if (categories.contains("player") || categories.contains("level")) {
            if (screen == "connect") requestPlayerList()
        }

        // Region changes — only refresh if the user is on a screen that needs it
        val hasRegionChange = categories.any { it.startsWith("region_") }
        if (hasRegionChange) {
            when (screen) {
                "chestfinder" -> requestChests()
                "pets" -> requestPets()
            }
        }
    }

    /** Desktop reported an error. */
    private fun handleError(message: SpyglassMessage) {
        val payload = json.decodeFromJsonElement(ErrorPayload.serializer(), message.payload)
        Timber.w("Server error: ${payload.code} — ${payload.message}")
        _loadingStatus.value = null
        // Emit empty map payload so MapState clears its loading indicator
        _mapTileBatch.tryEmit(MapRenderPayload("", emptyList(), 0.0, 0.0))
    }

    /**
     * Helper: run a cache-save block on IO if a world is currently selected.
     * Eliminates the repeated "if world != null → launch(IO) → save" pattern.
     */
    private fun cacheIfWorldSelected(block: suspend (String) -> Unit) {
        val world = _selectedWorld.value ?: return
        viewModelScope.launch(Dispatchers.IO) { block(world) }
    }

    // ── Waypoints ────────────────────────────────────────────────────────

    /**
     * Auto-generate system waypoints from player data.
     * Creates/updates/removes waypoints for world spawn, respawn point (bed
     * or respawn anchor), and last death location. Custom waypoints are
     * preserved — only auto-generated ones are touched.
     */
    private fun autoPopulateWaypoints(data: PlayerData) {
        val current = _connectWaypoints.value.toMutableList()

        fun upsertOrRemoveAuto(id: String, builder: () -> ConnectWaypoint?) {
            val wp = builder()
            val idx = current.indexOfFirst { it.id == id }
            if (wp != null) {
                if (idx >= 0) current[idx] = wp else current.add(wp)
            } else if (idx >= 0) {
                current.removeAt(idx)
            }
        }

        upsertOrRemoveAuto(ConnectWaypoint.ID_WORLD_SPAWN) {
            data.worldSpawn?.let {
                ConnectWaypoint(
                    id = ConnectWaypoint.ID_WORLD_SPAWN,
                    name = "World Spawn",
                    x = it.x, y = it.y, z = it.z,
                    dimension = it.dimension,
                    category = "spawn", color = "green",
                    source = ConnectWaypoint.SOURCE_AUTO,
                )
            }
        }

        upsertOrRemoveAuto(ConnectWaypoint.ID_RESPAWN) {
            data.spawnLocation?.let {
                val spawnType = if (it.dimension != "overworld" || data.spawnForced) "Respawn Anchor" else "Bed"
                ConnectWaypoint(
                    id = ConnectWaypoint.ID_RESPAWN,
                    name = "Respawn Point",
                    x = it.x, y = it.y, z = it.z,
                    dimension = it.dimension,
                    category = "spawn", color = "blue",
                    notes = spawnType,
                    source = ConnectWaypoint.SOURCE_AUTO,
                )
            }
        }

        upsertOrRemoveAuto(ConnectWaypoint.ID_DEATH) {
            data.lastDeathLocation?.let {
                ConnectWaypoint(
                    id = ConnectWaypoint.ID_DEATH,
                    name = "Last Death",
                    x = it.x, y = it.y, z = it.z,
                    dimension = it.dimension,
                    category = "death", color = "red",
                    source = ConnectWaypoint.SOURCE_AUTO,
                )
            }
        }

        Timber.d("Waypoints: ${current.count { it.source == ConnectWaypoint.SOURCE_AUTO }} auto, ${current.count { it.source == ConnectWaypoint.SOURCE_CUSTOM }} custom")
        _connectWaypoints.value = current
        saveWaypointsToCache()
    }

    /** Force-refresh waypoints from current player data (safety net for screen entry). */
    fun refreshWaypoints() {
        _playerData.value?.let { autoPopulateWaypoints(it) }
    }

    /** Create a custom waypoint. */
    fun createConnectWaypoint(waypoint: ConnectWaypoint) {
        _connectWaypoints.value = _connectWaypoints.value + waypoint
        saveWaypointsToCache()
    }

    /** Create a waypoint at the player's current position. */
    fun createWaypointAtPlayer(name: String, category: String = "other", color: String = "gold", notes: String = "") {
        val data = _playerData.value ?: return
        createConnectWaypoint(
            ConnectWaypoint(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                x = data.posX.toInt(),
                y = data.posY.toInt(),
                z = data.posZ.toInt(),
                dimension = data.dimension,
                category = category,
                color = color,
                notes = notes,
            ),
        )
    }

    /** Update a custom waypoint (matched by ID). */
    fun updateConnectWaypoint(waypoint: ConnectWaypoint) {
        _connectWaypoints.value = _connectWaypoints.value.map {
            if (it.id == waypoint.id) waypoint else it
        }
        saveWaypointsToCache()
    }

    /** Delete a waypoint by ID. */
    fun deleteConnectWaypoint(id: String) {
        _connectWaypoints.value = _connectWaypoints.value.filter { it.id != id }
        saveWaypointsToCache()
    }

    /** Persist current waypoints to disk cache. */
    private fun saveWaypointsToCache() {
        val world = _selectedWorld.value ?: return
        val waypoints = _connectWaypoints.value
        viewModelScope.launch(Dispatchers.IO) {
            ConnectCache.saveWaypoints(getApplication(), world, waypoints)
        }
    }

    // ── Internal helpers ─────────────────────────────────────────────────

    /** Send any crash logs saved to disk from previous sessions to the desktop. */
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

    /** Reset all StateFlows to their default values. Used by unpair() and clearCachedData(). */
    private fun resetAllState() {
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
        accumulatedMapTiles.clear()
        _selectedWorld.value = null
        _lastUpdated.value = null
        _pets.value = emptyList()
        _connectWaypoints.value = emptyList()
        _comparePlayerData.value = null
        SkinManager.clear()
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        Timber.uproot(logTree)
        reconnectJob?.cancel()
        mdnsDiscovery?.stopDiscovery()
        client.disconnect()
    }
}
