package dev.spyglass.android.connect

import android.app.Application
import dev.spyglass.android.connect.client.*
import dev.spyglass.android.core.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

internal class ConnectMessageHandler(
    private val application: Application,
    private val scope: CoroutineScope,
    private val json: Json,
    private val worlds: MutableStateFlow<List<WorldInfo>>,
    private val selectedWorld: MutableStateFlow<String?>,
    private val playerList: MutableStateFlow<List<PlayerSummary>>,
    private val playerData: MutableStateFlow<PlayerData?>,
    private val comparePlayerData: MutableStateFlow<PlayerData?>,
    private val chestContents: MutableStateFlow<ChestContentsPayload?>,
    private val searchResults: MutableStateFlow<SearchResultsPayload?>,
    private val structures: MutableStateFlow<List<StructureLocation>>,
    private val playerStats: MutableStateFlow<PlayerStatsPayload?>,
    private val playerAdvancements: MutableStateFlow<PlayerAdvancementsPayload?>,
    private val pets: MutableStateFlow<List<PetData>>,
    private val loadingStatus: MutableStateFlow<String?>,
    private val lastUpdated: MutableStateFlow<Long?>,
    private val mapTileBatch: MutableSharedFlow<MapRenderPayload>,
    private val activeScreen: MutableStateFlow<String?>,
    val accumulatedMapTiles: ConcurrentHashMap<String, ConcurrentHashMap<Long, MapTile>>,
    private val callbacks: Callbacks,
) {

    interface Callbacks {
        var pendingCompareUuid: String?
        var worldReselected: Boolean
        val connectionStateIsConnected: Boolean

        fun selectPlayer(uuid: String?)
        fun tryAutoSelectByIgn(players: List<PlayerSummary>)
        fun autoPopulateWaypoints(data: PlayerData)
        fun reselectWorldOnDesktop(worldFolder: String)
        fun requestPlayerData()
        fun requestPlayerList()
        fun requestChests()
        fun requestPets()
        fun persistPlayerUuid(uuid: String)
    }

    private var mapSaveJob: Job? = null

    fun handleMessage(message: SpyglassMessage) {
        Timber.i("\u2190 Received ${message.type}")
        try {
            when (message.type) {
                MessageType.WORLD_LIST          -> handleWorldList(message)
                MessageType.PLAYER_LIST         -> handlePlayerList(message)
                MessageType.PLAYER_DATA         -> handlePlayerData(message)
                MessageType.CHEST_CONTENTS      -> handleChestContents(message)
                MessageType.SEARCH_RESULTS      -> handleSearchResults(message)
                MessageType.STRUCTURE_LOCATIONS  -> handleStructures(message)
                MessageType.MAP_RENDER          -> handleMapRender(message)
                MessageType.PLAYER_STATS        -> handlePlayerStats(message)
                MessageType.PLAYER_ADVANCEMENTS -> handlePlayerAdvancements(message)
                MessageType.PETS_LIST           -> handlePetsList(message)
                MessageType.SCAN_PROGRESS       -> handleScanProgress(message)
                MessageType.WORLD_CHANGED       -> handleWorldChanged(message)
                MessageType.ERROR               -> handleError(message)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to handle message: ${message.type}")
            CrashReporter.recordException(e, "Handle message failed: ${message.type}")
        }
    }

    private fun handleWorldList(message: SpyglassMessage) {
        val payload = json.decodeFromJsonElement(WorldListPayload.serializer(), message.payload)
        Timber.i("  ${payload.worlds.size} worlds: ${payload.worlds.joinToString { it.displayName }}")
        worlds.value = payload.worlds

        scope.launch(Dispatchers.IO) {
            ConnectCache.saveMeta(
                application,
                ConnectCache.CacheMeta(selectedWorld = selectedWorld.value, worlds = payload.worlds),
            )
        }

        val currentWorld = selectedWorld.value
        if (currentWorld != null && callbacks.connectionStateIsConnected && !callbacks.worldReselected) {
            val worldAvailable = payload.worlds.any { it.folderName == currentWorld }
            if (worldAvailable) {
                callbacks.worldReselected = true
                Timber.i("  Re-selecting previously chosen world: $currentWorld")
                callbacks.reselectWorldOnDesktop(currentWorld)
                callbacks.requestPlayerList()
            } else {
                Timber.w("  Previously selected world '$currentWorld' not in world list \u2014 clearing selection")
                selectedWorld.value = null
            }
        }
    }

    private fun handlePlayerList(message: SpyglassMessage) {
        loadingStatus.value = null
        val payload = json.decodeFromJsonElement(PlayerListPayload.serializer(), message.payload)
        Timber.i("  ${payload.players.size} players: ${payload.players.joinToString { it.name ?: it.uuid.take(8) }}")
        playerList.value = payload.players

        if (payload.players.size == 1) {
            callbacks.selectPlayer(payload.players.first().uuid)
        } else if (payload.players.isNotEmpty()) {
            callbacks.tryAutoSelectByIgn(payload.players)
        }
    }

    private fun handlePlayerData(message: SpyglassMessage) {
        loadingStatus.value = null
        val payload = json.decodeFromJsonElement(PlayerData.serializer(), message.payload)
        Timber.i("  Player: ${payload.playerName ?: payload.playerUuid?.take(8) ?: "owner"} \u2014 HP:${payload.health.toInt()} Food:${payload.foodLevel} XP:${payload.xpLevel} Dim:${payload.dimension}")

        val compareUuid = callbacks.pendingCompareUuid
        if (compareUuid != null && payload.playerUuid.equals(compareUuid, ignoreCase = true)) {
            comparePlayerData.value = payload
            callbacks.pendingCompareUuid = null
            return
        }

        payload.playerUuid?.let { callbacks.persistPlayerUuid(it) }

        val changed = playerData.value != payload
        playerData.value = payload
        callbacks.autoPopulateWaypoints(payload)
        if (changed) {
            cacheIfWorldSelected { world ->
                ConnectCache.savePlayerData(application, world, payload)
                lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    private fun handleChestContents(message: SpyglassMessage) {
        loadingStatus.value = null
        val payload = json.decodeFromJsonElement(ChestContentsPayload.serializer(), message.payload)
        Timber.i("  ${payload.containers.size} containers (${payload.totalItemStacks} item stacks)")
        if (chestContents.value != payload) {
            chestContents.value = payload
            cacheIfWorldSelected { world ->
                ConnectCache.saveChests(application, world, payload)
            }
        }
    }

    private fun handleSearchResults(message: SpyglassMessage) {
        val payload = json.decodeFromJsonElement(SearchResultsPayload.serializer(), message.payload)
        Timber.i("  ${payload.results.size} search results")
        searchResults.value = payload
    }

    private fun handleStructures(message: SpyglassMessage) {
        val payload = json.decodeFromJsonElement(StructureLocationsPayload.serializer(), message.payload)
        Timber.i("  ${payload.structures.size} structures")
        if (structures.value != payload.structures) {
            structures.value = payload.structures
            cacheIfWorldSelected { world ->
                ConnectCache.saveStructures(application, world, payload.structures)
            }
        }
    }

    private fun handleMapRender(message: SpyglassMessage) {
        loadingStatus.value = null
        val payload = json.decodeFromJsonElement(MapRenderPayload.serializer(), message.payload)
        Timber.i("  Map tiles: ${payload.tiles.size} tiles")

        for (tile in payload.tiles) {
            val dimMap = accumulatedMapTiles.getOrPut(tile.dimension) { ConcurrentHashMap() }
            val key = (tile.chunkX.toLong() shl 32) or (tile.chunkZ.toLong() and 0xFFFFFFFFL)
            dimMap[key] = tile
        }

        mapTileBatch.tryEmit(payload)

        mapSaveJob?.cancel()
        mapSaveJob = scope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(5000)
            val allTiles = accumulatedMapTiles.values.flatMap { it.values }
            if (allTiles.isNotEmpty()) {
                val player = playerData.value
                val fullPayload = MapRenderPayload(
                    worldName = selectedWorld.value ?: "",
                    tiles = allTiles,
                    playerX = player?.posX ?: 0.0,
                    playerZ = player?.posZ ?: 0.0,
                )
                cacheIfWorldSelected { world ->
                    ConnectCache.saveMapData(application, world, fullPayload)
                }
            }
        }
    }

    private fun handlePlayerStats(message: SpyglassMessage) {
        loadingStatus.value = null
        val payload = json.decodeFromJsonElement(PlayerStatsPayload.serializer(), message.payload)
        if (playerStats.value != payload) {
            playerStats.value = payload
            cacheIfWorldSelected { world ->
                ConnectCache.saveStats(application, world, payload)
                lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    private fun handlePlayerAdvancements(message: SpyglassMessage) {
        loadingStatus.value = null
        val payload = json.decodeFromJsonElement(PlayerAdvancementsPayload.serializer(), message.payload)
        if (playerAdvancements.value != payload) {
            playerAdvancements.value = payload
            cacheIfWorldSelected { world ->
                ConnectCache.saveAdvancements(application, world, payload)
                lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    private fun handlePetsList(message: SpyglassMessage) {
        loadingStatus.value = null
        val payload = json.decodeFromJsonElement(PetsListPayload.serializer(), message.payload)
        Timber.i("  ${payload.pets.size} pets")
        if (pets.value != payload.pets) {
            pets.value = payload.pets
            cacheIfWorldSelected { world ->
                ConnectCache.savePets(application, world, payload.pets)
                lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    private fun handleScanProgress(message: SpyglassMessage) {
        if (chestContents.value != null) return
        val payload = json.decodeFromJsonElement(ScanProgressPayload.serializer(), message.payload)
        val dim = payload.dimension.replace("_", " ").replaceFirstChar { it.uppercase() }
        val containers = if (payload.containersFound > 0) "\n${payload.containersFound} containers found" else ""
        loadingStatus.value = "Scanning $dim (${payload.currentRegion} of ${payload.totalRegions})$containers"
    }

    private fun handleWorldChanged(message: SpyglassMessage) {
        val payload = try {
            json.decodeFromJsonElement(WorldChangedPayload.serializer(), message.payload)
        } catch (_: Exception) {
            Timber.i("  World changed (no categories) \u2014 refreshing player data")
            callbacks.requestPlayerData()
            return
        }
        val categories = payload.changedCategories.toSet()
        val screen = activeScreen.value
        Timber.i("  World changed \u2014 categories=$categories, screen=$screen")

        if (categories.contains("player") || categories.contains("level")) {
            if (screen == "connect") callbacks.requestPlayerList()
        }

        val hasRegionChange = categories.any { it.startsWith("region_") }
        if (hasRegionChange) {
            when (screen) {
                "chestfinder" -> callbacks.requestChests()
                "pets" -> callbacks.requestPets()
            }
        }
    }

    private fun handleError(message: SpyglassMessage) {
        val payload = json.decodeFromJsonElement(ErrorPayload.serializer(), message.payload)
        Timber.w("Server error: ${payload.code} \u2014 ${payload.message}")
        loadingStatus.value = null
        mapTileBatch.tryEmit(MapRenderPayload("", emptyList(), 0.0, 0.0))
    }

    private fun cacheIfWorldSelected(block: suspend (String) -> Unit) {
        val world = selectedWorld.value ?: return
        scope.launch(Dispatchers.IO) { block(world) }
    }
}
