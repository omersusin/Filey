package filey.app.feature.security.domain

enum class AccessMode { NORMAL, ROOT, SHIZUKU }

interface UserPreferences {
    fun isPrivilegedModeEnabled(): Boolean
    fun getMaxRiskLevel(): PrivilegedCommand.RiskLevel
}

object SecurityConfig {
    class DefaultUserPreferences(
        private val privilegedEnabled: Boolean = false,
        private val maxRisk: PrivilegedCommand.RiskLevel = PrivilegedCommand.RiskLevel.MEDIUM
    ) : UserPreferences {
        override fun isPrivilegedModeEnabled() = privilegedEnabled
        override fun getMaxRiskLevel() = maxRisk
    }

    class PrivilegedUserPreferences : UserPreferences {
        override fun isPrivilegedModeEnabled() = true
        override fun getMaxRiskLevel() = PrivilegedCommand.RiskLevel.HIGH
    }
}
