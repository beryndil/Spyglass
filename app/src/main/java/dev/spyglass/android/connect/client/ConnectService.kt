package dev.spyglass.android.connect.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.spyglass.android.MainActivity
import dev.spyglass.android.R
import dev.spyglass.android.core.CrashReporter

/**
 * Foreground service that keeps the Spyglass Connect WebSocket alive
 * when the app is in the background. Holds a WiFi lock to prevent
 * the radio from sleeping.
 */
class ConnectService : Service() {

    companion object {
        private const val CHANNEL_ID = "spyglass_connect"
        private const val NOTIFICATION_ID = 29170

        fun start(context: Context, deviceName: String) {
            val intent = Intent(context, ConnectService::class.java).apply {
                putExtra("device_name", deviceName)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ConnectService::class.java))
        }
    }

    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWifiLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceName = intent?.getStringExtra("device_name") ?: "PC"
        startForeground(NOTIFICATION_ID, buildNotification(deviceName))
        CrashReporter.log("Connect service started")
        CrashReporter.setKey("service_running", "true")
        return START_STICKY
    }

    override fun onDestroy() {
        CrashReporter.log("Connect service stopped")
        CrashReporter.setKey("service_running", "false")
        releaseWifiLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Spyglass Connect",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when connected to a PC via Spyglass Connect"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(deviceName: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Connected to $deviceName")
            .setContentText("Spyglass Connect is active")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun acquireWifiLock() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        wifiLock = wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "SpyglassConnect::WifiLock",
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWifiLock() {
        wifiLock?.let {
            if (it.isHeld) it.release()
        }
        wifiLock = null
    }
}
