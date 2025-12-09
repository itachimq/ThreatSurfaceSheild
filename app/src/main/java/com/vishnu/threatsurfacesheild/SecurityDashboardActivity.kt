package com.vishnu.threatsurfaceshield

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SecurityDashboardActivity : AppCompatActivity() {

    private lateinit var headlineText: TextView
    private lateinit var detailText: TextView
    private lateinit var phishingButton: Button
    private lateinit var malwareButton: Button
    private lateinit var privacyButton: Button
    private lateinit var networkButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_dashboard)

        headlineText = findViewById(R.id.dashboardHeadlineText)
        detailText   = findViewById(R.id.dashboardDetailText)
        phishingButton = findViewById(R.id.openPhishingButton)
        malwareButton  = findViewById(R.id.openMalwareButton)
        privacyButton  = findViewById(R.id.openPrivacyButton)
        networkButton  = findViewById(R.id.openNetworkButton)

        val lastPhish = LastStateStore.lastPhishDecision
        val malware   = LastStateStore.lastMalwareList
        val maxPrivacy = LastStateStore.lastMaxPrivacyScore
        val wifiRisk   = LastStateStore.lastWifiRiskLevel ?: "UNKNOWN"

        val status = SecurityEngine.computeStatus(
            context = this,
            lastPhishDecision = lastPhish,
            malwareList = malware,
            maxPrivacyScore = maxPrivacy,
            wifiRiskLevel = wifiRisk
        )

        headlineText.text = when {
            status.malwareAppsHigh > 0 || status.phishingVerdict == "HIGH" ->
                "⚠️ Your device may be at risk"
            status.malwareAppsMedium > 0 || status.privacyMaxScore >= 70 ->
                "⚠️ Some security issues detected"
            else ->
                "✅ Your device looks safe right now"
        }

        detailText.text = status.summary

        phishingButton.setOnClickListener {
            startActivity(Intent(this, SecureBrowserActivity::class.java))
        }
        malwareButton.setOnClickListener {
            startActivity(Intent(this, MalwareScanActivity::class.java))
        }
        privacyButton.setOnClickListener {
            startActivity(Intent(this, PrivacyScanActivity::class.java))
        }
        networkButton.setOnClickListener {
            startActivity(Intent(this, NetworkSafetyActivity::class.java))
        }
    }
}
