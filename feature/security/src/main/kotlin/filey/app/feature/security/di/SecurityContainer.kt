package filey.app.feature.security.di

import android.content.Context
import filey.app.feature.security.data.AuditDatabase
import filey.app.feature.security.data.AuditLogger
import filey.app.feature.security.data.RootCommandExecutor
import filey.app.feature.security.data.SandboxedCommandExecutor
import filey.app.feature.security.data.ShizukuCommandExecutor
import filey.app.feature.security.domain.AccessMode
import filey.app.feature.security.domain.SecurityConfig
import filey.app.feature.security.domain.SecurityPolicyEngine
import javax.inject.Provider

class SecurityContainer(context: Context) {
    private val db = AuditDatabase.getInstance(context)
    val auditLogger = AuditLogger(db.auditDao())
    private val userPreferences = SecurityConfig.DefaultUserPreferences()
    val policyEngine = SecurityPolicyEngine(auditLogger, userPreferences)
    val rootExecutor = RootCommandExecutor()
    val shizukuExecutor = ShizukuCommandExecutor()
    private var currentMode: AccessMode = AccessMode.NORMAL
    val sandboxedExecutor = SandboxedCommandExecutor(
        policyEngine, rootExecutor, shizukuExecutor, auditLogger, Provider { currentMode }
    )
    fun setAccessMode(mode: AccessMode) { currentMode = mode }
    companion object {
        @Volatile private var INSTANCE: SecurityContainer? = null
        fun getInstance(context: Context): SecurityContainer =
            INSTANCE ?: synchronized(this) { INSTANCE ?: SecurityContainer(context).also { INSTANCE = it } }
    }
}
