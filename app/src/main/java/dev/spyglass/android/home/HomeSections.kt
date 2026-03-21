package dev.spyglass.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.TodoEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.Dispatchers

// ── Browse tab index for favorite types ─────────────────────────────────────

internal fun browseTabForType(type: String): Int = when (type) {
    "block"     -> 0
    "item"      -> 1
    "recipe"    -> 2
    "mob"       -> 3
    "trade"     -> 4
    "biome"     -> 5
    "structure" -> 6
    "enchant"   -> 7
    "potion"      -> 8
    "command"     -> 9
    else          -> 0
}

private fun iconForFavorite(type: String, id: String): SpyglassIcon = when (type) {
    "block"     -> ItemTextures.get(id) ?: PixelIcons.Blocks
    "item"      -> ItemTextures.get(id) ?: PixelIcons.Item
    "recipe"    -> ItemTextures.get(id) ?: PixelIcons.Crafting
    "mob"       -> MobTextures.get(id) ?: PixelIcons.Mob
    "trade"     -> PixelIcons.Trade
    "biome"     -> BiomeTextures.get(id) ?: PixelIcons.Biome
    "structure" -> StructureTextures.get(id) ?: PixelIcons.Structure
    "enchant"   -> EnchantTextures.get(id) ?: PixelIcons.Enchant
    "potion"    -> PotionTextures.get(id) ?: PixelIcons.Potion
    else        -> PixelIcons.Bookmark
}

// ── Update check ────────────────────────────────────────────────────────────

/** Delegates to [CoreModule.checkForUpdate] for consistent manifest-based checking. */
internal fun checkForUpdate(): Boolean? =
    dev.spyglass.android.core.module.CoreModule.checkForUpdate()

// ── Extracted section composables ───────────────────────────────────────────

@Composable
internal fun HomeHeader() {
    val updateAvailable by produceState<Boolean?>(null) {
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
internal fun HomeSearchBar(onSearch: () -> Unit) {
    val hapticClick = rememberHapticClick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { hapticClick(); onSearch() }
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
internal fun HomeTodoSection(
    todoCount: Int,
    todoPreview: List<TodoEntity>,
    onCalcTab: (Int) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
) {
    SectionHeader(stringResource(R.string.home_todo), icon = PixelIcons.Todo)
    Spacer(Modifier.height(8.dp))
    if (todoCount > 0) {
        ResultCard {
            todoPreview.forEach { todo ->
                HomeTodoRow(
                    todo = todo,
                    onToggle = { onToggle(todo.id, !todo.completed) },
                )
            }
            if (todoCount > 3) {
                SpyglassDivider()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCalcTab(0) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (todoCount > 3) stringResource(R.string.home_todo_view_all, todoCount) else stringResource(R.string.home_todo_edit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            }
        }
    } else {
        ResultCard(
            modifier = Modifier.clickable { onCalcTab(0) },
        ) {
            Text(
                stringResource(R.string.home_todo_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.home_todo_open),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun HomeFavoritesSection(
    favorites: List<FavoriteEntity>,
    onBrowseTab: (Int) -> Unit,
) {
    SectionHeader(stringResource(R.string.favorites), icon = PixelIcons.Bookmark)
    Spacer(Modifier.height(8.dp))
    favorites.forEach { fav ->
        BrowseListItem(
            headline = fav.displayName,
            supporting = fav.type,
            leadingIcon = iconForFavorite(fav.type, fav.id),
            modifier = Modifier.clickable {
                onBrowseTab(browseTabForType(fav.type))
            },
        )
    }
}

@Composable
internal fun HomeTipSection(tips: List<Tip>, startIndex: Int, repo: GameDataRepository?, onDisable: () -> Unit) {
    var tipIndex by remember { mutableIntStateOf(startIndex) }
    var menuExpanded by remember { mutableStateOf(false) }
    val currentTip = tips[tipIndex]
    val tipText = if (repo != null) {
        translatedText(repo, "tip", currentTip.id.toString(), "text", currentTip.text)
    } else currentTip.text

    ResultCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PriorityHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_did_you_know), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.home_tip_options), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_tip_disable)) },
                        onClick = {
                            menuExpanded = false
                            onDisable()
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            tipText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { tipIndex = Math.floorMod(tipIndex - 1, tips.size) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.home_tip_previous), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = { tipIndex = (tipIndex + 1) % tips.size },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.home_tip_next), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun HomeBrowseSection(onBrowseTab: (Int) -> Unit) {
    SectionHeader(stringResource(R.string.home_browse), icon = PixelIcons.Browse)
    Spacer(Modifier.height(8.dp))
    browseLinks().let { links ->
        QuickLinkGrid(links.map { it.first }) { index -> onBrowseTab(links[index].second) }
    }
}

// ── Home todo row (interactive) ─────────────────────────────────────────────

@Composable
private fun HomeTodoRow(todo: TodoEntity, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
    ) {
        Checkbox(
            checked = todo.completed,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.secondary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        if (todo.itemId != null) {
            val tex = ItemTextures.get(todo.itemId)
            if (tex != null) {
                SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
        }
        Text(
            todo.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (todo.completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
