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
        title = { Text("What's your player name?", color = Stone100) },
        text = {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text("Minecraft username", color = Stone500) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (username.isNotBlank()) onSave(username.trim()) },
                enabled = username.isNotBlank(),
            ) {
                Text("Save", color = if (username.isNotBlank()) Gold else Stone500)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text("Later", color = Stone300) }
            TextButton(onClick = onDontAskAgain) { Text("Don't ask again", color = Stone500) }
        },
        containerColor = SurfaceDark,
    )
}
