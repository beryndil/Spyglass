package dev.spyglass.android.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.spyglass.android.R
import dev.spyglass.android.core.shell.imageThemeDrawable
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.core.ui.SupportedLanguages
import dev.spyglass.android.data.repository.GameDataRepository

@Composable
private fun browseTabNames() = listOf(
    stringResource(R.string.browse_tab_blocks), stringResource(R.string.browse_tab_items),
    stringResource(R.string.browse_tab_recipes), stringResource(R.string.browse_tab_mobs),
    stringResource(R.string.browse_tab_trades), stringResource(R.string.browse_tab_biomes),
    stringResource(R.string.browse_tab_structures), stringResource(R.string.browse_tab_enchants),
    stringResource(R.string.browse_tab_potions), stringResource(R.string.browse_tab_advancements),
    stringResource(R.string.browse_tab_commands), stringResource(R.string.browse_tab_reference),
    stringResource(R.string.browse_tab_versions),
)

@Composable
private fun toolTabNames() = listOf(
    stringResource(R.string.calc_tab_todo), stringResource(R.string.calc_tab_shopping),
    stringResource(R.string.calc_tab_enchanting), stringResource(R.string.calc_tab_fill),
    stringResource(R.string.calc_tab_shapes), stringResource(R.string.calc_tab_maze),
    stringResource(R.string.calc_tab_storage), stringResource(R.string.calc_tab_smelt),
    stringResource(R.string.calc_tab_nether), stringResource(R.string.calc_tab_game_clock),
    stringResource(R.string.calc_tab_light), stringResource(R.string.calc_tab_notes),
    stringResource(R.string.calc_tab_waypoints), stringResource(R.string.calc_tab_redstone),
    stringResource(R.string.calc_tab_librarian), stringResource(R.string.calc_tab_food),
    stringResource(R.string.calc_tab_banners), stringResource(R.string.calc_tab_trims),
    stringResource(R.string.calc_tab_loot),
)

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

