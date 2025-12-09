package com.vishnu.threatsurfaceshield

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Using the correct IDs from the new dark theme layout
        val secureBrowserCard = findViewById<MaterialCardView>(R.id.cardSecureBrowser)
        val malwareScannerCard = findViewById<MaterialCardView>(R.id.cardMalware)
        val networkSafetyCard = findViewById<MaterialCardView>(R.id.cardNetwork)
        val privacyScannerCard = findViewById<MaterialCardView>(R.id.cardPrivacy)

        secureBrowserCard.setOnClickListener {
            startActivity(Intent(this, SecureBrowserActivity::class.java))
        }

        malwareScannerCard.setOnClickListener {
            startActivity(Intent(this, MalwareScanActivity::class.java))
        }

        networkSafetyCard.setOnClickListener {
            startActivity(Intent(this, NetworkSafetyActivity::class.java))
        }

        privacyScannerCard.setOnClickListener {
            startActivity(Intent(this, PrivacyScanActivity::class.java))
        }
    }
}
