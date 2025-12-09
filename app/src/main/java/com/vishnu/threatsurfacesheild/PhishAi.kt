package com.vishnu.threatsurfaceshield

import java.net.URI
import kotlin.math.log2
import java.util.Locale

// What the AI decided
enum class Action {
    ALLOW,
    WARN,
    BLOCK
}

data class ScanDecision(
    val action: Action,
    val finalScore: Int,        // 0..100
    val verdict: String,        // "LOW", "MEDIUM", "HIGH"
    val reasons: List<String>   // list of signals that contributed
)

object PhishAi {

    // Big legit sites – strongly favoured to ALLOW
    private val whitelistDomains = setOf(
        "google.com", "www.google.com",
        "instagram.com", "www.instagram.com",
        "youtube.com", "www.youtube.com",
        "facebook.com", "www.facebook.com",
        "whatsapp.com", "www.whatsapp.com",
        "twitter.com", "x.com",
        "microsoft.com", "apple.com",
        "amazon.com", "www.amazon.com"
    )

    // Known phishing / training samples → always BLOCK
    private val blacklistDomains = setOf(
        // Your old lab tunnels
        "bomb-quotes-heads-performances.trycloudflare.com",
        "corporations-defeat-expiration-solomon.trycloudflare.com",
        "listings-rates-boost-cleanup.trycloudflare.com",

        // 10 phishing samples you provided
        "instagram-login-secure.example",
        "accounts-google-security.example",
        "facebook.verify-user.example",
        "paypal.confirm-update-account.example",
        "linkedin-auth.example",
        "snapchat.recovery-account.example",
        "tiktok-followers-free.example",
        "whatsapp-web-confirm.example",
        "twitter-verification.example",
        "secure-amazon-prime.example"
    )

    // Suspicious cheap/abused TLDs
    private val suspiciousTlds = setOf(
        "tk", "ml", "ga", "cf", "gq", "xyz", "top", "click",
        "live", "vip", "cam", "fun", "shop", "loan"
    )

    // Brand names we care about
    private val brandKeywords = listOf(
        "google", "facebook", "instagram", "whatsapp", "microsoft", "apple",
        "netflix", "paypal", "amazon", "steam", "bank", "flipkart", "phonepe",
        "paytm", "snapchat", "tiktok", "linkedin", "twitter"
    )

    // Phishy intent words
    private val phishWords = listOf(
        "login", "log in", "sign in", "signin", "verify", "verification", "account",
        "secure", "security", "update", "reactivate", "unlock", "otp", "one-time",
        "refund", "support", "billing", "session", "recovery"
    )

    fun analyze(url: String, html: String): ScanDecision {
        val reasons = mutableListOf<String>()
        val htmlLower = html.lowercase(Locale.ROOT)
        val host = extractHost(url)

        // 0) HARD WHITELIST
        if (isWhitelisted(host)) {
            return ScanDecision(
                action = Action.ALLOW,
                finalScore = 0,
                verdict = "LOW",
                reasons = listOf("whitelisted_domain:$host")
            )
        }

        // 0b) HARD BLACKLIST
        if (isBlacklisted(host)) {
            return ScanDecision(
                action = Action.BLOCK,
                finalScore = 100,
                verdict = "HIGH",
                reasons = listOf("blacklisted_domain:$host")
            )
        }

        // 1) DOMAIN HEURISTICS
        val domainScore = suspiciousDomainScore(host, url, reasons)

        // 2) HTML HEURISTICS – forms, password, email, keywords, JS, entropy
        val htmlScore = htmlHeuristicScore(htmlLower, reasons)

        // 3) NUMERIC FEATURES → extra score
        val numericScore = numericFeatureScore(url, htmlLower, reasons)

        // 4) BRAND MISMATCH – brand in HTML but not in domain
        val brandScore = brandMismatchScore(host, htmlLower, reasons)

        // 5) HTTP vs HTTPS
        val protocolScore = protocolScore(url, reasons)

        // 6) URL PARAM CLUTTER (many = & ? etc.)
        val paramScore = paramNoiseScore(url, reasons)

        // 7) HOMOGLYPH DETECTION (new)
        val homoglyphScore = homoglyphScore(host, reasons)

        var rawScore =
            domainScore +
            htmlScore +
            numericScore +
            brandScore +
            protocolScore +
            paramScore +
            homoglyphScore

        // 8) ORIGINAL LINK PATTERN
        if (isOriginalLinkPattern(host)) {
            rawScore -= 20
            reasons.add("original_link_pattern_detected")
        }

        // Clamp 0..100
        rawScore = rawScore.coerceIn(0, 100)

        // Extra boost if credentials on suspicious domain
        if (rawScore in 40..70 && reasons.any { it.contains("password") || it.contains("form_detected") }) {
            rawScore = (rawScore + 10).coerceAtMost(100)
            reasons.add("boosted_for_credentials_on_suspicious_domain")
        }

        val verdict = when {
            rawScore >= 70 -> "HIGH"
            rawScore >= 40 -> "MEDIUM"
            else -> "LOW"
        }

        val action = when {
            rawScore >= 70 -> Action.BLOCK   // strong phish → block
            rawScore >= 40 -> Action.WARN    // medium risk → warn but allow
            else -> Action.ALLOW
        }

        return ScanDecision(
            action = action,
            finalScore = rawScore,
            verdict = verdict,
            reasons = reasons
        )
    }

