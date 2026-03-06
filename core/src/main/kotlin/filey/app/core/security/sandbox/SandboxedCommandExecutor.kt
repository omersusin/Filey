package filey.app.core.security.sandbox

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SandboxedCommandExecutor(
    private val policyEngine: SecurityPolicyEngine
) {

    sealed class ExecutionResult {
        data class Success(val output: String, val executionTimeMs: Long) : ExecutionResult()
        data class Denied(val reason: String) : ExecutionResult()
        data class Error(val message: String, val exitCode: Int) : ExecutionResult()
    }

    suspend fun execute(command: PrivilegedCommand): ExecutionResult = withContext(Dispatchers.IO) {
        // 1) Policy Check
        val policyResult = policyEngine.evaluate(command)
        if (!policyResult.allowed) {
            return@withContext ExecutionResult.Denied(policyResult.reason)
        }

        // 2) Execution
        val startTime = System.currentTimeMillis()
        val shellCommand = command.compile()
        
        val result = if (command.requiresRoot) {
            Shell.cmd(shellCommand).exec()
        } else {
            // Normal execution if root not required, but here we use Shell for consistency
            Shell.cmd(shellCommand).exec()
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        if (result.isSuccess) {
            ExecutionResult.Success(result.out.joinToString("\n"), duration)
        } else {
            ExecutionResult.Error(result.err.joinToString("\n"), result.code)
        }
    }
}
