package dev.spyglass.android.help

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

@Composable
fun HelpScreen(onBack: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    val hapticClick = rememberHapticClick()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(stringResource(R.string.help_title))
        ResultCard {
            Text(
                text = stringResource(R.string.help_wiki),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { hapticClick(); uriHandler.openUri("https://github.com/beryndil/Spyglass/wiki") }
                    .padding(vertical = 8.dp),
            )
        }
    }
}
