package com.vishnu.threatsurfaceshield

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import kotlin.math.roundToInt

class PrivacyScanActivity : AppCompatActivity() {

    private lateinit var scanButton: Button
    private lateinit var resultContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_scan)

        scanButton = findViewById(R.id.privacyScanButton)
        resultContainer = findViewById(R.id.privacyResultsContainer)
        progressBar = findViewById(R.id.privacyProgress)
        statusText = findViewById(R.id.privacyStatusText)

        scanButton.setOnClickListener {
            startScan()
        }
    }

    private fun startScan() {
        scanButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        statusText.text = "Scanning apps for privacy exposure..."
        resultContainer.removeAllViews()

        Thread {
            val allRisks = MalwareRiskEngine.scanInstalledApps(this)

            // Only user apps (not system/vendor) with at least one dangerous permission
            val userApps = allRisks.filter { risk ->
                !risk.isSystemApp && risk.dangerousPermissions.isNotEmpty()
            }

            // Attach privacy score (0..100) based purely on permissions
            val scored = userApps.map { risk ->
                val score = computePrivacyScore(risk.dangerousPermissions)
                val level = when {
                    score >= 70 -> "HIGH"
                    score >= 40 -> "MEDIUM"
                    else -> "LOW"
                }
                Triple(risk, score, level)
            }.sortedByDescending { it.second }

            runOnUiThread {
                progressBar.visibility = View.GONE
                scanButton.isEnabled = true

                if (scored.isEmpty()) {
                    statusText.text = "No user apps with strong privacy access found."
                    return@runOnUiThread
                }

                statusText.text =
                    "Privacy exposure by app (not malware judgement):\n" +
                    "Higher score = more access to sensitive data."

                for ((risk, score, level) in scored) {
                    addPrivacyRow(risk, score, level)
                }
            }
        }.start()
    }

    // Build a 0..100 privacy score from permission strings
    private fun computePrivacyScore(perms: List<String>): Int {
        var score = 0.0

        perms.forEach { perm ->
            when {
                // Very strong data
                perm.contains("RECEIVE_SMS") || perm.contains("READ_SMS") ||
                perm.contains("SEND_SMS") ||
                perm.contains("READ_CALL_LOG") || perm.contains("WRITE_CALL_LOG") -> {
                    score += 18.0
                }

                // Sensitive sensors
                perm.contains("RECORD_AUDIO") ||
                perm.contains("CAMERA") -> {
                    score += 12.0
                }

                // People / location
                perm.contains("READ_CONTACTS") || perm.contains("WRITE_CONTACTS") ||
                perm.contains("LOCATION") -> {
                    score += 9.0
                }

                // Powerful control
                perm.contains("BIND_ACCESSIBILITY_SERVICE") ||
                perm.contains("SYSTEM_ALERT_WINDOW") -> {
                    score += 16.0
                }

                // Install ability
                perm.contains("REQUEST_INSTALL_PACKAGES") -> {
                    score += 12.0
                }

                // default small bump
                else -> score += 3.0
            }
        }

        if (score > 100.0) score = 100.0
        if (score < 0.0) score = 0.0
        return score.roundToInt()
    }

    private fun addPrivacyRow(risk: AppRisk, privacyScore: Int, level: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24)
        }

        val title = TextView(this).apply {
            text = risk.label.ifBlank { risk.packageName }
            textSize = 16f
            setTextColor(ContextCompat.getColor(this.context, R.color.white))
        }

        val pkgText = TextView(this).apply {
            text = "Package: ${risk.packageName}"
            textSize = 11f
            setTextColor(ContextCompat.getColor(this.context, R.color.grey))
        }

        val levelColor = when (level) {
            "HIGH" -> R.color.red
            "MEDIUM" -> R.color.amber_dark
            else -> R.color.green
        }

        val levelText = TextView(this).apply {
            text = "Privacy exposure: $level (${privacyScore}/100)"
            textSize = 13f
            setTextColor(ContextCompat.getColor(this.context, levelColor))
        }

        val summaryTags = buildAccessSummary(risk.dangerousPermissions)
        val summaryText = TextView(this).apply {
            text = "Access: ${summaryTags.joinToString(", ")}"
            textSize = 12f
            setTextColor(ContextCompat.getColor(this.context, R.color.amber_light))
        }

        val permsExplained = risk.dangerousPermissions.joinToString("\n") { perm ->
            when {
                perm.contains("RECEIVE_SMS") || perm.contains("READ_SMS") ->
                    "• $perm → Can read your SMS / OTP codes"
                perm.contains("SEND_SMS") ->
                    "• $perm → Can send SMS from your device"
                perm.contains("READ_CONTACTS") ->
                    "• $perm → Can read your contacts"
                perm.contains("RECORD_AUDIO") ->
                    "• $perm → Can record microphone audio"
                perm.contains("CAMERA") ->
                    "• $perm → Can access camera"
                perm.contains("ACCESS_FINE_LOCATION") ||
                perm.contains("ACCESS_COARSE_LOCATION") ->
                    "• $perm → Can track your location"
                perm.contains("READ_CALL_LOG") ->
                    "• $perm → Can read your call history"
                perm.contains("BIND_ACCESSIBILITY_SERVICE") ->
                    "• $perm → Can see & control your screen (Accessibility)"
                perm.contains("SYSTEM_ALERT_WINDOW") ->
                    "• $perm → Can draw over other apps (overlay screens)"
                perm.contains("REQUEST_INSTALL_PACKAGES") ->
                    "• $perm → Can request installing APK files"
                else -> "• $perm"
            }
        }

        val permsText = TextView(this).apply {
            text = permsExplained
            textSize = 11f
            setTextColor(ContextCompat.getColor(this.context, R.color.grey))
        }

        row.addView(title)
        row.addView(pkgText)
        row.addView(levelText)
        row.addView(summaryText)
        row.addView(permsText)

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
            setBackgroundColor(ContextCompat.getColor(this.context, R.color.grey_dark))
        }

        resultContainer.addView(row)
        resultContainer.addView(divider)
    }

    private fun buildAccessSummary(perms: List<String>): Set<String> {
        val tags = mutableSetOf<String>()

        if (perms.any { it.contains("SMS") }) tags.add("SMS / OTP")
        if (perms.any { it.contains("CONTACT") }) tags.add("Contacts")
        if (perms.any { it.contains("RECORD_AUDIO") }) tags.add("Microphone")
        if (perms.any { it.contains("CAMERA") }) tags.add("Camera")
        if (perms.any { it.contains("LOCATION") }) tags.add("Location")
        if (perms.any { it.contains("CALL_LOG") }) tags.add("Call history")
        if (perms.any { it.contains("BIND_ACCESSIBILITY_SERVICE") }) tags.add("Screen control")
        if (perms.any { it.contains("SYSTEM_ALERT_WINDOW") }) tags.add("Overlay windows")
        if (perms.any { it.contains("REQUEST_INSTALL_PACKAGES") }) tags.add("Install APKs")

        if (tags.isEmpty()) tags.add("General permissions")
        return tags
    }
}
