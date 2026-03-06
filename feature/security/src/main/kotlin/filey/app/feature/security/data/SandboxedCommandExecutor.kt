package filey.app.feature.security.data

import com.topjohnwu.superuser.Shell
import filey.app.feature.security.domain.AccessMode
import filey.app.feature.security.domain.PrivilegedCommand
import filey.app.feature.security.domain.SecurityPolicyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Root executor via libsu ───────────────────────────────────────────────────

interface CommandExecutor {
    suspend fun execute(command: String): String
}

class RootCommandExecutor : CommandExecutor {
    override suspend fun execute(command: String): String = withContext(Dispatchers.IO) {
        if (!Shell.getShell().isRoot) throw SecurityException("Root access not available")
        val result = Shell.cmd(command).exec()
        if (!result.isSuccess)
            throw RuntimeException("Root failed (${result.code}): ${result.err.joinToString("\n")}")
        result.out.joinToString("\n")
    }
    fun isAvailable(): Boolean = try { Shell.getShell().isRoot } catch (e: Exception) { false }
}

// ── Shizuku executor ──────────────────────────────────────────────────────────

class ShizukuCommandExecutor : CommandExecutor {
    override suspend fun execute(command: String): String = withContext(Dispatchers.IO) {
        if (!isAvailable()) throw SecurityException("Shizuku not available or permission not granted")
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exit = process.waitFor()
        if (exit != 0) throw RuntimeException("Shizuku command failed ($exit): $stderr")
        stdout
    }

    fun isAvailable(): Boolean = try {
        val shizukuClass = Class.forName("dev.rikka.shizuku.Shizuku")
        val pingBinder = shizukuClass.getMethod("pingBinder")
        val checkPerm = shizukuClass.getMethod("checkSelfPermission")
        val ping = pingBinder.invoke(null) as Boolean
        val perm = checkPerm.invoke(null) as Int
        ping && perm == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }
}

// ── Sandboxed orchestrator ────────────────────────────────────────────────────

class SandboxedCommandExecutor(
    private val policyEngine: SecurityPolicyEngine,
    private val rootExecutor: RootCommandExecutor,
    private val shizukuExecutor: ShizukuCommandExecutor,
    private val auditLogger: AuditLogger,
    private val accessMode: () -> AccessMode          // lambda replaces Provider<>
) {
    sealed class ExecutionResult {
        data class Success(val output: String = "", val executionTimeMs: Long = 0) : ExecutionResult()
        data class Denied(val reason: String) : ExecutionResult()
        data class Error(val exception: Throwable, val stderr: String = "") : ExecutionResult()
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
            val output = when (accessMode()) {
                AccessMode.ROOT    -> rootExecutor.execute(shellCommand)
                AccessMode.SHIZUKU -> shizukuExecutor.execute(shellCommand)
                AccessMode.NORMAL  -> throw SecurityException("Elevated privileges required")
            }
            val elapsed = System.currentTimeMillis() - startTime
            auditLogger.logExecuted(command, elapsed)
            ExecutionResult.Success(output = output, executionTimeMs = elapsed)
        } catch (e: Exception) {
            auditLogger.logError(command, e)
            ExecutionResult.Error(e)
        }
    }

    private fun compileCommand(cmd: PrivilegedCommand): String = when (cmd) {
        is PrivilegedCommand.ListDirectory     -> "ls ${if (cmd.showHidden) "-la" else "-l"} ${cmd.path.shellEscaped()}"
        is PrivilegedCommand.CopyFile          -> "cp -f ${cmd.source.shellEscaped()} ${cmd.destination.shellEscaped()}"
        is PrivilegedCommand.MoveFile          -> "mv -f ${cmd.source.shellEscaped()} ${cmd.destination.shellEscaped()}"
        is PrivilegedCommand.DeleteFile        -> "rm ${if (cmd.recursive) "-rf" else "-f"} ${cmd.target.shellEscaped()}"
        is PrivilegedCommand.ChangePermissions -> "chmod ${cmd.permissions.toOctalString()} ${cmd.target.shellEscaped()}"
        is PrivilegedCommand.ChangeOwner       -> "chown ${cmd.owner.name}:${cmd.group.name} ${cmd.target.shellEscaped()}"
        is PrivilegedCommand.ReadSystemFile    -> "cat ${cmd.path.shellEscaped()}"
        is PrivilegedCommand.MountFilesystem   -> "mount -o ${cmd.options.toFlags()} ${cmd.device.shellEscaped()} ${cmd.mountPoint.shellEscaped()}"
    }
}
