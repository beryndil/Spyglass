package dev.spyglass.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import dev.spyglass.android.core.ReviewHelper
import dev.spyglass.android.core.ui.ConsentDialog
import dev.spyglass.android.core.ui.DEFAULT_THEME
import dev.spyglass.android.core.ui.SpyglassTheme
import dev.spyglass.android.navigation.AppNavGraph
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : FragmentActivity() {

    private val isUnlocked = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Track app opens and prompt for review after 10 launches (one-time)
        ReviewHelper.trackOpenAndPrompt(this)

        // Check if app lock is enabled
        val appLockEnabled = runBlocking {
            dataStore.data.map { it[PreferenceKeys.APP_LOCK_ENABLED] ?: false }.first()
        }

        if (appLockEnabled) {
            promptBiometric()
        } else {
            isUnlocked.value = true
        }

        setContent {
            val theme by dataStore.data
                .map { it[PreferenceKeys.BACKGROUND_THEME] ?: DEFAULT_THEME }
                .collectAsStateWithLifecycle(initialValue = DEFAULT_THEME)

            val dynamicColor by dataStore.data
                .map { it[PreferenceKeys.DYNAMIC_COLOR] ?: false }
                .collectAsStateWithLifecycle(initialValue = false)

            val highContrast by dataStore.data
                .map { it[PreferenceKeys.HIGH_CONTRAST] ?: false }
                .collectAsStateWithLifecycle(initialValue = false)

            val consentShown by dataStore.data
                .map { it[PreferenceKeys.CONSENT_SHOWN] ?: false }
                .collectAsStateWithLifecycle(initialValue = true) // default true to avoid flash

            val scope = rememberCoroutineScope()

            val configuration = LocalConfiguration.current
            val isWideScreen = configuration.screenWidthDp.dp >= 600.dp

            SpyglassTheme(
                theme = theme,
                isWideScreen = isWideScreen,
                dynamicColor = dynamicColor,
                highContrast = highContrast,
            ) {
                if (!consentShown) {
                    ConsentDialog { analyticsConsent, crashConsent, adPersonalizationConsent ->
                        scope.launch {
                            dataStore.edit {
                                it[PreferenceKeys.ANALYTICS_CONSENT] = analyticsConsent
                                it[PreferenceKeys.CRASH_CONSENT] = crashConsent
                                it[PreferenceKeys.AD_PERSONALIZATION_CONSENT] = adPersonalizationConsent
                                it[PreferenceKeys.CONSENT_SHOWN] = true
                            }
                        }
                    }
                }

                val unlocked by isUnlocked
                if (unlocked) {
                    AppNavGraph()
                }
            }
        }
    }

    private fun promptBiometric() {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // No biometric/PIN available — skip lock
            isUnlocked.value = true
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isUnlocked.value = true
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    finish()
                } else {
                    isUnlocked.value = true
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.settings_app_lock_title))
            .setSubtitle(getString(R.string.settings_app_lock_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
