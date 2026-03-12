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

class SecurityContainer(context: Context) {
    private val db            = AuditDatabase.getInstance(context)
    val auditLogger           = AuditLogger(db.auditDao())
    private val userPrefs     = SecurityConfig.DefaultUserPreferences()
    val policyEngine          = SecurityPolicyEngine(auditLogger, userPrefs)
    val rootExecutor          = RootCommandExecutor()
    val shizukuExecutor       = ShizukuCommandExecutor()

    private var currentMode: AccessMode = AccessMode.NORMAL

    val sandboxedExecutor = SandboxedCommandExecutor(
        policyEngine    = policyEngine,
        rootExecutor    = rootExecutor,
        shizukuExecutor = shizukuExecutor,
        auditLogger     = auditLogger,
        accessMode      = { currentMode }    // lambda instead of Provider<>
    )

    fun setAccessMode(mode: AccessMode) { currentMode = mode }

    companion object {
        @Volatile private var INSTANCE: SecurityContainer? = null
        fun getInstance(context: Context): SecurityContainer =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityContainer(context).also { INSTANCE = it }
            }
    }
}
