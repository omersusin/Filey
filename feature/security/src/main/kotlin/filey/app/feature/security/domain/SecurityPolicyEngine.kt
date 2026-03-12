package filey.app.feature.security.domain

import filey.app.feature.security.data.AuditLogger
import filey.app.feature.security.domain.PrivilegedCommand.RiskLevel

class SecurityPolicyEngine(
    private val auditLogger: AuditLogger,
    private val userPreferences: UserPreferences
) {
    data class PolicyResult(
        val allowed: Boolean,
        val reason: String,
        val requiresConfirmation: Boolean = false
    )

    suspend fun evaluate(command: PrivilegedCommand): PolicyResult {
        if (!userPreferences.isPrivilegedModeEnabled()) {
            return PolicyResult(false, "Privileged mode is disabled")
        }
        val maxAllowedRisk = userPreferences.getMaxRiskLevel()
        if (command.riskLevel.ordinal > maxAllowedRisk.ordinal) {
            auditLogger.logDenied(command, "Risk level exceeded")
            return PolicyResult(false, "Operation risk (${command.riskLevel}) exceeds policy ($maxAllowedRisk)")
        }
        val pathResult = evaluatePathPolicy(command)
        if (!pathResult.allowed) return pathResult

        val rateLimitResult = evaluateRateLimit(command)
        if (!rateLimitResult.allowed) return rateLimitResult

        auditLogger.logAllowed(command)
        return PolicyResult(
            allowed = true,
            reason = "Policy check passed",
            requiresConfirmation = command.riskLevel >= RiskLevel.HIGH
        )
    }

    private fun evaluatePathPolicy(command: PrivilegedCommand): PolicyResult {
        val paths = when (command) {
            is PrivilegedCommand.CopyFile          -> listOf(command.source, command.destination)
            is PrivilegedCommand.MoveFile          -> listOf(command.source, command.destination)
            is PrivilegedCommand.DeleteFile        -> listOf(command.target)
            is PrivilegedCommand.ChangePermissions -> listOf(command.target)
            is PrivilegedCommand.ChangeOwner       -> listOf(command.target)
            is PrivilegedCommand.ReadSystemFile    -> listOf(command.path)
            else -> emptyList()
        }
        val writableRoots = listOf("/storage/emulated/0", "/sdcard", "/data/media/0", "/mnt/media_rw")
        val writeCommands = setOf(
            PrivilegedCommand.MoveFile::class, PrivilegedCommand.DeleteFile::class,
            PrivilegedCommand.ChangePermissions::class, PrivilegedCommand.ChangeOwner::class
        )
        if (command::class in writeCommands) {
            for (path in paths) {
                if (writableRoots.none { path.value.startsWith(it) }) {
                    return PolicyResult(false, "Write not allowed on: ${path.value}")
                }
            }
        }
        return PolicyResult(true, "Path OK")
    }

    private val commandHistory = ArrayDeque<Pair<Long, RiskLevel>>(100)

    private fun evaluateRateLimit(command: PrivilegedCommand): PolicyResult {
        val now = System.currentTimeMillis()
        while (commandHistory.isNotEmpty() && now - commandHistory.first().first > 60_000L)
            commandHistory.removeFirst()
        val highRiskCount = commandHistory.count { it.second >= RiskLevel.HIGH }
        if (highRiskCount >= 10 && command.riskLevel >= RiskLevel.HIGH) {
            return PolicyResult(false, "Rate limit exceeded: $highRiskCount high-risk ops in last minute")
        }
        commandHistory.addLast(now to command.riskLevel)
        return PolicyResult(true, "Rate OK")
    }
}
