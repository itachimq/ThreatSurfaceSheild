package com.vishnu.threatsurfaceshield

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class NetworkSafetyActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var detailsText: TextView
    private lateinit var refreshButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_safety)

        statusText = findViewById(R.id.networkStatusText)
        detailsText = findViewById(R.id.networkDetailsText)
        refreshButton = findViewById(R.id.refreshNetworkButton)

        refreshButton.setOnClickListener {
            showNetworkInfo()
        }

        showNetworkInfo()
    }

    private fun showNetworkInfo() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)

        if (network == null || caps == null) {
            statusText.text = "No active network connection"
            statusText.setTextColor(ContextCompat.getColor(this, R.color.red))
            detailsText.text =
                "You are currently offline.\n\n" +
                "• Phishing AI can still scan copied links\n" +
                "• Malware / privacy scans still work for installed apps"
            return
        }

        val isMetered = cm.isActiveNetworkMetered
        val hasVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                evaluateWifi(isMetered, hasVpn)
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                evaluateCellular(isMetered, hasVpn)
            }
            else -> {
                statusText.text = "Connected (special network)"
                statusText.setTextColor(ContextCompat.getColor(this, R.color.amber_dark))
                val vpnText = if (hasVpn) "VPN: active\n" else "VPN: not detected\n"
                detailsText.text =
                    "Type: Other / VPN / tethering\n$vpnText\n" +
                    "This is a non‑standard connection.\n\n" +
                    "Tips:\n" +
                    "• Prefer HTTPS websites\n" +
                    "• Always verify login pages before entering passwords\n" +
                    "• Use ThreatSurface Shield to scan strange links"
            }
        }
    }

    private fun evaluateCellular(isMetered: Boolean, hasVpn: Boolean) {
        statusText.text = "Connected via mobile data"
        statusText.setTextColor(ContextCompat.getColor(this, R.color.green))

        val vpnLine = if (hasVpn) "VPN: active\n" else "VPN: not detected\n"
        val meteredLine = if (isMetered) "Plan: metered (data‑limited)\n" else "Plan: unmetered / unknown\n"

        val risk = "Network risk level: LOW\n" +
                "Reason: Mobile data is harder to attack than random public Wi‑Fi."

        detailsText.text =
            vpnLine + meteredLine + "\n" +
            risk + "\n\n" +
            "Safety tips:\n" +
            "• Still avoid installing APKs from websites\n" +
            "• Use ThreatSurface Shield to scan phishing links\n" +
            "• Enable screen lock & Google Play Protect"
    }

    private fun evaluateWifi(isMetered: Boolean, hasVpn: Boolean) {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo

        val ssid = (info.ssid ?: "").replace("\"", "").ifBlank { "Unknown Wi‑Fi" }
        val bssid = info.bssid

        // Security type from scan results
        var security = "Unknown"
        var signalDbm = info.rssi
        var linkSpeedMbps = info.linkSpeed
        var band = "Unknown band"

        try {
            val scan = wifiManager.scanResults.firstOrNull { it.BSSID == bssid }
            val capsStr = scan?.capabilities ?: ""
            security = when {
                "WPA3" in capsStr -> "WPA3"
                "WPA2" in capsStr -> "WPA2"
                "WPA" in capsStr  -> "WPA"
                "WEP" in capsStr  -> "WEP"
                else              -> "Open / no encryption"
            }

            signalDbm = scan?.level ?: signalDbm
            val freq = scan?.frequency ?: 0
            band = when {
                freq in 2400..2500 -> "2.4 GHz"
                freq in 4900..5900 -> "5 GHz"
                freq in 5901..7100 -> "6 GHz"
                else               -> "Unknown band"
            }
        } catch (_: Exception) {
            // Some devices may restrict scan results; we still show basic info
        }

        val signalQuality = when {
            signalDbm >= -55 -> "Excellent"
            signalDbm >= -65 -> "Good"
            signalDbm >= -75 -> "OK"
            signalDbm >= -85 -> "Weak"
            else             -> "Very weak"
        }

        val isOpen = security == "Open / no encryption" || security == "Unknown" ||
                ssid.contains("free wifi", ignoreCase = true)

        // Network risk estimation (only about Wi‑Fi, not “hackers”)
        val risk: String
        val color: Int

        if (!isOpen && (security == "WPA3" || security == "WPA2")) {
            // Encrypted modern Wi‑Fi
            risk = "Network risk level: LOW\nReason: Encrypted Wi‑Fi (modern security)."
            color = R.color.green
        } else if (!isOpen && security == "WPA") {
            risk = "Network risk level: MEDIUM\nReason: Older Wi‑Fi security (WPA)."
            color = R.color.amber_dark
        } else {
            risk = "Network risk level: MEDIUM–HIGH\nReason: Open or unknown Wi‑Fi; traffic may be visible on the network."
            color = R.color.amber_dark
        }

        statusText.text = "Connected to Wi‑Fi"
        statusText.setTextColor(ContextCompat.getColor(this, color))

        val vpnLine = if (hasVpn) "VPN: active\n" else "VPN: not detected\n"
        val meteredLine = if (isMetered) "Metered connection: yes (hotspot / limited plan)\n" else "Metered connection: no / unknown\n"

        val speedLine = if (linkSpeedMbps > 0) "Link speed (device link): ~${linkSpeedMbps} Mbps\n" else ""
        val signalLine = "Signal: $signalQuality ($signalDbm dBm)\n"

        detailsText.text =
            "SSID: $ssid\n" +
            "Security: $security\n" +
            "Band: $band\n" +
            speedLine +
            signalLine +
            vpnLine +
            meteredLine +
            "\n$risk\n\n" +
            "Tips:\n" +
            "• On open/public Wi‑Fi, avoid entering passwords or OTPs\n" +
            "• Prefer mobile data for banking / email\n" +
            "• Scan suspicious links with ThreatSurface Shield before opening"
    }
}
