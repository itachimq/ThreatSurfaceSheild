package com.vishnu.threatsurfaceshield

import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.threatsurface.shield.net.ApiClient
import com.threatsurface.shield.net.UrlCheckRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SecureBrowserActivity : AppCompatActivity() {

    private lateinit var resultText: TextView
    private lateinit var webView: WebView
    private lateinit var urlInput: EditText
    private lateinit var scanButton: Button
    private lateinit var historyButton: Button
    private lateinit var fullScreenButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var contentScrollView: ScrollView

    private var isFullScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secure_browser)

        // --- View Initialization ---
        urlInput = findViewById(R.id.urlInput)
        scanButton = findViewById(R.id.scanButton)
        historyButton = findViewById(R.id.historyButton)
        fullScreenButton = findViewById(R.id.fullScreenButton)
        resultText = findViewById(R.id.resultText)
        webView = findViewById(R.id.secureWebView)
        progressBar = findViewById(R.id.progressBar)
        contentScrollView = findViewById(R.id.contentScrollView)

        setupWebView()
        setupClickListeners()

        handleIncomingLink(intent)
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true // Essential for modern web
        webView.settings.allowFileAccess = false   // Security hardening
        webView.settings.allowContentAccess = false // Security hardening

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.url?.toString()?.let { startHybridScan(normalizeUrl(it)) }
                return true // We are handling all URL loads
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.cancel() // Block insecure connections
                showErrorResult("SSL Error: The website\'s security certificate is invalid. Access blocked.")
            }
        }
    }

    private fun setupClickListeners() {
        scanButton.setOnClickListener {
            val rawUrl = urlInput.text.toString().trim()
            if (rawUrl.isNotEmpty()) {
                startHybridScan(normalizeUrl(rawUrl))
            }
        }

        historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        fullScreenButton.setOnClickListener { enterFullScreen() }
    }

    private fun handleIncomingLink(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.toString()?.let {
                urlInput.setText(it)
                startHybridScan(normalizeUrl(it))
            }
        }
    }

    private fun startHybridScan(url: String) {
        progressBar.visibility = View.VISIBLE
        resultText.text = "Scanning with Hybrid AI...\n$url"
        resultText.setTextColor(ContextCompat.getColor(this, R.color.grey))
        webView.loadUrl("about:blank")

        lifecycleScope.launch {
            // Run both Cloud AI and On-Device AI in parallel
            val cloudResultDeferred = async(Dispatchers.IO) { getCloudDecision(url) }
            val localResultDeferred = async(Dispatchers.IO) { PhishAi.analyze(url, "") }

            val cloudResult = cloudResultDeferred.await()
            val localResult = localResultDeferred.await()

            applyHybridDecision(url, cloudResult, localResult)
        }
    }

    private suspend fun getCloudDecision(url: String): com.threatsurface.shield.net.UrlCheckResponse? {
        return try {
            ApiClient.api.checkUrl(UrlCheckRequest(url))
        } catch (e: Exception) {
            null // Network error or server unreachable
        }
    }

    private fun applyHybridDecision(url: String, cloud: com.threatsurface.shield.net.UrlCheckResponse?, local: ScanDecision) {
        // --- Fusion Logic: Stricter policy --- 
        val isCloudPhishing = cloud?.label == "PHISHING"
        val isLocalPhishing = local.verdict == "HIGH"

        // If EITHER engine detects high risk, we block.
        if (isCloudPhishing || isLocalPhishing) {
            val reason = if (isCloudPhishing) "Cloud AI" else "On-Device Engine"
            HistoryManager.addHistory(HistoryItem(url, "HIGH", 95, System.currentTimeMillis()))
            Toast.makeText(this, "Hybrid AI: Phishing link blocked by $reason", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // If we reach here, neither engine flagged it as high-risk.
        // We can show the cloud result if available, otherwise the local one.
        if (cloud != null && cloud.error == null) {
            val phishProb = cloud.probs?.phishing ?: 0.0
            val score = (phishProb * 100).toInt()
            showSuccessResult(url, "Cloud AI: ${cloud.label}", score, cloud.explanation ?: "No details.")
            HistoryManager.addHistory(HistoryItem(url, cloud.label ?: "UNKNOWN", score, System.currentTimeMillis()))
        } else {
            showSuccessResult(url, "Local Engine: ${local.verdict}", local.finalScore, local.reasons.joinToString("\n"))
            HistoryManager.addHistory(HistoryItem(url, local.verdict, local.finalScore, System.currentTimeMillis()))
        }
    }

    private fun showSuccessResult(url: String, verdict: String, score: Int, explanation: String) {
        progressBar.visibility = View.GONE
        resultText.text = "✅ $verdict\nRisk Score: $score\n\n$explanation"
        resultText.setTextColor(ContextCompat.getColor(this, R.color.green))
        webView.loadUrl(url)
        fullScreenButton.visibility = View.VISIBLE
    }

    private fun showErrorResult(message: String) {
        progressBar.visibility = View.GONE
        resultText.text = "⚠️ $message"
        resultText.setTextColor(ContextCompat.getColor(this, R.color.red))
        fullScreenButton.visibility = View.GONE
    }

    private fun normalizeUrl(raw: String): String {
        return if (!raw.startsWith("http://") && !raw.startsWith("https://")) "https://$raw" else raw.trim()
    }
    
    // region Fullscreen & Lifecycle Logic
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else if (isFullScreen) exitFullScreen()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.clearCache(true)
        webView.destroy()
    }

    private fun enterFullScreen() {
        isFullScreen = true
        contentScrollView.visibility = View.GONE
        val params = webView.layoutParams as RelativeLayout.LayoutParams
        params.removeRule(RelativeLayout.BELOW)
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        webView.layoutParams = params
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun exitFullScreen() {
        isFullScreen = false
        contentScrollView.visibility = View.VISIBLE
        val params = webView.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.BELOW, R.id.contentScrollView)
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        webView.layoutParams = params
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
    // endregion
}
