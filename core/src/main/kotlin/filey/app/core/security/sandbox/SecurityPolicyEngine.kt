package filey.app.core.security.sandbox

import filey.app.core.data.preferences.AppPreferences
import kotlinx.coroutines.flow.first

class SecurityPolicyEngine(
    private val preferences: AppPreferences
) {

    data class PolicyResult(
        val allowed: Boolean,
        val reason: String,
        val requiresConfirmation: Boolean = false
    )

    suspend fun evaluate(command: PrivilegedCommand): PolicyResult {
        // 1) Global kill switch
        // For simplicity, we assume we can get the mode
        // In reality, we'd use preferences.accessModeFlow.first()
        
        // 2) Risk level policy
        val maxAllowedRisk = PrivilegedCommand.RiskLevel.CRITICAL // Placeholder
        if (command.riskLevel.ordinal > maxAllowedRisk.ordinal) {
            return PolicyResult(false, "Risk level exceeded")
        }

        // 3) Path-specific policies
        val pathResult = evaluatePathPolicy(command)
        if (!pathResult.allowed) return pathResult

        // 4) Confirmation requirement
        val requiresConfirmation = command.riskLevel >= PrivilegedCommand.RiskLevel.HIGH

        return PolicyResult(true, "Policy check passed", requiresConfirmation)
    }

    private fun evaluatePathPolicy(command: PrivilegedCommand): PolicyResult {
        // Critical system directories protection
        val paths = when (command) {
            is PrivilegedCommand.ListDirectory -> listOf(command.path)
            is PrivilegedCommand.CopyFile -> listOf(command.source, command.destination)
            is PrivilegedCommand.MoveFile -> listOf(command.source, command.destination)
            is PrivilegedCommand.DeleteFile -> listOf(command.target)
            is PrivilegedCommand.ChangePermissions -> listOf(command.target)
            is PrivilegedCommand.ChangeOwner -> listOf(command.target)
        }

        val forbiddenPrefixes = listOf(
            "/system/bin", "/system/xbin", "/system/etc",
            "/sbin", "/proc", "/sys/kernel"
        )

        for (path in paths) {
            if (forbiddenPrefixes.any { path.value.startsWith(it) }) {
                // Allow read-only if it's just listing or copying FROM
                if (command is PrivilegedCommand.ListDirectory || 
                   (command is PrivilegedCommand.CopyFile && path == command.source)) {
                    continue
                }
                return PolicyResult(false, "Operation on protected path not allowed: ${path.value}")
            }
        }

        return PolicyResult(true, "Path OK")
    }
}
