package com.vishnu.threatsurfaceshield

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ThreatResponseActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var summaryText: TextView
    private lateinit var reasonsText: TextView
    private lateinit var permsText: TextView

    private lateinit var uninstallButton: Button
    private lateinit var managePermsButton: Button
    private lateinit var accessibilityButton: Button
    private lateinit var overlayButton: Button
    private lateinit var unknownSourcesButton: Button

    private var pkgName: String = ""
    private var riskLevel: String = ""
    private var riskScore: Int = 0
    private var isSystemApp: Boolean = false
    private var isFromPlayStore: Boolean = false
    private var dangerousPermissions: List<String> = emptyList()
    private var reasons: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_threat_response)

        titleText = findViewById(R.id.responseTitle)
        summaryText = findViewById(R.id.responseSummary)
        reasonsText = findViewById(R.id.responseReasons)
        permsText = findViewById(R.id.responsePerms)

        uninstallButton = findViewById(R.id.buttonUninstall)
        managePermsButton = findViewById(R.id.buttonManagePerms)
        accessibilityButton = findViewById(R.id.buttonAccessibilitySettings)
        overlayButton = findViewById(R.id.buttonOverlaySettings)
        unknownSourcesButton = findViewById(R.id.buttonUnknownSources)

        readExtras()
        bindUi()
        setupButtons()
    }

    private fun readExtras() {
        intent?.let {
            pkgName = it.getStringExtra("packageName") ?: ""
            val label = it.getStringExtra("label") ?: pkgName
            riskLevel = it.getStringExtra("riskLevel") ?: "UNKNOWN"
            riskScore = it.getIntExtra("riskScore", 0)
            isSystemApp = it.getBooleanExtra("isSystemApp", false)
            isFromPlayStore = it.getBooleanExtra("isFromPlayStore", false)
            dangerousPermissions =
                it.getStringArrayListExtra("dangerousPermissions") ?: emptyList()
            reasons =
                it.getStringArrayListExtra("reasons") ?: emptyList()

            titleText.text = label
        }
    }

    private fun bindUi() {
        val source = if (isFromPlayStore) "Play Store" else "Unknown source"
        val system = if (isSystemApp) "System app" else "User app"

        summaryText.text =
            "Risk level: $riskLevel ($riskScore/100)\n" +
            "Type: $system\n" +
            "Install source: $source\n" +
            "Package: $pkgName"

        reasonsText.text = if (reasons.isEmpty()) {
            "Signals: none"
        } else {
            "Signals:\n• " + reasons.joinToString("\n• ")
        }

        permsText.text = if (dangerousPermissions.isEmpty()) {
            "High‑risk permissions: none detected."
        } else {
            "High‑risk permissions:\n" + dangerousPermissions.joinToString("\n")
        }

        // Hide some buttons if not relevant
        val hasAccessibility =
            dangerousPermissions.any { it.contains("BIND_ACCESSIBILITY_SERVICE") }
        val hasOverlay =
            dangerousPermissions.any { it.contains("SYSTEM_ALERT_WINDOW") }

        if (!hasAccessibility) {
            accessibilityButton.isEnabled = false
            accessibilityButton.alpha = 0.5f
        }
        if (!hasOverlay) {
            overlayButton.isEnabled = false
            overlayButton.alpha = 0.5f
        }

        if (isFromPlayStore) {
            unknownSourcesButton.isEnabled = false
            unknownSourcesButton.alpha = 0.5f
        }
    }

    private fun setupButtons() {
        uninstallButton.setOnClickListener {
            openAppSettings()
        }

        managePermsButton.setOnClickListener {
            openAppSettings()
        }

        accessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }

        overlayButton.setOnClickListener {
            openOverlaySettings()
        }

        unknownSourcesButton.setOnClickListener {
            openUnknownSourcesSettings()
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$pkgName")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun openOverlaySettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$pkgName")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun openUnknownSourcesSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$pkgName")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}