@Composable
private fun settingsTabs() = listOf(
    SpyglassTab(stringResource(R.string.settings_tab_appearance), PixelIcons.Enchant, untinted = true),
    SpyglassTab(stringResource(R.string.settings_tab_gameplay), PixelIcons.Blocks),
    SpyglassTab(stringResource(R.string.settings_tab_general), PixelIcons.Storage),
    SpyglassTab(stringResource(R.string.settings_tab_about_privacy), PixelIcons.Globe),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onCalcTab: (Int) -> Unit = {},
    onAbout: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onChangelog: () -> Unit = {},
    vm: SettingsViewModel = viewModel(),
) {
    val defaultBrowseTab    by vm.defaultBrowseTab.collectAsStateWithLifecycle()
    val defaultToolTab      by vm.defaultToolTab.collectAsStateWithLifecycle()
    val showTipOfDay        by vm.showTipOfDay.collectAsStateWithLifecycle()
    val showFavoritesOnHome by vm.showFavoritesOnHome.collectAsStateWithLifecycle()
    val gameClockEnabled    by vm.gameClockEnabled.collectAsStateWithLifecycle()
    val allFavorites        by vm.allFavorites.collectAsStateWithLifecycle()
    val backgroundTheme     by vm.backgroundTheme.collectAsStateWithLifecycle()
    val minecraftEdition    by vm.minecraftEdition.collectAsStateWithLifecycle()
    val minecraftVersion    by vm.minecraftVersion.collectAsStateWithLifecycle()
    val versionFilterMode   by vm.versionFilterMode.collectAsStateWithLifecycle()
    val analyticsConsent    by vm.analyticsConsent.collectAsStateWithLifecycle()
    val crashConsent        by vm.crashConsent.collectAsStateWithLifecycle()
    val adPersonalizationConsent by vm.adPersonalizationConsent.collectAsStateWithLifecycle()
    val hapticFeedback      by vm.hapticFeedback.collectAsStateWithLifecycle()
    val reduceAnimations    by vm.reduceAnimations.collectAsStateWithLifecycle()
    val highContrast        by vm.highContrast.collectAsStateWithLifecycle()
    val defaultStartupTab   by vm.defaultStartupTab.collectAsStateWithLifecycle()
    val hideUnobtainable    by vm.hideUnobtainableBlocks.collectAsStateWithLifecycle()
    val showExperimental    by vm.showExperimental.collectAsStateWithLifecycle()
    val appLockEnabled      by vm.appLockEnabled.collectAsStateWithLifecycle()
    val syncFrequencyHours  by vm.syncFrequencyHours.collectAsStateWithLifecycle()
    val offlineMode         by vm.offlineMode.collectAsStateWithLifecycle()
    val fontScale           by vm.fontScale.collectAsStateWithLifecycle()
    val textureState        by TextureManager.state.collectAsStateWithLifecycle()
    val syncing             by vm.syncing.collectAsStateWithLifecycle()
    val appLanguage         by vm.appLanguage.collectAsStateWithLifecycle()
    val translateGameData   by vm.translateGameData.collectAsStateWithLifecycle()
    val showOriginalNames   by vm.showOriginalNames.collectAsStateWithLifecycle()

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val mcRepo by produceState<GameDataRepository?>(null) {
        value = kotlinx.coroutines.withContext(Dispatchers.IO) { GameDataRepository.get(app) }
    }
    val mcUpdateTx by remember(mcRepo) {
        mcRepo?.let { translationMapFlow(app.dataStore, it, "mc_update") }
            ?: kotlinx.coroutines.flow.flowOf(emptyMap())
    }.collectAsState(initial = emptyMap())
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()

    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                CustomWallpaper.save(context, uri)
                withContext(Dispatchers.Main) { vm.setBackgroundTheme("custom") }
            }
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var versionExpanded by remember { mutableStateOf(false) }
    var storageBytes by remember { mutableStateOf(vm.getTextureStorageBytes()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header row — always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { hapticClick(); onBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Tab row
        SpyglassTabRow(
            tabs = settingsTabs(),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        when (selectedTab) {
            0 -> AppearanceTab(
                backgroundTheme = backgroundTheme,
                highContrast = highContrast,
                fontScale = fontScale,
                hapticFeedback = hapticFeedback,
                reduceAnimations = reduceAnimations,
                hapticClick = hapticClick,
                hapticConfirm = hapticConfirm,
                photoPickerLauncher = photoPickerLauncher,
                vm = vm,
            )
            1 -> GameplayTab(
                minecraftEdition = minecraftEdition,
                minecraftVersion = minecraftVersion,
                versionFilterMode = versionFilterMode,
                hideUnobtainable = hideUnobtainable,
                showExperimental = showExperimental,
                gameClockEnabled = gameClockEnabled,
                defaultBrowseTab = defaultBrowseTab,
                defaultToolTab = defaultToolTab,
                versionExpanded = versionExpanded,
                onVersionExpandedChange = { versionExpanded = it },
                mcUpdateTx = mcUpdateTx,
                hapticClick = hapticClick,
                onCalcTab = onCalcTab,
                vm = vm,
            )
            2 -> GeneralTab(
                defaultStartupTab = defaultStartupTab,
                showTipOfDay = showTipOfDay,
                showFavoritesOnHome = showFavoritesOnHome,
                offlineMode = offlineMode,
                syncFrequencyHours = syncFrequencyHours,
                syncing = syncing,
                textureState = textureState,
                storageBytes = storageBytes,
                allFavorites = allFavorites,
                hapticClick = hapticClick,
                hapticConfirm = hapticConfirm,
                onStorageBytesChange = { storageBytes = it },
                vm = vm,
            )
            3 -> AboutPrivacyTab(
                appLockEnabled = appLockEnabled,
                analyticsConsent = analyticsConsent,
                crashConsent = crashConsent,
                adPersonalizationConsent = adPersonalizationConsent,
                hapticClick = hapticClick,
                hapticConfirm = hapticConfirm,
                onShowDeleteConfirm = { showDeleteConfirm = true },
                onAbout = onAbout,
                onChangelog = onChangelog,
                onFeedback = onFeedback,
                vm = vm,
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_data_title), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    stringResource(R.string.settings_delete_data_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    hapticConfirm()
                    vm.deleteAllUserData()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.settings_delete_data_confirm), color = Red400)
                }
            },
            dismissButton = {
                TextButton(onClick = { hapticClick(); showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// Tab 0: Appearance
// ══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceTab(
    backgroundTheme: String,
    highContrast: Boolean,
    fontScale: Int,
    hapticFeedback: Boolean,
    reduceAnimations: Boolean,
    hapticClick: () -> Unit,
    hapticConfirm: () -> Unit,
    photoPickerLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
    vm: SettingsViewModel,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Theme picker ──
        item(key = "theme_picker") {
            ResultCard {
                Text(
                    stringResource(R.string.settings_backgrounds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ImageThemeOrder.forEach { key ->
                        val info = ThemeInfoMap[key] ?: return@forEach
                        val drawableRes = imageThemeDrawable(key) ?: return@forEach
                        val isSelected = backgroundTheme == key
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        val shape = RoundedCornerShape(8.dp)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(64.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(9f / 16f)
                                    .clip(shape)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = borderColor,
                                        shape = shape,
                                    )
                                    .clickable { hapticClick(); vm.setBackgroundTheme(key) },
                            ) {
                                Image(
                                    painter = painterResource(drawableRes),
                                    contentDescription = info.label,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter,
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                info.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                    // Custom wallpaper card
                    run {
                        val hasCustom = CustomWallpaper.hasWallpaper(context)
                        val isSelected = backgroundTheme == "custom"
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        val shape = RoundedCornerShape(8.dp)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(64.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(9f / 16f)
                                    .clip(shape)
                                    .background(Color(0xFF1A1A1A), shape)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = borderColor,
                                        shape = shape,
                                    )
                                    .clickable {
                                        hapticClick()
                                        if (hasCustom) {
                                            vm.setBackgroundTheme("custom")
                                        } else {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                val thumb = CustomWallpaper.cachedBitmap
                                if (hasCustom && thumb != null) {
                                    Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = stringResource(R.string.settings_my_photo),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.TopCenter,
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.AddPhotoAlternate,
                                        contentDescription = stringResource(R.string.settings_choose_photo),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            if (isSelected && hasCustom) {
                                Text(
                                    stringResource(R.string.settings_change_photo),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.clickable {
                                        hapticClick()
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                )
                            } else {
                                Text(
                                    stringResource(R.string.settings_my_photo),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_solid_colors),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SolidThemeOrder.chunked(9).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { key ->
                                val info = ThemeInfoMap[key] ?: return@forEach
                                val isSelected = backgroundTheme == key
                                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(info.background, CircleShape)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = borderColor,
                                            shape = CircleShape,
                                        )
                                        .clickable { hapticClick(); vm.setBackgroundTheme(key) },
                                )
                            }
                        }
                    }
                }
                Text(
                    ThemeInfoMap[backgroundTheme]?.label ?: stringResource(R.string.settings_default_theme),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // ── Other appearance settings ──
        item(key = "appearance_settings") {
            ResultCard {
                SettingsToggle(
                    title = stringResource(R.string.settings_high_contrast),
                    description = stringResource(R.string.settings_high_contrast_desc),
                    checked = highContrast,
                    onCheckedChange = vm::setHighContrast,
                )

                SpyglassDivider()

                // Font size
                Text(
                    stringResource(R.string.settings_font_size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.settings_font_size_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FontScaleOptions.forEachIndexed { i, (label, _) ->
                        FilterChip(
                            selected = fontScale == i,
                            onClick = { hapticClick(); vm.setFontScale(i) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                SpyglassDivider()

                SettingsToggle(
                    title = stringResource(R.string.settings_haptic_feedback),
                    description = stringResource(R.string.settings_haptic_feedback_desc),
                    checked = hapticFeedback,
                    onCheckedChange = vm::setHapticFeedback,
                )

                SpyglassDivider()

                SettingsToggle(
                    title = stringResource(R.string.settings_reduce_animations),
                    description = stringResource(R.string.settings_reduce_animations_desc),
                    checked = reduceAnimations,
                    onCheckedChange = vm::setReduceAnimations,
                )
            }
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════
// Tab 1: Gameplay
// ══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GameplayTab(
    minecraftEdition: String,
    minecraftVersion: String,
    versionFilterMode: String,
    hideUnobtainable: Boolean,
    showExperimental: Boolean,
    gameClockEnabled: Boolean,
    defaultBrowseTab: Int,
    defaultToolTab: Int,
    versionExpanded: Boolean,
    onVersionExpandedChange: (Boolean) -> Unit,
    mcUpdateTx: Map<String, Map<String, String>>,
    hapticClick: () -> Unit,
    onCalcTab: (Int) -> Unit,
    vm: SettingsViewModel,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Game Filters ──
        item(key = "game_filters") {
            SectionHeader(stringResource(R.string.settings_game_settings))
            ResultCard {
                Text(
                    stringResource(R.string.settings_game_filter_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Text(stringResource(R.string.settings_edition), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                TogglePill(
                    options = listOf(stringResource(R.string.settings_edition_java), stringResource(R.string.settings_edition_bedrock)),
                    selected = if (minecraftEdition == "bedrock") 1 else 0,
                    onSelect = { vm.setMinecraftEdition(if (it == 1) "bedrock" else "java") },
                )

                val versions = if (minecraftEdition == "bedrock") MinecraftVersions.BEDROCK_VERSIONS else MinecraftVersions.JAVA_VERSIONS
                val latestLabel = stringResource(R.string.settings_version_latest)
                val displayVersion = minecraftVersion.ifBlank { latestLabel }
                Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Box {
                    OutlinedButton(onClick = { hapticClick(); onVersionExpandedChange(true) }) {
                        Text(displayVersion, color = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(expanded = versionExpanded, onDismissRequest = { onVersionExpandedChange(false) }) {
                        DropdownMenuItem(
                            text = { Text(latestLabel, color = if (minecraftVersion.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                            onClick = { hapticClick(); vm.setMinecraftVersion(""); onVersionExpandedChange(false) },
                        )
                        versions.reversed().forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v, color = if (minecraftVersion == v) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                                onClick = { hapticClick(); vm.setMinecraftVersion(v); onVersionExpandedChange(false) },
                            )
                        }
                    }
                }

                Text(stringResource(R.string.settings_filter_mode), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val modes = listOf("show_all" to stringResource(R.string.settings_filter_show_all), "highlight" to stringResource(R.string.settings_filter_highlight), "hide" to stringResource(R.string.settings_filter_hide))
                    modes.forEach { (key, label) ->
                        FilterChip(
                            selected = versionFilterMode == key,
                            onClick = { hapticClick(); vm.setVersionFilterMode(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                SpyglassDivider()

                SettingsToggle(
                    title = stringResource(R.string.settings_hide_unobtainable),
                    description = stringResource(R.string.settings_hide_unobtainable_desc),
                    checked = hideUnobtainable,
                    onCheckedChange = vm::setHideUnobtainableBlocks,
                )

                SpyglassDivider()

                SettingsToggle(
                    title = stringResource(R.string.settings_show_experimental),
                    description = stringResource(R.string.settings_show_experimental_desc),
                    checked = showExperimental,
                    onCheckedChange = vm::setShowExperimental,
                )

                SpyglassDivider()

                SettingsToggle(
                    title = stringResource(R.string.settings_game_clock),
                    description = stringResource(R.string.settings_game_clock_desc),
                    checked = gameClockEnabled,
                    onCheckedChange = vm::setGameClockEnabled,
                )
                Text(
                    stringResource(R.string.settings_configure_clock),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { hapticClick(); onCalcTab(9) },
                )
            }

            val selectedVersion = minecraftVersion.ifBlank { MinecraftVersions.JAVA_VERSIONS.last() }
            val updateInfo = remember(selectedVersion) { MinecraftUpdates.forVersion(selectedVersion) }
            if (updateInfo != null) {
                val tx = mcUpdateTx[updateInfo.version]
                Spacer(Modifier.height(4.dp))
                VersionCard(
                    version = updateInfo.version,
                    name = tx?.get("name") ?: updateInfo.name,
                    releaseDate = tx?.get("date") ?: updateInfo.releaseDate,
                    accentColor = updateInfo.color,
                    icon = updateInfo.icon,
                    changelog = updateInfo.changelog.mapIndexed { i, fallback ->
                        tx?.get("c${i + 1}") ?: fallback
                    },
                )
            }
        }

        // ── Default Tabs ──
        item(key = "default_tabs") {
            SectionHeader(stringResource(R.string.settings_defaults))
            ResultCard {
                // Default browse tab
                Text(
                    stringResource(R.string.settings_default_browse_tab),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.settings_browse_tab_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    browseTabNames().forEachIndexed { i, name ->
                        FilterChip(
                            selected = defaultBrowseTab == i,
                            onClick = { hapticClick(); vm.setDefaultBrowseTab(i) },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                SpyglassDivider()

                // Default tool tab
                Text(
                    stringResource(R.string.settings_default_tool_tab),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.settings_tool_tab_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    toolTabNames().forEachIndexed { i, name ->
                        FilterChip(
                            selected = defaultToolTab == i,
                            onClick = { hapticClick(); vm.setDefaultToolTab(i) },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════
// Tab 2: General
// ══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneralTab(
    defaultStartupTab: Int,
    showTipOfDay: Boolean,
    showFavoritesOnHome: Boolean,
    offlineMode: Boolean,
    syncFrequencyHours: Int,
    syncing: Boolean,
    textureState: TextureManager.TextureState,
    storageBytes: Long,
    allFavorites: List<dev.spyglass.android.data.db.entities.FavoriteEntity>,
    hapticClick: () -> Unit,
    hapticConfirm: () -> Unit,
    onStorageBytesChange: (Long) -> Unit,
    vm: SettingsViewModel,
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── App Behavior ──
        item(key = "app_behavior") {
            SectionHeader(stringResource(R.string.settings_defaults))
            ResultCard {
                // Startup screen
                Text(
                    stringResource(R.string.settings_startup_tab),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.settings_startup_tab_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val tabs = listOf(
                        stringResource(R.string.nav_home),
                        stringResource(R.string.nav_browse),
                        stringResource(R.string.nav_tools),
                        stringResource(R.string.nav_search),
                    )
                    tabs.forEachIndexed { i, name ->
                        FilterChip(
                            selected = defaultStartupTab == i,
                            onClick = { hapticClick(); vm.setDefaultStartupTab(i) },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                SpyglassDivider()

                SettingsToggle(
                    title = stringResource(R.string.settings_tip_of_day),
                    description = stringResource(R.string.settings_tip_of_day_desc),
                    checked = showTipOfDay,
                    onCheckedChange = vm::setShowTipOfDay,
                )

                SpyglassDivider()

                SettingsToggle(
                    title = stringResource(R.string.settings_favorites_on_home),
                    description = stringResource(R.string.settings_favorites_on_home_desc),
                    checked = showFavoritesOnHome,
                    onCheckedChange = vm::setShowFavoritesOnHome,
                )
            }
        }

        // ── Data & Sync ──
        item(key = "data_sync") {
            SectionHeader(stringResource(R.string.settings_data_sync))
            ResultCard {
                // Offline mode
                SettingsToggle(
                    title = stringResource(R.string.settings_offline_mode),
                    description = stringResource(R.string.settings_offline_mode_desc),
                    checked = offlineMode,
                    onCheckedChange = vm::setOfflineMode,
                )

                SpyglassDivider()

                // Sync frequency
                Text(
                    stringResource(R.string.settings_sync_frequency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (offlineMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.settings_sync_frequency_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (offlineMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val options = listOf(1 to "1h", 6 to "6h", 12 to "12h", 24 to "24h")
                    options.forEach { (hours, label) ->
                        FilterChip(
                            selected = syncFrequencyHours == hours,
                            onClick = { hapticClick(); if (!offlineMode) vm.setSyncFrequencyHours(hours) },
                            enabled = !offlineMode,
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { hapticClick(); vm.syncNow() },
                    enabled = !offlineMode && !syncing,
                ) {
                    if (syncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_syncing), color = MaterialTheme.colorScheme.secondary)
                    } else {
                        Text(stringResource(R.string.settings_sync_now), color = MaterialTheme.colorScheme.primary)
                    }
                }

                SpyglassDivider()

                // Storage usage
                Text(
                    stringResource(R.string.settings_storage_usage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.settings_downloaded_textures, formatBytes(storageBytes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (textureState == TextureManager.TextureState.DOWNLOADED) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = {
                        hapticConfirm()
                        vm.clearTextureCache()
                        onStorageBytesChange(0L)
                    }) {
                        Text(stringResource(R.string.settings_clear_cache), color = MaterialTheme.colorScheme.primary)
                    }
                }

                SpyglassDivider()

                // Spyglass Connect link
                Text(
                    stringResource(R.string.settings_connect_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.settings_connect_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    stringResource(R.string.settings_connect_download),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        hapticClick()
                        uriHandler.openUri("https://hardknocks.com/spyglass-connect")
                    },
                )
            }
        }

        // ── Favorites ──
        item(key = "favorites") {
            ResultCard {
                Text(
                    stringResource(R.string.settings_favorites),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (allFavorites.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_no_favorites),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    Text(
                        stringResource(R.string.settings_favorites_count, allFavorites.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    allFavorites.forEach { fav ->
                        Text(
                            "\u2605  ${fav.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SpyglassDivider()
                    TextButton(onClick = { hapticConfirm(); vm.clearAllFavorites() }) {
                        Text(stringResource(R.string.settings_clear_all_favorites), color = Red400)
                    }
                }
            }
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════
// Tab 3: About & Privacy
// ══════════════════════════════════════════════════════════════════

@Composable
private fun AboutPrivacyTab(
    appLockEnabled: Boolean,
    analyticsConsent: Boolean,
    crashConsent: Boolean,
    adPersonalizationConsent: Boolean,
    hapticClick: () -> Unit,
    hapticConfirm: () -> Unit,
    onShowDeleteConfirm: () -> Unit,
    onAbout: () -> Unit,
    onChangelog: () -> Unit,
    onFeedback: () -> Unit,
    vm: SettingsViewModel,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Privacy & Security ──
        item(key = "privacy_security") {
            SectionHeader(stringResource(R.string.settings_privacy_security))
            ResultCard {
                SettingsToggle(
                    title = stringResource(R.string.settings_app_lock),
                    description = stringResource(R.string.settings_app_lock_desc),
                    checked = appLockEnabled,
                    onCheckedChange = vm::setAppLockEnabled,
                )

                SpyglassDivider()

                SettingsToggle(
                    title = stringResource(R.string.consent_analytics),
                    description = stringResource(R.string.consent_analytics_desc),
                    checked = analyticsConsent,
                    onCheckedChange = vm::setAnalyticsConsent,
                )

                SpyglassDivider()

                SettingsToggle(
                    title = stringResource(R.string.consent_crash_reports),
                    description = stringResource(R.string.consent_crash_reports_desc),
                    checked = crashConsent,
                    onCheckedChange = vm::setCrashConsent,
                )

                SpyglassDivider()

                SettingsToggle(
                    title = stringResource(R.string.consent_personalized_ads),
                    description = stringResource(R.string.consent_personalized_ads_desc),
                    checked = adPersonalizationConsent,
                    onCheckedChange = vm::setAdPersonalizationConsent,
                )

                SpyglassDivider()

                Text(
                    text = stringResource(R.string.settings_privacy_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        hapticClick()
                        uriHandler.openUri("https://hardknocks.university/privacy-policy.html")
                    },
                )

                SpyglassDivider()

                TextButton(onClick = { hapticConfirm(); onShowDeleteConfirm() }) {
                    Text(stringResource(R.string.settings_delete_data), color = Red400)
                }
            }
        }

        // ── About ──
        item(key = "about") {
            SectionHeader(stringResource(R.string.settings_about_section))
            ResultCard {
                SettingsLink(
                    title = stringResource(R.string.settings_about),
                    description = stringResource(R.string.settings_about_desc),
                    onClick = onAbout,
                )
                SpyglassDivider()
                SettingsLink(
                    title = stringResource(R.string.settings_changelog),
                    description = stringResource(R.string.settings_changelog_desc),
                    onClick = onChangelog,
                )
                SpyglassDivider()
                SettingsLink(
                    title = stringResource(R.string.settings_wiki),
                    description = stringResource(R.string.settings_wiki_desc),
                    onClick = { uriHandler.openUri("https://github.com/beryndil/Spyglass/wiki") },
                )
                SpyglassDivider()
                SettingsLink(
                    title = stringResource(R.string.settings_send_feedback),
                    description = stringResource(R.string.settings_send_feedback_desc),
                    onClick = onFeedback,
                )
                SpyglassDivider()
                SettingsLink(
                    title = stringResource(R.string.settings_rate_app),
                    description = stringResource(R.string.settings_rate_app_desc),
                    onClick = {
                        uriHandler.openUri("https://play.google.com/store/apps/details?id=dev.spyglass.android")
                    },
                )
                SpyglassDivider()
                SettingsLink(
                    title = stringResource(R.string.settings_app_permissions),
                    description = stringResource(R.string.settings_app_permissions_desc),
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    },
                )
            }
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val hapticConfirm = rememberHapticConfirm()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = { hapticConfirm(); onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Composable
private fun SettingsLink(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val hapticClick = rememberHapticClick()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { hapticClick(); onClick() }
            .padding(vertical = 6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}
