package dev.spyglass.android.connect.client

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import timber.log.Timber

/**
 * Discover Spyglass Connect desktop app on the LAN using Android NSD (mDNS).
 * More reliable on Android than JmDNS.
 */
class MdnsDiscovery(private val context: Context) {

    companion object {
        const val SERVICE_TYPE = "_spyglass._tcp."
    }

    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var onFound: ((String, Int) -> Unit)? = null

    /** Start discovering Spyglass Connect services on the LAN. */
    fun startDiscovery(onServiceFound: (ip: String, port: Int) -> Unit) {
        onFound = onServiceFound
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Timber.d("mDNS discovery started for $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Timber.d("mDNS service found: ${serviceInfo.serviceName}")
                nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        Timber.w("mDNS resolve failed: $errorCode")
                    }

                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val host = info.host?.hostAddress ?: return
                        val port = info.port
                        Timber.d("mDNS resolved: $host:$port")
                        onFound?.invoke(host, port)
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.d("mDNS service lost: ${serviceInfo.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.d("mDNS discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.w("mDNS start failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.w("mDNS stop failed: $errorCode")
            }
        }

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Timber.w(e, "Failed to start mDNS discovery")
        }
    }

    /** Stop discovery. */
    fun stopDiscovery() {
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {
            // May throw if already stopped
        }
        discoveryListener = null
        onFound = null
    }
}
