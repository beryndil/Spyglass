package dev.spyglass.android.connect

import android.Manifest
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.spyglass.android.R
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * QR scanner screen using CameraX + ZXing for barcode decoding.
 * Scans the pairing QR code from Spyglass Connect desktop app.
 */
@Composable
fun QrScannerScreen(
    onPairingDataScanned: (QrPairingData) -> Unit,
    onBack: () -> Unit,
) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Unbind camera when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            try {
                val future = ProcessCameraProvider.getInstance(context)
                if (future.isDone) {
                    future.get().unbindAll()
                } else {
                    future.addListener(
                        { try { future.get().unbindAll() } catch (_: Exception) {} },
                        ContextCompat.getMainExecutor(context),
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to unbind camera on dispose")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                stringResource(R.string.connect_qr_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
        }

        if (hasCameraPermission) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CameraPreviewWithAnalysis(
                    onQrDecoded = { data ->
                        // Already on main thread via mainHandler post in analyzer
                        try {
                            // Try raw JSON first, then Base64-wrapped JSON
                            val jsonStr = if (data.trimStart().startsWith("{")) {
                                data
                            } else {
                                String(Base64.decode(data, Base64.DEFAULT))
                            }
                            val json = Json { ignoreUnknownKeys = true }
                            val pairingData = json.decodeFromString<QrPairingData>(jsonStr)
                            if (pairingData.app == "spyglass-connect") {
                                onPairingDataScanned(pairingData)
                            } else {
                                scanError = context.getString(R.string.connect_qr_not_spyglass)
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to parse QR data: ${e.message}")
                            scanError = context.getString(R.string.connect_invalid_qr, e.message?.take(80) ?: "")
                        }
                    },
                )

                // Scanning overlay
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                )

                // Instructions
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.connect_qr_instruction),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    scanError?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            error,
                            color = Color(0xFFF44336),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        } else {
            // Permission denied
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.connect_qr_camera_required),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text(stringResource(R.string.connect_qr_grant_permission))
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewWithAnalysis(onQrDecoded: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val decoded = remember { AtomicBoolean(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        val executor = Executors.newSingleThreadExecutor()
                        analysis.setAnalyzer(executor) { imageProxy ->
                            if (decoded.get()) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val buffer = imageProxy.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)

                            val source = PlanarYUVLuminanceSource(
                                bytes,
                                imageProxy.width,
                                imageProxy.height,
                                0, 0,
                                imageProxy.width,
                                imageProxy.height,
                                false,
                            )

                            try {
                                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                                val result = MultiFormatReader().apply {
                                    setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
                                }.decode(binaryBitmap)

                                if (decoded.compareAndSet(false, true)) {
                                    // Post to main thread for Compose state and navigation
                                    mainHandler.post {
                                        onQrDecoded(result.text)
                                    }
                                }
                            } catch (_: NotFoundException) {
                                // No QR code found in this frame
                            } catch (e: Exception) {
                                Timber.w(e, "QR decode error")
                            } finally {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Camera bind failed")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize(),
    )
}
