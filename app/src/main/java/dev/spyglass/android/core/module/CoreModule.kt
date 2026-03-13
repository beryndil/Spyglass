package dev.spyglass.android.core.module

import android.content.Context
import dev.spyglass.android.data.sync.DataManifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.BuildConfig
import dev.spyglass.android.R
import dev.spyglass.android.core.FirebaseHelper
import dev.spyglass.android.core.ui.DEFAULT_THEME
import dev.spyglass.android.core.ui.FontScaleOptions
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.Red400
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.SpyglassDivider
import dev.spyglass.android.core.ui.SpyglassIcon
import dev.spyglass.android.core.ui.SpyglassIconImage
import dev.spyglass.android.core.ui.CustomWallpaper
import dev.spyglass.android.core.ui.TextureManager
import dev.spyglass.android.core.ui.ImageThemeOrder
import dev.spyglass.android.core.ui.SolidThemeOrder
import dev.spyglass.android.core.ui.ThemeInfoMap
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.core.shell.imageThemeDrawable
import dev.spyglass.android.core.ui.SupportedLanguages
import dev.spyglass.android.core.ui.rememberHapticConfirm
import dev.spyglass.android.home.TipsLoader
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit
import dev.spyglass.android.data.sync.DataSyncWorker

/**
 * Core module — always enabled. Owns theme, preferences, privacy/consent,
 * texture management, news, app lock, and informational screens.
 */
object CoreModule : SpyglassModule {

    override val id = "core"
    override val name = "Core"
    override val icon: SpyglassIcon = PixelIcons.Blocks
    override val priority = 0
    override val canDisable = false

    // ── Home sections ───────────────────────────────────────────────────────

    override fun homeSections(): List<HomeSection> = listOf(
        HomeSection("header", 0) { HeaderSection() },
        HomeSection("search", 5) { scope -> SearchBarSection(scope) },
        HomeSection("tip", 50) { TipOfDaySection() },
    )

    // ── Settings sections ───────────────────────────────────────────────────

    override fun settingsSections(): List<SettingsSection> = listOf(
        // Tab 0: Appearance
        SettingsSection("appearance", "Appearance", 0, tab = 0) { AppearanceContent() },
        // Tab 1: Gameplay
        SettingsSection("game_filters", "Game Filters", 0, tab = 1) { GameFiltersContent() },
        SettingsSection("default_tabs", "Default Tabs", 10, tab = 1) { DefaultTabsContent() },
        // Tab 2: General
        SettingsSection("app_behavior", "App Behavior", 0, tab = 2) { AppBehaviorContent() },
        SettingsSection("data_sync", "Data & Sync", 10, tab = 2) { DataSyncContent() },
        SettingsSection("favorites", "Favorites", 20, tab = 2) { FavoritesContent() },
        // Tab 3: About & Privacy
        SettingsSection("privacy_security", "Privacy & Security", 0, tab = 3) { PrivacySecurityContent() },
        SettingsSection("about", "About", 10, tab = 3) { scope -> AboutContent(scope) },
    )

    // ── Nav routes ──────────────────────────────────────────────────────────

    override fun navRoutes(): List<ModuleRoute> = listOf(
        ModuleRoute("about") { _, nav ->
            dev.spyglass.android.about.AboutScreen(
                onBack = { nav.navigateBack() },
                onLicense = { nav.navigateTo("license") },
                onDisclaimer = { nav.navigateTo("disclaimer") },
            )
        },
        ModuleRoute("license") { _, nav ->
            dev.spyglass.android.license.LicenseScreen(onBack = { nav.navigateBack() })
        },
        ModuleRoute("disclaimer") { _, nav ->
            dev.spyglass.android.disclaimer.DisclaimerScreen(onBack = { nav.navigateBack() })
        },
        ModuleRoute("changelog") { _, nav ->
            dev.spyglass.android.changelog.ChangelogScreen(onBack = { nav.navigateBack() })
        },
        ModuleRoute("feedback") { _, nav ->
            dev.spyglass.android.feedback.FeedbackScreen(onBack = { nav.navigateBack() })
        },
        ModuleRoute("help") { _, nav ->
            dev.spyglass.android.help.HelpScreen(onBack = { nav.navigateBack() })
        },
        ModuleRoute("news") { _, nav ->
            dev.spyglass.android.news.NewsScreen(onBack = { nav.navigateBack() })
        },
    )