    // ================= helpers ===================

    private fun extractHost(url: String): String {
        return try {
            (URI(url).host ?: url).lowercase(Locale.ROOT)
        } catch (e: Exception) {
            url.lowercase(Locale.ROOT)
        }
    }

    private fun isWhitelisted(host: String): Boolean {
        return whitelistDomains.any { host == it || host.endsWith(".$it") }
    }

    private fun isBlacklisted(host: String): Boolean {
        return blacklistDomains.any { host.equals(it, ignoreCase = true) }
    }

    private fun isOriginalLinkPattern(host: String): Boolean {
        val pattern = Regex("""^(www\.[a-zA-Z0-9-]+\.com|app\.[a-zA-Z0-9-]+\.in|app\.[a-zA-Z0-9-]+\.com|app\.[a-zA-Z0-9-]+\.org)$""")
        return pattern.matches(host)
    }

    private fun homoglyphScore(host: String, reasons: MutableList<String>): Int {
        val nonAsciiChars = host.filter { it.code !in 0..127 }
        if (nonAsciiChars.isNotEmpty()) {
            reasons.add("homoglyph_detected:${nonAsciiChars}")
            return 30 // Strong signal for non-ASCII characters in domain
        }
        return 0
    }

    private fun suspiciousDomainScore(
        host: String,
        url: String,
        reasons: MutableList<String>
    ): Int {
        var score = 0

        val hyphens = host.count { it == '-' }
        if (hyphens >= 2) {
            score += minOf(20, hyphens * 6)
            reasons.add("many_hyphens:$hyphens")
        }

        val segments = host.split('.')
        if (segments.size >= 4) {
            score += 10
            reasons.add("many_subdomains:${segments.size}")
        }

        if (host.length > 30) {
            score += 10
            reasons.add("long_host:${host.length}")
        }

        if (host.contains("trycloudflare.com")) {
            score += 40
            reasons.add("cloudflare_tunnel")
        }

        val ipRegex = Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$")
        if (ipRegex.matches(host)) {
            score += 20
            reasons.add("ip_like_domain")
        }

        val tld = host.substringAfterLast('.', "")
        if (tld in suspiciousTlds) {
            score += 15
            reasons.add("suspicious_tld:$tld")
        }

        // very long URL overall
        if (url.length > 120) {
            score += 10
            reasons.add("long_url:${url.length}")
        }

        // brand + auth/login/secure words directly in the domain
        val domainAuthWords = listOf("login", "secure", "verify", "auth", "confirm", "recovery", "update", "account")
        for (b in brandKeywords) {
            if (host.contains(b)) {
                for (w in domainAuthWords) {
                    if (host.contains(w)) {
                        score += 20
                        reasons.add("brand+${w}_in_domain:$b")
                        break
                    }
                }
            }
        }

        return score
    }

