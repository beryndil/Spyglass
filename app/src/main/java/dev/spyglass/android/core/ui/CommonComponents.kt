package dev.spyglass.android.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.spyglass.android.core.VersionFilterState
import dev.spyglass.android.core.checkMechanicsChanged
import dev.spyglass.android.data.ItemTags
import dev.spyglass.android.data.db.entities.VersionTagEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.spyglass.android.data.db.entities.RecipeEntity
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.serialization.json.*
import java.net.URLEncoder

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, icon: SpyglassIcon? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        if (icon != null) {
            SpyglassIconImage(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text  = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp, modifier = Modifier.weight(1f))
    }
}

// ── Stat row — label / value pair ────────────────────────────────────────────

@Composable
fun StatRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier            = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(12.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge,  color = MaterialTheme.colorScheme.onSurface)
    }
}

// ── Number input field ────────────────────────────────────────────────────────

@Composable
fun SpyglassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Number,
    imeAction: ImeAction = ImeAction.Done,
    onDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        placeholder   = if (placeholder.isNotEmpty()) ({ Text(placeholder, color = MaterialTheme.colorScheme.secondary) }) else null,
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor    = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor  = MaterialTheme.colorScheme.secondary,
            cursorColor          = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

// ── Result card ───────────────────────────────────────────────────────────────

@Composable
fun ResultCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

// ── Segmented toggle (2 options) ─────────────────────────────────────────────

@Composable
fun TogglePill(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticClick = rememberHapticClick()
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            .padding(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(4.dp),
                    )
                    .height(32.dp),
            ) {
                TextButton(
                    onClick    = { hapticClick(); onSelect(i) },
                    modifier   = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Divider ───────────────────────────────────────────────────────────────────

@Composable
fun SpyglassDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
}

// ── Input card — wraps calculator inputs to break up the black void ──────────

@Composable
fun InputCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LocalSurfaceCard.current, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

// ── Browse list item — card-style row with leading icon ──────────────────────

@Composable
fun BrowseListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String = "",
    leadingIcon: SpyglassIcon? = null,
    leadingIconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    supportingMaxLines: Int = 1,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LocalSurfaceCard.current, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            SpyglassIconImage(leadingIcon, contentDescription = null, tint = leadingIconTint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(headline, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (supporting.isNotEmpty()) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                    maxLines = supportingMaxLines, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.widthIn(max = 140.dp)) {
                trailing()
            }
        }
    }
}

// ── Empty state — large icon + title + subtitle ─────────────────────────────

