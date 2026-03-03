package dev.spyglass.android.core.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import dev.spyglass.android.BuildConfig
import timber.log.Timber

@SuppressLint("MissingPermission")
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    val adView by remember {
        mutableStateOf(
            try {
                AdView(context).apply {
                    setAdSize(
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                            context, screenWidthDp
                        )
                    )
                    adUnitId = BuildConfig.ADMOB_BANNER_ID
                    adListener = object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Timber.w("Ad failed to load: %s", error.message)
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create AdView")
                null
            }
        )
    }

    if (adView == null) return

    DisposableEffect(Unit) {
        onDispose { adView?.destroy() }
    }

    AndroidView(
        factory = { adView!! },
        modifier = modifier.fillMaxWidth(),
    )
}
