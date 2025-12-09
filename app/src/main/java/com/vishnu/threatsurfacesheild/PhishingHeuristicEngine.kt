package com.vishnu.threatsurfaceshield

import kotlin.math.ln
import java.net.URI
import java.util.Locale

/**
 * Kotlin port of your phishing.py heuristic engine.
 * Uses: suspicious domain, forms, password/email fields, keywords, JS, entropy.
 */
object PhishingHeuristicEngine {

    data class Page(
        val url: String,
        val html: String
    )

    data class Result(
        val score: Int,          // 0..100
        val reason: String,      // e.g. "cloudflare_tunnel,form_detected,password_field"
        val components: Map<String, Int>
    )

    fun scorePage(page: Page): Result {
        val url = (page.url.trim())
        val html = (page.html ?: "").ifEmpty { "" }
        val urlLower = url.lowercase(Locale.ROOT)
        val htmlLower = html.lowercase(Locale.ROOT)

        val components = mutableMapOf<String, Int>()
        val reasons = mutableListOf<String>()
        var score = 0

        // 1) suspicious domain heuristics (Cloudflare tunnel etc)
        val domScore = suspiciousDomainScore(url)
        if (domScore > 0) {
            components["domain_score"] = domScore
            score += domScore
            if (urlLower.contains("trycloudflare.com")) {
                reasons.add("cloudflare_tunnel")
            }
        }

        // 2) form detection (we only have raw HTML here)
        var hasForm = false
        if (htmlLower.contains("<form")) {
            hasForm = true
        }
        if (hasForm) {
            components["form_detected"] = 30
            score += 30
            reasons.add("form_detected")
        }

        // 3) password/email detection in HTML content
        var passwordPresent = false
        var emailPresent = false

        if (htmlLower.contains("type=\"password\"") || htmlLower.contains("type='password'") || htmlLower.contains("password")) {
            passwordPresent = true
        }
        if (
            htmlLower.contains("type=\"email\"") ||
            htmlLower.contains("type='email'") ||
            htmlLower.contains("email") ||
            htmlLower.contains("e-mail")
        ) {
            emailPresent = true
        }

        if (passwordPresent) {
            components["password_field"] = 40
            score += 40
            reasons.add("password_field")
        }
        if (emailPresent) {
            components["email_field"] = 10
            score += 10
            reasons.add("email_field")
        }

        // 4) keyword scan (suspicious phrases)
        val keywords = listOf(
            "login",
            "sign in",
            "signin",
            "verify",
            "account",
            "secure",
            "bank",
            "otp",
            "one-time",
            "authenticate",
            "re-enter",
            "password recovery"
        )
        var kwCount = 0
        for (k in keywords) {
            var index = htmlLower.indexOf(k)
            while (index >= 0) {
                kwCount += 1
                index = htmlLower.indexOf(k, index + k.length)
            }
        }
        if (kwCount > 0) {
            val kwScore = minOf(20, kwCount * 5)
            components["keyword_score"] = kwScore
            score += kwScore
            reasons.add("keywords")
        }

        // 5) suspicious JavaScript
        var jsScore = 0
        if (
            htmlLower.contains("eval(") ||
            htmlLower.contains("atob(") ||
            htmlLower.contains("fromcharcode") ||
            htmlLower.contains("document.write")
        ) {
            jsScore = 15
            components["js_score"] = jsScore
            score += jsScore
            reasons.add("suspicious_js")
        }

        // 6) entropy-based score
        val ent = if (htmlLower.isNotEmpty()) {
            (entropy(htmlLower) * 10).toInt()
        } else {
            0
        }
        val entScore = minOf(20, ent)
        components["entropy_score"] = entScore
        score += entScore

        // cap score
        val finalScore = score.coerceIn(0, 100)
        val reasonText = if (reasons.isEmpty()) "low_risk" else reasons.joinToString(",")

        return Result(
            score = finalScore,
            reason = reasonText,
            components = components
        )
    }

    // --- helpers ---

    private fun suspiciousDomainScore(url: String): Int {
        val host = try {
            val u = URI(url)
            (u.host ?: url)
        } catch (e: Exception) {
            url
        }.lowercase(Locale.ROOT).let { rawHost ->
            // fallback similar to your regex stripping
            rawHost.replace(Regex("^.*?://"), "").split("/")[0]
        }

        var score = 0

        // many hyphens
        val hyphens = host.count { it == '-' }
        if (hyphens >= 2) {
            score += minOf(20, hyphens * 6)
        }

        // many segments
        val segs = host.split('.')
        if (segs.size >= 4) {
            score += 10
        }

        // odd length
        if (host.length > 30) {
            score += 10
        }

        // trycloudflare special
        if (host.contains("trycloudflare.com")) {
            score += 60
        }

        // IP address disguised as hostname
        if (Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$").matches(host)) {
            score += 20
        }

        return score
    }

    private fun entropy(s: String): Double {
        if (s.isEmpty()) return 0.0
        val counts = s.groupingBy { it }.eachCount()
        val len = s.length.toDouble()
        var sum = 0.0
        for (c in counts.values) {
            val p = c / len
            // log2(p) = ln(p)/ln(2)
            sum += -p * (ln(p) / ln(2.0))
        }
        return sum
    }
}