package dev.spyglass.android.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.net.MojangApi
import dev.spyglass.android.core.ui.*

@Composable
fun UsernameDialog(
    onSave: (String) -> Unit,
    onLater: () -> Unit,
    onDontAskAgain: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    val isValid = username.isBlank() || MojangApi.USERNAME_REGEX.matches(username)
    val canSave = username.isNotBlank() && isValid

    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.username_dialog_title), color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { if (it.length <= 16) username = it },
                    placeholder = { Text(stringResource(R.string.username_dialog_placeholder), color = MaterialTheme.colorScheme.secondary) },
                    singleLine = true,
                    isError = !isValid,
                    supportingText = if (!isValid) {
                        { Text(stringResource(R.string.username_dialog_error), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (canSave) onSave(username.trim()) },
                enabled = canSave,
            ) {
                Text(stringResource(R.string.save), color = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text(stringResource(R.string.username_dialog_later), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = onDontAskAgain) { Text(stringResource(R.string.username_dialog_dont_ask), color = MaterialTheme.colorScheme.secondary) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