@Composable
fun EmptyState(
    icon: SpyglassIcon,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpyglassIconImage(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

// ── Tab intro header — icon + headline + description + optional stat ────────

@Composable
fun TabIntroHeader(
    icon: SpyglassIcon,
    title: String,
    description: String,
    stat: String = "",
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconSize: Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpyglassIconImage(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (stat.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(stat, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ── Category badge — small colored pill ─────────────────────────────────────

@Composable
fun CategoryBadge(label: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text  = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

// ── Version badge — shows "1.17" for items added in later versions ───────────

@Composable
fun VersionBadge(version: String, modifier: Modifier = Modifier) {
    Text(
        text  = version,
        style = MaterialTheme.typography.labelSmall,
        color = PotionBlue,
        modifier = modifier
            .background(PotionBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

// ── Minecraft ID row — tap to copy ──────────────────────────────────────────

@Composable
fun MinecraftIdRow(id: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fullId = if (id.contains(':')) id else "minecraft:$id"
    var copied by remember { mutableStateOf(false) }
    val hapticConfirm = rememberHapticConfirm()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager ?: return@clickable
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Minecraft ID", fullId))
                hapticConfirm()
                copied = true
            }
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            fullId,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        )
        Text(
            if (copied) stringResource(R.string.core_copied) else stringResource(R.string.core_tap_to_copy),
            style = MaterialTheme.typography.labelSmall,
            color = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        )
    }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }
}

// ── Report a problem — opens pre-filled GitHub issue ─────────────────────────

@Composable
fun ReportProblemRow(
    entityType: String,
    entityName: String,
    entityId: String,
    textColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val uriHandler = LocalUriHandler.current
    val title = URLEncoder.encode("[$entityType] $entityName — Data Issue", "UTF-8")
    val body = URLEncoder.encode(
        "**$entityType:** $entityName\n**ID:** `$entityId`\n\n" +
        "### What's wrong?\n\n\n### What should it be?\n\n",
        "UTF-8"
    )
    val url = "https://github.com/beryndil/Spyglass/issues/new?title=$title&body=$body&labels=data-issue"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(16.dp), tint = textColor)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.core_report_problem), style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

// ── Tab row — reusable icon+text tabs for Calculators and Browse ────────────

// ── Texture-based crafting grid ──────────────────────────────────────────────

private fun formatCellName(id: String): String =
    id.replace('_', ' ').replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TextureCraftingGrid(
    recipe: RecipeEntity,
    onItemTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticClick = rememberHapticClick()
    // Parse the original grid from the recipe
    val parsed = runCatching {
        Json.parseToJsonElement(recipe.ingredientsJson).jsonArray
    }.getOrElse { return }

    val origRows = parsed.size
    val origCols = runCatching {
        val first = parsed.firstOrNull()
        if (first is JsonArray) first.size else 1
    }.getOrDefault(1)

    // Build a flat list from the original grid
    val origCells = parsed.flatMap { row ->
        if (row is JsonArray) row.map { it.jsonPrimitive.contentOrNull }
        else listOf(row.jsonPrimitive.contentOrNull)
    }

    // For crafting_shaped, always render as 3×3 grid (pad top-left)
    val isShaped = recipe.type.contains("shaped") && !recipe.type.contains("shapeless")
    val gridRows = if (isShaped) 3 else origRows
    val gridCols = if (isShaped) 3 else origCols

    // Build the padded 3×3 grid
    val cells = if (isShaped) {
        (0 until 9).map { idx ->
            val r = idx / 3
            val c = idx % 3
            if (r < origRows && c < origCols) origCells.getOrNull(r * origCols + c)
            else null
        }
    } else {
        origCells
    }

    Column(modifier = modifier) {
        for (row in 0 until gridRows) {
            Row {
                for (col in 0 until gridCols) {
                    val cell = cells.getOrNull(row * gridCols + col)
                    val texture = if (!cell.isNullOrBlank()) ItemTextures.get(cell) else null

                    if (!cell.isNullOrBlank()) {
                        val tag = ItemTags.tagForIngredient(cell, recipe.outputItem)
                        val tooltipState = remember { TooltipState() }
                        val displayName = if (tag != null) formatTagName(tag) else formatCellName(cell)
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip { Text(displayName) }
                            },
                            state = tooltipState,
                            enableUserInput = true,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                                    .clickable { hapticClick(); onItemTap(cell) },
                            ) {
                                if (tag != null) {
                                    RotatingTagIcon(tag, modifier = Modifier.size(22.dp))
                                } else if (texture != null) {
                                    SpyglassIconImage(
                                        texture, contentDescription = cell,
                                        modifier = Modifier.size(22.dp),
                                    )
                                } else {
                                    Text(
                                        cell.take(2).uppercase(),
                                        fontSize = 7.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty cell — no tooltip needed
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(2.dp))
                                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
                        ) {}
                    }
                }
            }
        }
    }
}

// ── Tab row — reusable icon+text tabs for Calculators and Browse ────────────

data class SpyglassTab(val label: String, val icon: SpyglassIcon, val untinted: Boolean = false)

@Composable
fun SpyglassTabRow(
    tabs: List<SpyglassTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticClick = rememberHapticClick()
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding      = 12.dp,
        containerColor   = MaterialTheme.colorScheme.surface,
        contentColor     = MaterialTheme.colorScheme.primary,
        divider          = {},
        modifier         = modifier,
    ) {
        tabs.forEachIndexed { i, tab ->
            val selected = selectedIndex == i
            Tab(
                selected = selected,
                onClick  = { hapticClick(); onSelect(i) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SpyglassIconImage(
                            tab.icon, contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (tab.untinted) Color.Unspecified else if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        )
                    }
                },
            )
        }
    }
}

// ── Rotating tag icon — cycles through all item textures in a tag ────────────

@Composable
fun RotatingTagIcon(
    tagId: String,
    modifier: Modifier = Modifier,
    intervalMs: Long = 1200,
) {
    val members = remember(tagId) { ItemTags.membersOfTag(tagId) }
    if (members.isEmpty()) return

    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(tagId) {
        while (true) {
            delay(intervalMs)
            index = (index + 1) % members.size
        }
    }

    val reduceMotion = LocalReduceAnimations.current
    Crossfade(
        targetState = index,
        animationSpec = if (reduceMotion) snap() else tween(400),
        modifier = modifier,
        label = "tag_icon",
    ) { i ->
        val tex = ItemTextures.get(members[i])
        if (tex != null) {
            SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}

fun formatTagName(tagId: String): String =
    "Any " + tagId.removePrefix("#").split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

// ── Sort button ──────────────────────────────────────────────────────────────

data class SortOption(val label: String, val key: String)

// ── Version card — themed card showing Minecraft update info + changelog ─────

@Composable
fun VersionCard(
    version: String,
    name: String,
    releaseDate: String,
    accentColor: Color,
    icon: SpyglassIcon,
    changelog: List<String>,
    modifier: Modifier = Modifier,
) {
    val hapticClick = rememberHapticClick()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { hapticClick(); expanded = !expanded },
    ) {
        // Header banner with accent color
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    accentColor.copy(alpha = 0.15f),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                )
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SpyglassIconImage(
                    icon, contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Minecraft $version",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = accentColor,
                    )
                }
                Text(
                    releaseDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // Changelog section
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val visibleItems = if (expanded) changelog else changelog.take(4)
            visibleItems.forEach { entry ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "\u2022",
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        modifier = Modifier.padding(end = 8.dp, top = 1.dp),
                    )
                    Text(
                        entry,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (changelog.size > 4) {
                Text(
                    if (expanded) stringResource(R.string.core_show_less) else stringResource(R.string.core_show_more, changelog.size - 4),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun SortButton(
    options: List<SortOption>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticClick = rememberHapticClick()
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { hapticClick(); expanded = true }) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.core_sort),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                val isSelected = option.key == selectedKey
                DropdownMenuItem(
                    text = {
                        Text(
                            option.label,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { hapticClick(); onSelect(option.key); expanded = false },
                    trailingIcon = if (isSelected) { {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    } } else null,
                )
            }
        }
    }
}

// ── Version & Edition section for detail cards ────────────────────────────────

private val AmberWarning = Color(0xFFFF8F00)

@Composable
fun VersionEditionSection(tag: VersionTagEntity, filter: VersionFilterState) {
    val isJava = filter.edition == "java"
    val addedJava = tag.addedInJava.ifBlank { "1.0" }
    val addedBedrock = tag.addedInBedrock.ifBlank { null }

    // Edition badge
    val editionLabel = when {
        tag.javaOnly -> stringResource(R.string.core_java_only)
        tag.bedrockOnly -> stringResource(R.string.core_bedrock_only)
        else -> stringResource(R.string.core_both_editions)
    }
    val editionColor = when {
        tag.javaOnly -> PotionBlue
        tag.bedrockOnly -> Emerald
        else -> Color(0xFF78909C)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Java $addedJava", style = MaterialTheme.typography.bodySmall, color = PotionBlue)
                if (addedBedrock != null && !tag.javaOnly) {
                    Text("Bedrock $addedBedrock", style = MaterialTheme.typography.bodySmall, color = Emerald)
                }
            }
        }
        Text(
            editionLabel,
            style = MaterialTheme.typography.labelSmall,
            color = editionColor,
            modifier = Modifier
                .background(editionColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }

    // Mechanics change warning (only when version filter is active)
    val mechInfo = checkMechanicsChanged(tag, filter)
    if (mechInfo != null) {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmberWarning.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .border(0.5.dp, AmberWarning.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    stringResource(R.string.core_mechanics_changed, mechInfo.version),
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberWarning,
                )
                Text(
                    mechInfo.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
