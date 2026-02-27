package dev.spyglass.android.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.spyglass.android.data.ItemTags
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.serialization.json.*

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, icon: SpyglassIcon? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        if (icon != null) {
            SpyglassIconImage(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text  = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Gold,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(color = Stone700, thickness = 0.5.dp, modifier = Modifier.weight(1f))
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
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Stone500,
            modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(12.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge,  color = Stone100)
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
        placeholder   = if (placeholder.isNotEmpty()) ({ Text(placeholder, color = Stone500) }) else null,
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Gold,
            unfocusedBorderColor = Stone700,
            focusedLabelColor    = Gold,
            unfocusedLabelColor  = Stone500,
            cursorColor          = Gold,
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
            .background(SurfaceDark, RoundedCornerShape(8.dp))
            .border(1.dp, Stone700, RoundedCornerShape(8.dp))
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
    Row(
        modifier = modifier
            .background(SurfaceMid, RoundedCornerShape(6.dp))
            .border(1.dp, Stone700, RoundedCornerShape(6.dp))
            .padding(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) Gold else Color.Transparent,
                        RoundedCornerShape(4.dp),
                    )
                    .height(32.dp),
            ) {
                TextButton(
                    onClick    = { onSelect(i) },
                    modifier   = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Background else Stone300,
                    )
                }
            }
        }
    }
}

// ── Divider ───────────────────────────────────────────────────────────────────

@Composable
fun SpyglassDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = Stone700, thickness = 0.5.dp)
}

// ── Input card — wraps calculator inputs to break up the black void ──────────

@Composable
fun InputCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .border(1.dp, Stone700, RoundedCornerShape(12.dp))
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
    leadingIconTint: Color = Stone300,
    supportingMaxLines: Int = 1,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(10.dp))
            .border(1.dp, Stone700, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            SpyglassIconImage(leadingIcon, contentDescription = null, tint = leadingIconTint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(headline, style = MaterialTheme.typography.bodyLarge, color = Stone100,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (supporting.isNotEmpty()) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = Stone500,
                    maxLines = supportingMaxLines, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
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
        SpyglassIconImage(icon, contentDescription = null, tint = Stone700, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Stone300)
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Stone500)
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
    iconTint: Color = Gold,
    iconSize: Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpyglassIconImage(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Stone100)
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = Stone500,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (stat.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(stat, style = MaterialTheme.typography.labelSmall, color = Gold)
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

// ── Minecraft ID row — tap to copy ──────────────────────────────────────────

@Composable
fun MinecraftIdRow(id: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fullId = if (id.contains(':')) id else "minecraft:$id"
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Minecraft ID", fullId))
                copied = true
            }
            .background(Stone700.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            fullId,
            style = MaterialTheme.typography.bodySmall,
            color = Stone300,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        )
        Text(
            if (copied) "Copied!" else "Tap to copy",
            style = MaterialTheme.typography.labelSmall,
            color = if (copied) Gold else Stone500,
        )
    }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
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
                                    .background(SurfaceMid, RoundedCornerShape(2.dp))
                                    .border(0.5.dp, Stone700, RoundedCornerShape(2.dp))
                                    .clickable { onItemTap(cell) },
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
                                        color = Stone300,
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
                                .background(Background, RoundedCornerShape(2.dp))
                                .border(0.5.dp, Stone700, RoundedCornerShape(2.dp)),
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
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding      = 12.dp,
        containerColor   = MaterialTheme.colorScheme.surface,
        contentColor     = Gold,
        divider          = {},
        modifier         = modifier,
    ) {
        tabs.forEachIndexed { i, tab ->
            val selected = selectedIndex == i
            Tab(
                selected = selected,
                onClick  = { onSelect(i) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SpyglassIconImage(
                            tab.icon, contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (tab.untinted) Color.Unspecified else if (selected) Gold else Stone500,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Gold else Stone500,
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

    Crossfade(
        targetState = index,
        animationSpec = tween(400),
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
