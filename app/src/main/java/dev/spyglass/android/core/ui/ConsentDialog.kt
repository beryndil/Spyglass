package dev.spyglass.android.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R

@Composable
fun ConsentDialog(
    onResult: (analyticsConsent: Boolean, crashConsent: Boolean, adPersonalizationConsent: Boolean) -> Unit,
) {
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()
    var showCustomize by remember { mutableStateOf(false) }
    var analyticsChecked by remember { mutableStateOf(true) }
    var crashChecked by remember { mutableStateOf(true) }
    var adPersonalizationChecked by remember { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = { /* non-dismissible */ },
        title = {
            Text(
                stringResource(R.string.consent_title),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.consent_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showCustomize) {
                    SpyglassDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = analyticsChecked,
                            onCheckedChange = { hapticConfirm(); analyticsChecked = it },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.consent_analytics), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.consent_analytics_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = crashChecked,
                            onCheckedChange = { hapticConfirm(); crashChecked = it },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.consent_crash_reports), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.consent_crash_reports_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = adPersonalizationChecked,
                            onCheckedChange = { hapticConfirm(); adPersonalizationChecked = it },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.consent_personalized_ads), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.consent_personalized_ads_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.consent_privacy_policy_link),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://hardknocks.university/privacy-policy.html")
                    },
                )
            }
        },
        confirmButton = {
            if (showCustomize) {
                TextButton(onClick = { hapticClick(); onResult(analyticsChecked, crashChecked, adPersonalizationChecked) }) {
                    Text(stringResource(R.string.consent_save_choices), color = MaterialTheme.colorScheme.primary)
                }
            } else {
                TextButton(onClick = { hapticClick(); onResult(true, true, true) }) {
                    Text(stringResource(R.string.consent_accept_all), color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        dismissButton = {
            if (showCustomize) {
                TextButton(onClick = { hapticClick(); showCustomize = false }) {
                    Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row {
                    TextButton(onClick = { hapticClick(); onResult(false, false, false) }) {
                        Text(stringResource(R.string.consent_decline_all), color = MaterialTheme.colorScheme.secondary)
                    }
                    TextButton(onClick = { hapticClick(); showCustomize = true }) {
                        Text(stringResource(R.string.consent_customize), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