    override fun bottomNavItems(): List<BottomNavItem> = emptyList()

    override fun searchProvider(): SearchProvider? = null

    // ── Home section composables ────────────────────────────────────────────

    @Composable
    private fun HeaderSection() {
        val hapticClick = rememberHapticClick()
        val updateAvailable by androidx.compose.runtime.produceState<Boolean?>(null) {
            value = kotlinx.coroutines.withContext(Dispatchers.IO) { checkForUpdate() }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpyglassIconImage(
                SpyglassIcon.Drawable(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(144.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.home_welcome),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (updateAvailable == true) {
                val uriHandler = LocalUriHandler.current
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.home_update_available),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.clickable {
                        hapticClick()
                        uriHandler.openUri("https://github.com/beryndil/Spyglass/releases/latest")
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

    @Composable
    private fun SearchBarSection(scope: HomeSectionScope) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clickable { scope.navigateToSearch() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            SpyglassIconImage(PixelIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.search), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }

    @Composable
    private fun TipOfDaySection() {
        val hapticClick = rememberHapticClick()
        val context = LocalContext.current
        val showTip by remember {
            context.dataStore.data.map { it[PreferenceKeys.SHOW_TIP_OF_DAY] ?: true }
        }.collectAsStateWithLifecycle(initialValue = true)

        if (!showTip) return

        val edition by remember {
            context.dataStore.data.map { it[PreferenceKeys.MINECRAFT_EDITION] ?: "java" }
        }.collectAsStateWithLifecycle(initialValue = "java")

        val tips by androidx.compose.runtime.produceState(emptyList<String>(), edition) {
            val allTips = TipsLoader.load(context)
            value = allTips.filter { it.edition == "both" || it.edition == edition }.map { it.text }
        }
        if (tips.isEmpty()) return
        val startIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % tips.size }
        var tipIndex by remember { mutableIntStateOf(startIndex) }
        var menuExpanded by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        ResultCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PriorityHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_did_you_know), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { hapticClick(); menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.home_tip_options), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_tip_disable)) },
                            onClick = {
                                hapticClick()
                                menuExpanded = false
                                scope.launch { context.dataStore.edit { it[PreferenceKeys.SHOW_TIP_OF_DAY] = false } }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                tips[tipIndex],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = { hapticClick(); tipIndex = Math.floorMod(tipIndex - 1, tips.size) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.home_tip_previous), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { hapticClick(); tipIndex = (tipIndex + 1) % tips.size },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.home_tip_next), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    // ── Settings section composables ────────────────────────────────────────

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun LanguageContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val hapticClick = rememberHapticClick()

        val appLanguage by remember {
            context.dataStore.data.map { it[PreferenceKeys.APP_LANGUAGE] ?: "system" }
        }.collectAsStateWithLifecycle(initialValue = "system")

        val translateGameData by remember {
            context.dataStore.data.map { it[PreferenceKeys.TRANSLATE_GAME_DATA] ?: true }
        }.collectAsStateWithLifecycle(initialValue = true)

        val showOriginalNames by remember {
            context.dataStore.data.map { it[PreferenceKeys.SHOW_ORIGINAL_NAMES] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        SectionHeader(stringResource(R.string.settings_language))
        ResultCard {
            Text(
                stringResource(R.string.settings_language_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SupportedLanguages.forEach { (code, label) ->
                    FilterChip(
                        selected = appLanguage == code,
                        onClick = {
                            hapticClick()
                            // Save preference first, then apply locale on Main thread.
                            // setApplicationLocales recreates the Activity, so the
                            // coroutine scope may be cancelled — use GlobalScope to
                            // ensure the locale switch completes.
                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                context.dataStore.edit { it[PreferenceKeys.APP_LANGUAGE] = code }
                                val localeList = if (code == "system") {
                                    androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                                } else {
                                    androidx.core.os.LocaleListCompat.forLanguageTags(code)
                                }
                                withContext(Dispatchers.Main) {
                                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
                                }
                            }
                        },
                        label = {
                            val displayLabel = if (code == "system") {
                                // Always show in the phone's system language, not the app's override
                                val ctx = LocalContext.current
                                remember {
                                    val sysConfig = android.content.res.Configuration(ctx.resources.configuration).apply {
                                        setLocale(java.util.Locale.getDefault())
                                    }
                                    ctx.createConfigurationContext(sysConfig).getString(R.string.settings_language_system_default)
                                }
                            } else label
                            Text(displayLabel, style = MaterialTheme.typography.labelSmall)
                        },
                    )
                }
            }
            if (appLanguage != "en" && appLanguage != "system") {
                SpyglassDivider()
                SettingsToggle(
                    title = stringResource(R.string.settings_translate_game_data),
                    description = stringResource(R.string.settings_translate_game_data_desc),
                    checked = translateGameData,
                    onCheckedChange = { newVal ->
                        scope.launch { context.dataStore.edit { it[PreferenceKeys.TRANSLATE_GAME_DATA] = newVal } }
                    },
                )
                SettingsToggle(
                    title = stringResource(R.string.settings_show_original_names),
                    description = stringResource(R.string.settings_show_original_names_desc),
                    checked = showOriginalNames,
                    onCheckedChange = { newVal ->
                        scope.launch { context.dataStore.edit { it[PreferenceKeys.SHOW_ORIGINAL_NAMES] = newVal } }
                    },
                )
            }
            SpyglassDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        }
                        context.startActivity(intent)
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.settings_language_system_override),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    // ── Tab 0: Appearance ──────────────────────────────────────────────────

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun AppearanceContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val hapticClick = rememberHapticClick()

        val backgroundTheme by remember {
            context.dataStore.data.map { it[PreferenceKeys.BACKGROUND_THEME] ?: DEFAULT_THEME }
        }.collectAsStateWithLifecycle(initialValue = DEFAULT_THEME)

        val photoPickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    CustomWallpaper.save(context, uri)
                    withContext(Dispatchers.Main) {
                        scope.launch { context.dataStore.edit { it[PreferenceKeys.BACKGROUND_THEME] = "custom" } }
                    }
                }
            }
        }

        val highContrast by remember {
            context.dataStore.data.map { it[PreferenceKeys.HIGH_CONTRAST] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        val hapticFeedback by remember {
            context.dataStore.data.map { it[PreferenceKeys.HAPTIC_FEEDBACK] ?: true }
        }.collectAsStateWithLifecycle(initialValue = true)

        val reduceAnimations by remember {
            context.dataStore.data.map { it[PreferenceKeys.REDUCE_ANIMATIONS] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        val fontScale by remember {
            context.dataStore.data.map { it[PreferenceKeys.FONT_SCALE] ?: 1 }
        }.collectAsStateWithLifecycle(initialValue = 1)

        SectionHeader(stringResource(R.string.settings_appearance))
        ResultCard {
            // Image background themes
            Text(
                stringResource(R.string.settings_background),
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
                                .clickable {
                                    hapticClick()
                                    scope.launch { context.dataStore.edit { it[PreferenceKeys.BACKGROUND_THEME] = key } }
                                },
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
                                        scope.launch { context.dataStore.edit { it[PreferenceKeys.BACKGROUND_THEME] = "custom" } }
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
                                    .clickable {
                                        hapticClick()
                                        scope.launch { context.dataStore.edit { it[PreferenceKeys.BACKGROUND_THEME] = key } }
                                    },
                            )
                        }
                    }
                }
            }
            Text(
                ThemeInfoMap[backgroundTheme]?.label ?: "Obsidian",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        ResultCard {
            SettingsToggle(
                title = stringResource(R.string.settings_high_contrast),
                description = stringResource(R.string.settings_high_contrast_desc),
                checked = highContrast,
                onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.HIGH_CONTRAST] = !highContrast } } },
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
                val fontScaleLabels = listOf(
                    stringResource(R.string.settings_font_scale_small),
                    stringResource(R.string.settings_font_scale_default),
                    stringResource(R.string.settings_font_scale_large),
                    stringResource(R.string.settings_font_scale_extra_large),
                )
                FontScaleOptions.forEachIndexed { i, _ ->
                    FilterChip(
                        selected = fontScale == i,
                        onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.FONT_SCALE] = i } } },
                        label = { Text(fontScaleLabels[i], style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            SpyglassDivider()

            SettingsToggle(
                title = stringResource(R.string.settings_haptic_feedback),
                description = stringResource(R.string.settings_haptic_feedback_desc),
                checked = hapticFeedback,
                onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.HAPTIC_FEEDBACK] = !hapticFeedback } } },
            )

            SpyglassDivider()

            SettingsToggle(
                title = stringResource(R.string.settings_reduce_animations),
                description = stringResource(R.string.settings_reduce_animations_desc),
                checked = reduceAnimations,
                onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.REDUCE_ANIMATIONS] = !reduceAnimations } } },
            )
        }
    }

    // ── Tab 1: Gameplay ─────────────────────────────────────────────────────

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun GameFiltersContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val hapticClick = rememberHapticClick()

        val minecraftEdition by remember {
            context.dataStore.data.map { it[PreferenceKeys.MINECRAFT_EDITION] ?: "java" }
        }.collectAsStateWithLifecycle(initialValue = "java")

        val minecraftVersion by remember {
            context.dataStore.data.map { it[PreferenceKeys.MINECRAFT_VERSION] ?: "" }
        }.collectAsStateWithLifecycle(initialValue = "")

        val versionFilterMode by remember {
            context.dataStore.data.map { it[PreferenceKeys.VERSION_FILTER_MODE] ?: "show_all" }
        }.collectAsStateWithLifecycle(initialValue = "show_all")

        val hideUnobtainable by remember {
            context.dataStore.data.map { it[PreferenceKeys.HIDE_UNOBTAINABLE_BLOCKS] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        val showExperimental by remember {
            context.dataStore.data.map { it[PreferenceKeys.SHOW_EXPERIMENTAL] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        val gameClockEnabled by remember {
            context.dataStore.data.map { it[PreferenceKeys.GAME_CLOCK_ENABLED] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        var versionExpanded by remember { mutableStateOf(false) }

        SectionHeader(stringResource(R.string.settings_game_settings))
        ResultCard {
            Text(
                stringResource(R.string.settings_game_filter_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            Text(stringResource(R.string.settings_edition), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            dev.spyglass.android.core.ui.TogglePill(
                options = listOf(stringResource(R.string.settings_edition_java), stringResource(R.string.settings_edition_bedrock)),
                selected = if (minecraftEdition == "bedrock") 1 else 0,
                onSelect = {
                    val edition = if (it == 1) "bedrock" else "java"
                    scope.launch { context.dataStore.edit { prefs -> prefs[PreferenceKeys.MINECRAFT_EDITION] = edition } }
                },
            )

            val versions = if (minecraftEdition == "bedrock") dev.spyglass.android.settings.MinecraftVersions.BEDROCK_VERSIONS else dev.spyglass.android.settings.MinecraftVersions.JAVA_VERSIONS
            val latestLabel = stringResource(R.string.settings_version_latest)
            val displayVersion = minecraftVersion.ifBlank { latestLabel }
            Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Box {
                androidx.compose.material3.OutlinedButton(onClick = { hapticClick(); versionExpanded = true }) {
                    Text(displayVersion, color = MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(expanded = versionExpanded, onDismissRequest = { versionExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(latestLabel, color = if (minecraftVersion.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                        onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.MINECRAFT_VERSION] = "" } }; versionExpanded = false },
                    )
                    versions.reversed().forEach { v ->
                        DropdownMenuItem(
                            text = { Text(v, color = if (minecraftVersion == v) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                            onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.MINECRAFT_VERSION] = v } }; versionExpanded = false },
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
                        onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.VERSION_FILTER_MODE] = key } } },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            SpyglassDivider()

            SettingsToggle(
                title = stringResource(R.string.settings_hide_unobtainable),
                description = stringResource(R.string.settings_hide_unobtainable_desc),
                checked = hideUnobtainable,
                onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.HIDE_UNOBTAINABLE_BLOCKS] = !hideUnobtainable } } },
            )

            SpyglassDivider()

            SettingsToggle(
                title = stringResource(R.string.settings_show_experimental),
                description = stringResource(R.string.settings_show_experimental_desc),
                checked = showExperimental,
                onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.SHOW_EXPERIMENTAL] = !showExperimental } } },
            )

            SpyglassDivider()

            SettingsToggle(
                title = stringResource(R.string.settings_game_clock),
                description = stringResource(R.string.settings_game_clock_desc),
                checked = gameClockEnabled,
                onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.GAME_CLOCK_ENABLED] = !gameClockEnabled } } },
            )
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun DefaultTabsContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val hapticClick = rememberHapticClick()

        val defaultBrowseTab by remember {
            context.dataStore.data.map { it[PreferenceKeys.DEFAULT_BROWSE_TAB] ?: 0 }
        }.collectAsStateWithLifecycle(initialValue = 0)

        val defaultToolTab by remember {
            context.dataStore.data.map { it[PreferenceKeys.DEFAULT_TOOL_TAB] ?: 0 }
        }.collectAsStateWithLifecycle(initialValue = 0)

        SectionHeader(stringResource(R.string.settings_defaults))
        ResultCard {
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
                val browseNames = listOf(
                    stringResource(R.string.browse_tab_blocks), stringResource(R.string.browse_tab_items),
                    stringResource(R.string.browse_tab_recipes), stringResource(R.string.browse_tab_mobs),
                    stringResource(R.string.browse_tab_trades), stringResource(R.string.browse_tab_biomes),
                    stringResource(R.string.browse_tab_structures), stringResource(R.string.browse_tab_enchants),
                    stringResource(R.string.browse_tab_potions), stringResource(R.string.browse_tab_advancements),
                    stringResource(R.string.browse_tab_commands), stringResource(R.string.browse_tab_reference),
                    stringResource(R.string.browse_tab_versions),
                )
                browseNames.forEachIndexed { i, name ->
                    FilterChip(
                        selected = defaultBrowseTab == i,
                        onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.DEFAULT_BROWSE_TAB] = i } } },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            SpyglassDivider()

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
                val toolNames = listOf(
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
                toolNames.forEachIndexed { i, name ->
                    FilterChip(
                        selected = defaultToolTab == i,
                        onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.DEFAULT_TOOL_TAB] = i } } },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
    }

    // ── Tab 2: General ──────────────────────────────────────────────────────

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun AppBehaviorContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val hapticClick = rememberHapticClick()

        val defaultStartupTab by remember {
            context.dataStore.data.map { it[PreferenceKeys.DEFAULT_STARTUP_TAB] ?: 0 }
        }.collectAsStateWithLifecycle(initialValue = 0)

        val showTipOfDay by remember {
            context.dataStore.data.map { it[PreferenceKeys.SHOW_TIP_OF_DAY] ?: true }
        }.collectAsStateWithLifecycle(initialValue = true)

        val showFavoritesOnHome by remember {
            context.dataStore.data.map { it[PreferenceKeys.SHOW_FAVORITES_ON_HOME] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        SectionHeader(stringResource(R.string.settings_defaults))
        ResultCard {
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
                        onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.DEFAULT_STARTUP_TAB] = i } } },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            SpyglassDivider()

            SettingsToggle(
                title = stringResource(R.string.settings_tip_of_day),
                description = stringResource(R.string.settings_tip_of_day_desc),
                checked = showTipOfDay,
                onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.SHOW_TIP_OF_DAY] = !showTipOfDay } } },
            )

            SpyglassDivider()

            SettingsToggle(
                title = stringResource(R.string.settings_favorites_on_home),
                description = stringResource(R.string.settings_favorites_on_home_desc),
                checked = showFavoritesOnHome,
                onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.SHOW_FAVORITES_ON_HOME] = !showFavoritesOnHome } } },
            )
        }
    }

    @Composable
    private fun FavoritesContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val hapticConfirm = rememberHapticConfirm()

        val repo by androidx.compose.runtime.produceState<dev.spyglass.android.data.repository.GameDataRepository?>(null) {
            value = withContext(Dispatchers.IO) { dev.spyglass.android.data.repository.GameDataRepository.get(context) }
        }
        val allFavorites by remember(repo) {
            repo?.allFavorites() ?: kotlinx.coroutines.flow.flowOf(emptyList())
        }.collectAsStateWithLifecycle(initialValue = emptyList())

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
                TextButton(onClick = {
                    hapticConfirm()
                    scope.launch(Dispatchers.IO) { repo?.deleteAllFavorites() }
                }) {
                    Text(stringResource(R.string.settings_clear_all_favorites), color = Red400)
                }
            }
        }
    }

    // ── Tab 2: Data & Sync (unchanged) ──────────────────────────────────────
    // ── Tab 3: Privacy & Security, About (unchanged) ────────────────────────

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun DataSyncContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val hapticClick = rememberHapticClick()
        val hapticConfirm = rememberHapticConfirm()

        val offlineMode by remember {
            context.dataStore.data.map { it[PreferenceKeys.OFFLINE_MODE] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        val syncFrequencyHours by remember {
            context.dataStore.data.map { it[PreferenceKeys.SYNC_FREQUENCY_HOURS] ?: 12 }
        }.collectAsStateWithLifecycle(initialValue = 12)

        val textureState by TextureManager.state.collectAsStateWithLifecycle()
        var storageBytes by remember {
            mutableStateOf(
                File(context.filesDir, "textures").let { dir ->
                    if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
                }
            )
        }

        var syncing by remember { mutableStateOf(false) }

        SectionHeader(stringResource(R.string.settings_data_sync))
        ResultCard {
            // Sync Now
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_sync_now), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.settings_sync_check_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                if (syncing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    TextButton(
                        onClick = {
                            hapticClick()
                            if (!offlineMode) {
                                syncing = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        dev.spyglass.android.data.sync.DataSyncManager.sync(context)
                                    } finally {
                                        syncing = false
                                    }
                                }
                            }
                        },
                        enabled = !offlineMode,
                    ) {
                        Text(stringResource(R.string.settings_sync_btn), color = if (offlineMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary)
                    }
                }
            }

            SpyglassDivider()

            SettingsToggle(
                title = stringResource(R.string.settings_offline_mode),
                description = stringResource(R.string.settings_offline_mode_desc),
                checked = offlineMode,
                onCheckedChange = {
                    scope.launch {
                        val newValue = !offlineMode
                        context.dataStore.edit { it[PreferenceKeys.OFFLINE_MODE] = newValue }
                        if (newValue) DataSyncWorker.cancel(context)
                        else DataSyncWorker.enqueue(context, syncFrequencyHours)
                    }
                },
            )

            SpyglassDivider()

            Text(
                stringResource(R.string.settings_sync_frequency),
                style = MaterialTheme.typography.bodyLarge,
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
                        onClick = {
                            hapticClick()
                            if (!offlineMode) scope.launch {
                                context.dataStore.edit { it[PreferenceKeys.SYNC_FREQUENCY_HOURS] = hours }
                                DataSyncWorker.enqueue(context, hours)
                            }
                        },
                        enabled = !offlineMode,
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            SpyglassDivider()

            Text(
                stringResource(R.string.settings_storage_usage),
                style = MaterialTheme.typography.bodyLarge,
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
                    scope.launch { TextureManager.delete(context) }
                    storageBytes = 0L
                }) {
                    Text(stringResource(R.string.settings_clear_cache), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    @Composable
    private fun PrivacySecurityContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current
        val hapticClick = rememberHapticClick()
        val hapticConfirm = rememberHapticConfirm()
        var showDeleteConfirm by remember { mutableStateOf(false) }

        val analyticsConsent by remember {
            context.dataStore.data.map { it[PreferenceKeys.ANALYTICS_CONSENT] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        val crashConsent by remember {
            context.dataStore.data.map { it[PreferenceKeys.CRASH_CONSENT] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        val adPersonalizationConsent by remember {
            context.dataStore.data.map { it[PreferenceKeys.AD_PERSONALIZATION_CONSENT] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        val appLockEnabled by remember {
            context.dataStore.data.map { it[PreferenceKeys.APP_LOCK_ENABLED] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        SectionHeader(stringResource(R.string.settings_privacy_security))
        ResultCard {
            SettingsToggle(
                title = stringResource(R.string.settings_app_lock),
                description = stringResource(R.string.settings_app_lock_desc),
                checked = appLockEnabled,
                onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.APP_LOCK_ENABLED] = !appLockEnabled } } },
            )
            SpyglassDivider()
            SettingsToggle(
                title = stringResource(R.string.consent_analytics),
                description = stringResource(R.string.consent_analytics_desc),
                checked = analyticsConsent,
                onCheckedChange = {
                    scope.launch {
                        val newVal = !analyticsConsent
                        context.dataStore.edit { it[PreferenceKeys.ANALYTICS_CONSENT] = newVal }
                        FirebaseHelper.applyConsent(crashConsent, newVal)
                    }
                },
            )
            SpyglassDivider()
            SettingsToggle(
                title = stringResource(R.string.consent_crash_reports),
                description = stringResource(R.string.consent_crash_reports_desc),
                checked = crashConsent,
                onCheckedChange = {
                    scope.launch {
                        val newVal = !crashConsent
                        context.dataStore.edit { it[PreferenceKeys.CRASH_CONSENT] = newVal }
                        FirebaseHelper.applyConsent(newVal, analyticsConsent)
                    }
                },
            )
            SpyglassDivider()
            SettingsToggle(
                title = stringResource(R.string.consent_personalized_ads),
                description = stringResource(R.string.consent_personalized_ads_desc),
                checked = adPersonalizationConsent,
                onCheckedChange = {
                    scope.launch {
                        context.dataStore.edit { it[PreferenceKeys.AD_PERSONALIZATION_CONSENT] = !adPersonalizationConsent }
                    }
                },
            )
            SpyglassDivider()
            TextButton(onClick = { hapticConfirm(); showDeleteConfirm = true }) {
                Text(stringResource(R.string.settings_delete_data), color = Red400)
            }
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
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(stringResource(R.string.settings_delete_data_title), color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Text(stringResource(R.string.settings_delete_data_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                confirmButton = {
                    TextButton(onClick = {
                        hapticConfirm()
                        scope.launch {
                            dev.spyglass.android.data.repository.GameDataRepository.get(context).deleteAllUserData()
                        }
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

    @Composable
    private fun AboutContent(scope: SettingsSectionScope) {
        val uriHandler = LocalUriHandler.current
        val context = LocalContext.current

        SectionHeader(stringResource(R.string.settings_about_section))
        ResultCard {
            Text(
                stringResource(R.string.settings_version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            SpyglassDivider()
            SettingsLink(
                title = stringResource(R.string.settings_rate_app),
                description = stringResource(R.string.settings_rate_app_desc),
                onClick = { uriHandler.openUri("https://play.google.com/store/apps/details?id=dev.spyglass.android") },
            )
            SpyglassDivider()
            SettingsLink(
                title = stringResource(R.string.settings_send_feedback),
                description = stringResource(R.string.settings_send_feedback_desc),
                onClick = { scope.navigateTo("feedback") },
            )
            SpyglassDivider()
            SettingsLink(
                title = stringResource(R.string.settings_changelog),
                description = stringResource(R.string.settings_changelog_desc),
                onClick = { scope.navigateTo("changelog") },
            )
            SpyglassDivider()
            SettingsLink(
                title = stringResource(R.string.settings_about),
                description = stringResource(R.string.settings_about_desc),
                onClick = { scope.navigateTo("about") },
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

    // ── Reusable composable helpers ─────────────────────────────────────────

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

    // ── Utilities ───────────────────────────────────────────────────────────

    private fun checkForUpdate(): Boolean? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("https://data.hardknocks.university/manifest.json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val manifest = DataManifest.fromJson(body)
                val latestApp = manifest.latestApp
                if (latestApp.isEmpty()) return null
                // latestApp is e.g. "FireHorse.0307.2049" — compare using zodiac version ordering
                // Strip trailing "-a" suffix if present for comparison
                val currentVersion = BuildConfig.VERSION_NAME.removeSuffix("-a")
                DataManifest.compareVersions(latestApp, currentVersion) > 0
            }
        } catch (e: Exception) {
            Timber.d(e, "Update check failed")
            null
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
