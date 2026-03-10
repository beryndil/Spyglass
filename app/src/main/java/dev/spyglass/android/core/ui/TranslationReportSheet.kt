package dev.spyglass.android.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import dev.spyglass.android.data.db.entities.TranslationReportEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.launch
import java.net.URLEncoder

/**
 * Row shown in detail cards when the locale is not English.
 * Tapping opens a bottom sheet for reporting translation issues.
 */
@Composable
fun ReportTranslationRow(
    entityType: String,
    entityName: String,
    entityId: String,
    textColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val locale = LocalAppLocale.current
    if (locale == "en") return

    var showSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Translate, contentDescription = null, modifier = Modifier.size(16.dp), tint = textColor)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.core_report_translation), style = MaterialTheme.typography.labelSmall, color = textColor)
    }

    if (showSheet) {
        TranslationReportSheet(
            locale = locale,
            entityType = entityType,
            entityName = entityName,
            entityId = entityId,
            onDismiss = { showSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationReportSheet(
    locale: String,
    entityType: String,
    entityName: String,
    entityId: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var comment by remember { mutableStateOf("") }

    val langName = SupportedLanguages.firstOrNull { it.first == locale }?.second ?: locale

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(R.string.core_report_translation_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Text(
                stringResource(R.string.core_report_translation_entity, entityType, entityName),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.core_report_translation_language, langName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(stringResource(R.string.core_report_translation_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        val repo = GameDataRepository.get(context)
                        repo.createTranslationReport(
                            TranslationReportEntity(
                                locale = locale,
                                screenName = entityType.lowercase(),
                                entityType = entityType.lowercase(),
                                stringKey = entityId,
                                currentValue = entityName,
                                comment = comment,
                                reportedAt = System.currentTimeMillis(),
                            )
                        )

                        val title = URLEncoder.encode(
                            "[Translation] $langName — $entityType: $entityName", "UTF-8"
                        )
                        val body = URLEncoder.encode(
                            "**Language:** $langName ($locale)\n" +
                            "**$entityType:** $entityName\n" +
                            "**ID:** `$entityId`\n\n" +
                            "### What's wrong?\n${comment.ifBlank { "(no details provided)" }}\n\n" +
                            "### Suggested correction\n\n",
                            "UTF-8"
                        )
                        val url = "https://github.com/beryndil/Spyglass-Data/issues/new?title=$title&body=$body&labels=translation"
                        uriHandler.openUri(url)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.core_report_translation_submit))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
