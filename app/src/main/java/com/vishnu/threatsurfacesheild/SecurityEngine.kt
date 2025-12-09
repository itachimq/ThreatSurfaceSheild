package com.vishnu.threatsurfaceshield

import android.content.Context

data class GlobalSecurityStatus(
    val phishingRisk: Int,           // 0..100
    val phishingVerdict: String,     // "LOW" / "MEDIUM" / "HIGH"
    val malwareAppsHigh: Int,
    val malwareAppsMedium: Int,
    val privacyMaxScore: Int,        // max privacy exposure among apps
    val wifiRisk: String,            // "LOW" / "MEDIUM" / "HIGH" / "OFFLINE"
    val summary: String              // short text for dashboard
)

object SecurityEngine {

    fun computeStatus(
        context: Context,
        lastPhishDecision: ScanDecision?,    // from PhishAi.analyze()
        malwareList: List<AppRisk>,          // from MalwareRiskEngine.scanInstalledApps()
        maxPrivacyScore: Int,                // from PrivacyScanActivity computePrivacyScore
        wifiRiskLevel: String                // from NetworkSafetyActivity
    ): GlobalSecurityStatus {

        // 1) Phishing normalization
        val phishScore = lastPhishDecision?.finalScore ?: 0
        val phishVerdict = lastPhishDecision?.verdict ?: "LOW"

        // 2) Malware summary (only really suspicious ones matter)
        val highCount = malwareList.count { it.riskLevel == "HIGH" }
        val medCount  = malwareList.count { it.riskLevel == "MEDIUM" }

        // 3) Privacy → we already have per-app score, here we just take max
        val privacyMax = maxPrivacyScore

        // 4) Wifi risk is already a word from your NetworkSafetyActivity
        val wifi = wifiRiskLevel

        // 5) Build a human summary for dashboard
        val summaryBuilder = StringBuilder()

        // phishing
        summaryBuilder.append(
            when (phishVerdict) {
                "HIGH" -> "⚠️ Current link looks highly suspicious. "
                "MEDIUM" -> "⚠️ Current link looks somewhat risky. "
                else -> "✅ Current link seems low‑risk. "
            }
        )

        // malware
        when {
            highCount > 0 -> summaryBuilder.append("Found $highCount high‑risk sideloaded apps. ")
            medCount > 0  -> summaryBuilder.append("Found $medCount apps with risky behavior. ")
            else          -> summaryBuilder.append("No clearly suspicious apps detected. ")
        }

        // privacy
        when {
            privacyMax >= 70 -> summaryBuilder.append("Some apps have very high privacy access. ")
            privacyMax >= 40 -> summaryBuilder.append("Several apps have medium privacy access. ")
            else             -> summaryBuilder.append("App privacy exposure is mild. ")
        }

        // wifi
        summaryBuilder.append("Wi‑Fi risk: $wifi.")

        return GlobalSecurityStatus(
            phishingRisk = phishScore,
            phishingVerdict = phishVerdict,
            malwareAppsHigh = highCount,
            malwareAppsMedium = medCount,
            privacyMaxScore = privacyMax,
            wifiRisk = wifi,
            summary = summaryBuilder.toString()
        )
    }
}
