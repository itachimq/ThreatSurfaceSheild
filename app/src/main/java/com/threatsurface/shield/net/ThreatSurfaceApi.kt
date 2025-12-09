package com.threatsurface.shield.net

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

data class UrlCheckRequest(
    val url: String
)

data class UrlProbs(
    val benign: Double? = null,
    val phishing: Double? = null,
    val malware: Double? = null,
    @SerializedName("rf_struct")
    val rfStruct: Double? = null
)

data class UrlCheckResponse(
    val mode: String? = null,
    val probs: UrlProbs? = null,
    @SerializedName("fused_prob")
    val fusedProb: Double? = null,
    val label: String? = null,          // "PHISHING" or "BENIGN"
    val explanation: String? = null,
    val source: String? = null,         // "model" or "blocklist"
    val error: String? = null           // Error message from backend
)

interface ThreatSurfaceApi {
    @POST("/api/url_check")
    suspend fun checkUrl(@Body body: UrlCheckRequest): UrlCheckResponse
}
