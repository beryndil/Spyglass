package dev.spyglass.android.core.ui

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

private const val LINK_TAG = "entity_link"

@Composable
fun LinkedDescription(
    description: String,
    linkIndex: EntityLinkIndex,
    selfId: String? = null,
    onItemTap: (String) -> Unit = {},
    onMobTap: (String) -> Unit = {},
    onBiomeTap: (String) -> Unit = {},
    onStructureTap: (String) -> Unit = {},
    onEnchantTap: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary  // gold / GoldOnLight depending on theme
    val baseStyle = MaterialTheme.typography.bodyMedium.copy(color = baseColor)

    val matches = remember(description, selfId) { linkIndex.findMatches(description, selfId) }

    val annotatedString = remember(description, matches, linkColor) {
        buildAnnotatedString {
            var cursor = 0
            for (match in matches) {
                // Append text before this match
                if (cursor < match.start) {
                    append(description.substring(cursor, match.start))
                }
                // Annotate the matched entity name
                val linkValue = "${match.link.type.name}:${match.link.id}"
                pushStringAnnotation(tag = LINK_TAG, annotation = linkValue)
                withStyle(SpanStyle(color = linkColor)) {
                    append(description.substring(match.start, match.end))
                }
                pop()
                cursor = match.end
            }
            // Remaining text after last match
            if (cursor < description.length) {
                append(description.substring(cursor))
            }
        }
    }

    if (matches.isEmpty()) {
        // No links — render as plain text to avoid ClickableText overhead
        androidx.compose.material3.Text(
            description,
            style = baseStyle,
            modifier = modifier,
        )
    } else {
        @Suppress("DEPRECATION")
        ClickableText(
            text = annotatedString,
            style = baseStyle,
            modifier = modifier,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = LINK_TAG, start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val parts = annotation.item.split(":", limit = 2)
                        if (parts.size == 2) {
                            val type = EntityType.valueOf(parts[0])
                            val id = parts[1]
                            when (type) {
                                EntityType.ITEM, EntityType.BLOCK -> onItemTap(id)
                                EntityType.MOB -> onMobTap(id)
                                EntityType.BIOME -> onBiomeTap(id)
                                EntityType.STRUCTURE -> onStructureTap(id)
                                EntityType.ENCHANT -> onEnchantTap(id)
                            }
                        }
                    }
            },
        )
    }
}
