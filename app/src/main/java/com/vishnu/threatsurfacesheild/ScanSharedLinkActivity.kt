package com.vishnu.threatsurfaceshield

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ScanSharedLinkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val url = extractFirstUrl(sharedText)

        if (url != null) {
            // Launch SecureBrowserActivity with this URL to trigger your AI scan
            val i = Intent(this, SecureBrowserActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(url)
            }
            startActivity(i)
        }

        finish()
    }

    private fun extractFirstUrl(text: String): String? {
        val regex = Regex("""https?://\S+|www\.\S+""")
        return regex.find(text)?.value
    }
}
