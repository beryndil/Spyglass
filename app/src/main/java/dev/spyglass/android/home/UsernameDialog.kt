package dev.spyglass.android.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.spyglass.android.core.ui.*

@Composable
fun UsernameDialog(
    onSave: (String) -> Unit,
    onLater: () -> Unit,
    onDontAskAgain: () -> Unit,
) {
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("What's your player name?", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            OutlinedTextField(
                value = username,
                onValueChange = { if (it.length <= 16) username = it },
                placeholder = { Text("Minecraft username", color = MaterialTheme.colorScheme.secondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (username.isNotBlank()) onSave(username.trim()) },
                enabled = username.isNotBlank(),
            ) {
                Text("Save", color = if (username.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = onDontAskAgain) { Text("Don't ask again", color = MaterialTheme.colorScheme.secondary) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
