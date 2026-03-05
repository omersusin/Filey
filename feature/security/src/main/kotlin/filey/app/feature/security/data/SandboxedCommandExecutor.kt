package filey.app.feature.security.data

import filey.app.feature.security.domain.AccessMode
import filey.app.feature.security.domain.PrivilegedCommand
import filey.app.feature.security.domain.SecurityPolicyEngine
import javax.inject.Inject
import javax.inject.Provider

interface CommandExecutor {
    suspend fun execute(command: String): String
}

class RootCommandExecutor @Inject constructor() : CommandExecutor {
    override suspend fun execute(command: String): String {
        // Placeholder for actual root execution
        return "Root execution not implemented yet"
    }
}

class ShizukuCommandExecutor @Inject constructor() : CommandExecutor {
    override suspend fun execute(command: String): String {
        // Placeholder for actual Shizuku execution
        return "Shizuku execution not implemented yet"
    }
}

class SandboxedCommandExecutor @Inject constructor(
    private val policyEngine: SecurityPolicyEngine,
    private val rootExecutor: RootCommandExecutor,
    private val shizukuExecutor: ShizukuCommandExecutor,
    private val auditLogger: AuditLogger,
    private val accessMode: Provider<AccessMode>
) {

    sealed class ExecutionResult {
        data class Success(
            val output: String = "",
            val executionTimeMs: Long = 0
        ) : ExecutionResult()

        data class Denied(
            val reason: String
        ) : ExecutionResult()

        data class Error(
            val exception: Throwable,
            val stderr: String = ""
        ) : ExecutionResult()
    }

    suspend fun execute(command: PrivilegedCommand): ExecutionResult {
        val policy = policyEngine.evaluate(command)
        if (!policy.allowed) {
            auditLogger.logDenied(command, policy.reason)
            return ExecutionResult.Denied(policy.reason)
        }

        val shellCommand = compileCommand(command)
        val startTime = System.currentTimeMillis()

        return try {
            val output = when (accessMode.get()) {
                AccessMode.ROOT -> rootExecutor.execute(shellCommand)
                AccessMode.SHIZUKU -> shizukuExecutor.execute(shellCommand)
                AccessMode.NORMAL -> throw SecurityException(
                    "Command requires elevated privileges"
                )
            }

            val elapsed = System.currentTimeMillis() - startTime
            auditLogger.logExecuted(command, elapsed)

            ExecutionResult.Success(
                output = output,
                executionTimeMs = elapsed
            )
        } catch (e: Exception) {
            auditLogger.logError(command, e)
            ExecutionResult.Error(e)
        }
    }

    private fun compileCommand(command: PrivilegedCommand): String {
        return when (command) {
            is PrivilegedCommand.ListDirectory -> {
                val flags = if (command.showHidden) "-la" else "-l"
                "ls $flags ${command.path.shellEscaped()}"
            }
            is PrivilegedCommand.CopyFile -> {
                "cp -f ${command.source.shellEscaped()} ${command.destination.shellEscaped()}"
            }
            is PrivilegedCommand.MoveFile -> {
                "mv -f ${command.source.shellEscaped()} ${command.destination.shellEscaped()}"
            }
            is PrivilegedCommand.DeleteFile -> {
                val flags = if (command.recursive) "-rf" else "-f"
                "rm $flags ${command.target.shellEscaped()}"
            }
            is PrivilegedCommand.ChangePermissions -> {
                "chmod ${command.permissions.toOctalString()} ${command.target.shellEscaped()}"
            }
            is PrivilegedCommand.ChangeOwner -> {
                "chown ${command.owner.name}:${command.group.name} ${command.target.shellEscaped()}"
            }
            is PrivilegedCommand.ReadSystemFile -> {
                "cat ${command.path.shellEscaped()}"
            }
            is PrivilegedCommand.MountFilesystem -> {
                "mount -o ${command.options.toFlags()} " +
                        "${command.device.shellEscaped()} ${command.mountPoint.shellEscaped()}"
            }
        }
    }
}
