package dev.spyglass.android.core.module

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.CustomWallpaper
import dev.spyglass.android.core.ui.DEFAULT_THEME
import dev.spyglass.android.core.ui.FontScaleOptions
import dev.spyglass.android.core.ui.ImageThemeOrder
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.SolidThemeOrder
import dev.spyglass.android.core.ui.SpyglassDivider
import dev.spyglass.android.core.ui.ThemeInfoMap
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.core.shell.imageThemeDrawable
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceContent() {
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

