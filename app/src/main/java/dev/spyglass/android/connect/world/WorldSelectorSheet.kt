package dev.spyglass.android.connect.world

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.spyglass.android.connect.WorldInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet listing worlds received from the desktop app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldSelectorSheet(
    worlds: List<WorldInfo>,
    selectedWorld: String?,
    onSelectWorld: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                "Select World",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            worlds.forEach { world ->
                val isSelected = world.folderName == selectedWorld
                ListItem(
                    headlineContent = { Text(world.displayName) },
                    supportingContent = {
                        val date = if (world.lastPlayed > 0) dateFormat.format(Date(world.lastPlayed)) else "Unknown"
                        Text("${world.gameMode.replaceFirstChar { it.uppercase() }} • $date")
                    },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Public,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.clickable {
                        onSelectWorld(world.folderName)
                        onDismiss()
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
