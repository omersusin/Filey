package filey.app.feature.security.data

import filey.app.feature.security.domain.PrivilegedCommand
import filey.app.feature.security.domain.PrivilegedCommand.RiskLevel
import timber.log.Timber

class AuditLogger(
    private val auditDao: AuditDao
) {
    suspend fun logDenied(command: PrivilegedCommand, reason: String) =
        log(command, "DENIED", reason)

    suspend fun logAllowed(command: PrivilegedCommand) =
        log(command, "ALLOWED", "Policy check passed")

    suspend fun logExecuted(command: PrivilegedCommand, timeMs: Long) =
        log(command, "EXECUTED", "", timeMs)

    suspend fun logError(command: PrivilegedCommand, error: Throwable) =
        log(command, "ERROR", "", errorMessage = error.message)

    private suspend fun log(
        command: PrivilegedCommand,
        action: String,
        reason: String,
        executionTimeMs: Long = 0,
        errorMessage: String? = null
    ) {
        auditDao.insert(
            AuditEntry(
                commandType = command::class.simpleName ?: "Unknown",
                commandDetails = sanitizeForLog(command),
                riskLevel = command.riskLevel.name,
                action = action,
                reason = reason,
                executionTimeMs = executionTimeMs,
                errorMessage = errorMessage
            )
        )
        if (command.riskLevel >= RiskLevel.HIGH) {
            Timber.w("AUDIT [$action]: ${command::class.simpleName} risk=${command.riskLevel} reason=$reason")
        }
    }

    private fun sanitizeForLog(command: PrivilegedCommand): String = when (command) {
        is PrivilegedCommand.DeleteFile ->
            "target=${command.target.value.takeLast(50)}, recursive=${command.recursive}"
        is PrivilegedCommand.ChangeOwner ->
            "target=***, owner=${command.owner.name}"
        else -> command.toString().take(200)
    }
}