    private fun htmlHeuristicScore(
        htmlLower: String,
        reasons: MutableList<String>
    ): Int {
        var score = 0

        val hasForm = "<form" in htmlLower
        if (hasForm) {
            score += 25
            reasons.add("form_detected")
        }

        var passwordPresent = false
        var emailPresent = false

        if ("type=\"password\"" in htmlLower ||
            "type='password'" in htmlLower ||
            "password" in htmlLower
        ) {
            passwordPresent = true
        }

        if ("type=\"email\"" in htmlLower ||
            "type='email'" in htmlLower ||
            "email" in htmlLower ||
            "e-mail" in htmlLower
        ) {
            emailPresent = true
        }

        if (passwordPresent) {
            score += 40
            reasons.add("password_field")
        }
        if (emailPresent) {
            score += 10
            reasons.add("email_field")
        }

        // phishing text keywords
        var kwCount = 0
        for (k in phishWords) {
            var idx = htmlLower.indexOf(k)
            while (idx >= 0) {
                kwCount++
                idx = htmlLower.indexOf(k, idx + k.length)
            }
        }
        if (kwCount > 0) {
            val kwScore = minOf(25, kwCount * 4)
            score += kwScore
            reasons.add("phishing_keywords:$kwCount")
        }

        // suspicious JS
        if (htmlLower.contains("eval(") ||
            htmlLower.contains("atob(") ||
            htmlLower.contains("fromcharcode") ||
            htmlLower.contains("document.write")
        ) {
            score += 15
            reasons.add("suspicious_js")
        }

        // entropy
        if (htmlLower.isNotEmpty()) {
            val ent = entropy(htmlLower)
            val entScore = minOf(20, (ent * 2).toInt())
            score += entScore
            reasons.add("entropy:${String.format("%.2f", ent)}")
        }

        return score
    }

    private fun numericFeatureScore(
        url: String,
        htmlLower: String,
        reasons: MutableList<String>
    ): Int {
        val host = extractHost(url)

        val numHyphens = host.count { it == '-' }
        val numDots = host.count { it == '.' }
        val length = url.length
        val hostLen = host.length
        val digits = url.count { it.isDigit() }
        val digitRatio = if (length > 0) digits.toFloat() / length.toFloat() else 0f
        val ipLike = if (Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$").matches(host)) 1f else 0f
        val manySegs = if (numDots >= 3) 1f else 0f

        val tld = host.substringAfterLast('.', "")
        val isCom = if (tld == "com") 1f else 0f
        val common = setOf("com", "org", "net", "edu", "gov", "io", "in", "co")
        val uncommonTld = if (tld.isNotEmpty() && !common.contains(tld)) 1f else 0f

        val hasForm = if (htmlLower.contains("<form")) 1f else 0f
        val hasPass = if (htmlLower.contains("type=\"password\"") || htmlLower.contains("password")) 1f else 0f

        val keywordCount = phishWords.sumOf { k ->
            var c = 0
            var idx = htmlLower.indexOf(k)
            while (idx >= 0) {
                c++
                idx = htmlLower.indexOf(k, idx + k.length)
            }
            c
        }.toFloat()

        val jsSusp = if (htmlLower.contains("eval(") ||
            htmlLower.contains("atob(") ||
            htmlLower.contains("fromcharcode")
        ) 1f else 0f

        val ent = if (htmlLower.isNotEmpty()) entropy(htmlLower).toFloat() else 0f

        var score = 0f

        // url numeric features
        score += (length / 300f).coerceAtMost(15f)
        score += (hostLen / 80f).coerceAtMost(10f)
        score += numHyphens * 3f
        score += numDots * 2f
        score += digitRatio * 30f
        score += ipLike * 25f
        score += manySegs * 10f
        score += uncommonTld * 15f
        score -= isCom * 5f // ".com" slightly safer

        // html numeric features
        score += hasForm * 10f
        score += hasPass * 15f
        score += (keywordCount / 3f).coerceAtMost(15f)
        score += jsSusp * 10f
        score += (ent / 3f).coerceAtMost(10f)

        val final = score.coerceIn(0f, 40f) // cap numeric part to 40
        if (final > 0f) {
            reasons.add("numeric_features_score:${final.toInt()}")
        }
        return final.toInt()
    }

    private fun brandMismatchScore(
        host: String,
        htmlLower: String,
        reasons: MutableList<String>
    ): Int {
        var score = 0
        for (b in brandKeywords) {
            if (htmlLower.contains(b)) {
                if (!host.contains(b)) {
                    score += 15
                    reasons.add("brand_mismatch:$b")
                }
            }
        }
        return score
    }

    private fun protocolScore(url: String, reasons: MutableList<String>): Int {
        return if (url.startsWith("http://")) {
            reasons.add("plain_http")
            15
        } else 0
    }

    private fun paramNoiseScore(url: String, reasons: MutableList<String>): Int {
        val paramChars = url.count { it == '&' || it == '=' || it == '?' || it == '%' }
        var score = 0
        if (paramChars >= 5) {
            score += 5
        }
        if (paramChars >= 10) {
            score += 5
        }
        if (score > 0) {
            reasons.add("noisy_params:$paramChars")
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
            sum += -p * log2(p)
        }
        return sum
    }
}
