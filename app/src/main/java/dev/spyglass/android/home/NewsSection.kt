package dev.spyglass.android.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.SpyglassDivider
import java.io.File

@Composable
fun HomeNewsSection() {
    val context = LocalContext.current
    val newsItems = remember { NewsLoader.load(context).sortedByDescending { it.date }.take(5) }
    if (newsItems.isEmpty()) return

    SectionHeader("News")
    Spacer(Modifier.height(8.dp))
    ResultCard {
        newsItems.forEachIndexed { index, item ->
            NewsCard(item)
            if (index < newsItems.lastIndex) {
                SpyglassDivider()
            }
        }
    }
}

@Composable
private fun NewsCard(item: NewsItem) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Optional image
        item.image?.let { imageName ->
            NewsImage(imageName)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            item.date,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(4.dp))
        MarkdownText(
            markdown = item.body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            linkColor = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun NewsImage(imageName: String) {
    val context = LocalContext.current
    val bitmap = remember(imageName) {
        // Check synced news/ directory first
        val syncedFile = File(context.filesDir, "minecraft/news/$imageName")
        if (syncedFile.exists()) {
            BitmapFactory.decodeFile(syncedFile.absolutePath)
        } else {
            // Try bundled assets
            try {
                context.assets.open("minecraft/news/$imageName").use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                null
            }
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
            contentScale = ContentScale.Crop,
        )
    }
}

/**
 * Renders simple Markdown inline formatting: **bold**, *italic*, [links](url).
 * Uses modern LinkAnnotation API (no deprecated ClickableText).
 */
@Composable
private fun MarkdownText(
    markdown: String,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color,
) {
    val annotatedString = remember(markdown, color, linkColor) {
        parseMarkdown(markdown, color, linkColor)
    }
    Text(text = annotatedString, style = style)
}

private fun parseMarkdown(
    markdown: String,
    textColor: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color,
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    val text = markdown

    while (i < text.length) {
        when {
            // Bold: **text**
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end >= 0) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Italic: *text*
            text[i] == '*' && (i + 1 < text.length) && text[i + 1] != '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end >= 0) {
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Link: [text](url)
            text[i] == '[' -> {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket >= 0 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen >= 0) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        builder.pushLink(LinkAnnotation.Url(url))
                        builder.withStyle(SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        )) {
                            append(linkText)
                        }
                        builder.pop()
                        i = closeParen + 1
                    } else {
                        builder.append(text[i])
                        i++
                    }
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Newline
            text[i] == '\n' -> {
                builder.append('\n')
                i++
            }
            else -> {
                builder.append(text[i])
                i++
            }
        }
    }

    return builder.toAnnotatedString()
}
