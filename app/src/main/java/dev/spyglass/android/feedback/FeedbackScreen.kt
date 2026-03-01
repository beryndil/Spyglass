package dev.spyglass.android.feedback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun FeedbackScreen(onBack: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    val issuesUrl = "https://github.com/beryndil/spyglass-android/issues"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionHeader(stringResource(R.string.feedback_title))
        ResultCard {
            Text(
                text = stringResource(R.string.feedback_report_bug),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("$issuesUrl/new?labels=bug") }
                    .padding(vertical = 8.dp),
            )
            SpyglassDivider()
            Text(
                text = stringResource(R.string.feedback_request_feature),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("$issuesUrl/new?labels=enhancement") }
                    .padding(vertical = 8.dp),
            )
            SpyglassDivider()
            Text(
                text = stringResource(R.string.feedback_rate),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://play.google.com/store/apps/details?id=dev.spyglass.android") }
                    .padding(vertical = 8.dp),
            )
        }

        Text(
            stringResource(R.string.feedback_thanks),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )

        Spacer(Modifier.height(8.dp))
    }
}
