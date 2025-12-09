package com.vishnu.threatsurfaceshield

object LastStateStore {
    var lastPhishDecision: ScanDecision? = null
    var lastMalwareList: List<AppRisk> = emptyList()
    var lastMaxPrivacyScore: Int = 0
    var lastWifiRiskLevel: String? = null
}
