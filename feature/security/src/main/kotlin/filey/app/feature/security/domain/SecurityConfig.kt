package filey.app.feature.security.domain

enum class AccessMode {
    NORMAL, ROOT, SHIZUKU
}

interface UserPreferences {
    fun isPrivilegedModeEnabled(): Boolean
    fun getMaxRiskLevel(): PrivilegedCommand.RiskLevel
}
