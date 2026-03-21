package dev.spyglass.android.core.module

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.BuildConfig
import dev.spyglass.android.R
import dev.spyglass.android.core.FirebaseHelper
import dev.spyglass.android.core.ui.Red400
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.SpyglassDivider
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Privacy & Security settings — analytics consent, crash reporting, app lock, data deletion. */
@Composable
internal fun PrivacySecurityContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val hapticClick = dev.spyglass.android.core.ui.rememberHapticClick()
    val hapticConfirm = dev.spyglass.android.core.ui.rememberHapticConfirm()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val analyticsConsent by remember {
        context.dataStore.data.map { it[PreferenceKeys.ANALYTICS_CONSENT] ?: false }
    }.collectAsStateWithLifecycle(initialValue = false)

    val crashConsent by remember {
        context.dataStore.data.map { it[PreferenceKeys.CRASH_CONSENT] ?: false }
    }.collectAsStateWithLifecycle(initialValue = false)

    val adPersonalizationConsent by remember {
        context.dataStore.data.map { it[PreferenceKeys.AD_PERSONALIZATION_CONSENT] ?: false }
    }.collectAsStateWithLifecycle(initialValue = false)

    val appLockEnabled by remember {
        context.dataStore.data.map { it[PreferenceKeys.APP_LOCK_ENABLED] ?: false }
    }.collectAsStateWithLifecycle(initialValue = false)

    SectionHeader(stringResource(R.string.settings_privacy_security))
    ResultCard {
        SettingsToggle(
            title = stringResource(R.string.settings_app_lock),
            description = stringResource(R.string.settings_app_lock_desc),
            checked = appLockEnabled,
            onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.APP_LOCK_ENABLED] = !appLockEnabled } } },
        )
        SpyglassDivider()
        SettingsToggle(
            title = stringResource(R.string.consent_analytics),
            description = stringResource(R.string.consent_analytics_desc),
            checked = analyticsConsent,
            onCheckedChange = {
                scope.launch {
                    val newVal = !analyticsConsent
                    context.dataStore.edit { it[PreferenceKeys.ANALYTICS_CONSENT] = newVal }
                    FirebaseHelper.applyConsent(crashConsent, newVal)
                }
            },
        )
        SpyglassDivider()
        SettingsToggle(
            title = stringResource(R.string.consent_crash_reports),
            description = stringResource(R.string.consent_crash_reports_desc),
            checked = crashConsent,
            onCheckedChange = {
                scope.launch {
                    val newVal = !crashConsent
                    context.dataStore.edit { it[PreferenceKeys.CRASH_CONSENT] = newVal }
                    FirebaseHelper.applyConsent(newVal, analyticsConsent)
                }
            },
        )
        SpyglassDivider()
        SettingsToggle(
            title = stringResource(R.string.consent_personalized_ads),
            description = stringResource(R.string.consent_personalized_ads_desc),
            checked = adPersonalizationConsent,
            onCheckedChange = {
                scope.launch {
                    context.dataStore.edit { it[PreferenceKeys.AD_PERSONALIZATION_CONSENT] = !adPersonalizationConsent }
                }
            },
        )
        SpyglassDivider()
        TextButton(onClick = { hapticConfirm(); showDeleteConfirm = true }) {
            Text(stringResource(R.string.settings_delete_data), color = Red400)
        }
        SpyglassDivider()
        Text(
            text = stringResource(R.string.settings_privacy_policy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                hapticClick()
                uriHandler.openUri("https://hardknocks.university/privacy-policy.html")
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_data_title), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(stringResource(R.string.settings_delete_data_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                TextButton(onClick = {
                    hapticConfirm()
                    scope.launch {
                        dev.spyglass.android.data.repository.GameDataRepository.get(context).deleteAllUserData()
                    }
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.settings_delete_data_confirm), color = Red400)
                }
            },
            dismissButton = {
                TextButton(onClick = { hapticClick(); showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

/** About section — version, links to feedback/changelog/about screens, app permissions. */
@Composable
internal fun AboutContent(scope: SettingsSectionScope) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    SectionHeader(stringResource(R.string.settings_about_section))
    ResultCard {
        Text(
            stringResource(R.string.settings_version_label, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        SpyglassDivider()
        SettingsLink(
            title = stringResource(R.string.settings_rate_app),
            description = stringResource(R.string.settings_rate_app_desc),
            onClick = { uriHandler.openUri("https://play.google.com/store/apps/details?id=dev.spyglass.android") },
        )
        SpyglassDivider()
        SettingsLink(
            title = stringResource(R.string.settings_send_feedback),
            description = stringResource(R.string.settings_send_feedback_desc),
            onClick = { scope.navigateTo("feedback") },
        )
        SpyglassDivider()
        SettingsLink(
            title = stringResource(R.string.settings_changelog),
            description = stringResource(R.string.settings_changelog_desc),
            onClick = { scope.navigateTo("changelog") },
        )
        SpyglassDivider()
        SettingsLink(
            title = stringResource(R.string.settings_about),
            description = stringResource(R.string.settings_about_desc),
            onClick = { scope.navigateTo("about") },
        )
        SpyglassDivider()
        SettingsLink(
            title = stringResource(R.string.settings_app_permissions),
            description = stringResource(R.string.settings_app_permissions_desc),
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
        )
    }
}
