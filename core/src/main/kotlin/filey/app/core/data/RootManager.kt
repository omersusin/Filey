package filey.app.core.data

import android.util.Log
import filey.app.core.model.FileResult
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

/**
 * Root komutlarını daha güvenli çalıştırmak için:
 * - argümanlar tek tek shellEscape edilir
 * - command whitelist uygulanır
 * - flag doğrulaması yapılır
 * - kritik dizinlerde destructive operasyonlar engellenir
 *
 * NOT: Bu sınıf Sprint 2'de SafeRootShell'e ayrıştırılacak.
 */
class RootManager {

    companion object {
        private const val TAG = "RootManager"

        private val ALLOWED_COMMANDS = setOf(
            "ls", "cp", "mv", "rm", "mkdir", "stat",
            "cat", "touch", "chmod", "find", "du",
            "tar", "unzip", "head", "tail", "wc"
        )

        private val PROTECTED_PATHS = setOf(
            "/system", "/vendor", "/proc", "/sys",
            "/dev", "/init", "/sbin", "/boot"
        )

        fun shellEscape(input: String): String {
            require('\u0000' !in input) { "Path contains null byte" }
            require('\n' !in input && '\r' !in input) { "Path contains newline" }
            // POSIX single-quote escaping:  ' -> '"'"'
            return "'" + input.replace("'", "'\"'\"'") + "'"
        }

        fun isProtectedPath(path: String): Boolean {
            val canonical = try { File(path).canonicalPath } catch (_: Exception) { path }
            return PROTECTED_PATHS.any { canonical.startsWith(it) }
        }

        fun validateFlag(flag: String) {
            require(flag.startsWith("-")) { "Invalid flag: $flag" }
            require(flag.matches(Regex("^--?[a-zA-Z0-9-]+$"))) { "Invalid flag characters: $flag" }
        }
    }

    data class ShellResult(
        val exitCode: Int,
        val stdout: List<String>,
        val stderr: List<String>
    ) {
        val isSuccess: Boolean get() = exitCode == 0
        val errorText: String get() = stderr.joinToString("\n")
        val outputText: String get() = stdout.joinToString("\n")
    }

    /**
     * TODO: Projede halihazırda hangi root library kullanılıyorsa
     * burada oraya bağlayacağız. Şimdilik "su -c" ile çalıştırıyoruz.
     *
     * Eğer projede libsu vb. varsa, bunu ona göre uyarlayacağız.
     */
    private suspend fun runSu(command: String, timeoutMs: Long): ShellResult = withContext(Dispatchers.IO) {
        try {
            val proc = withTimeout(timeoutMs) {
                Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            }
            val stdout = proc.inputStream.bufferedReader().readLines()
            val stderr = proc.errorStream.bufferedReader().readLines()
            val code = proc.waitFor()
            ShellResult(code, stdout, stderr)
        } catch (e: TimeoutCancellationException) {
            ShellResult(-1, emptyList(), listOf("Command timed out after ${timeoutMs}ms"))
        } catch (e: Exception) {
            ShellResult(-1, emptyList(), listOf(e.message ?: "Unknown shell error"))
        }
    }

    suspend fun execute(
        command: String,
        args: List<String> = emptyList(),
        flags: List<String> = emptyList(),
        timeoutMs: Long = 30_000
    ): FileResult<ShellResult> = withContext(Dispatchers.IO) {
        try {
            require(command in ALLOWED_COMMANDS) { "Command not allowed: $command" }
            flags.forEach { validateFlag(it) }

            // Destructive komutlarda protected path kontrolü
            if (command in setOf("rm", "mv")) {
                args.forEach { arg ->
                    require(!isProtectedPath(arg)) { "Blocked operation on protected path: $arg" }
                }
            }

            val full = buildString {
                append(command)
                flags.forEach { f -> append(' ').append(f) }
                args.forEach { a -> append(' ').append(shellEscape(a)) }
            }

            Log.d(TAG, "Executing root: $full")

            val res = runSu(full, timeoutMs)
            if (!res.isSuccess) {
                FileResult.Error.ShellCommandFailed(
                    command = full,
                    exitCode = res.exitCode,
                    stderr = res.errorText
                )
            } else {
                FileResult.Success(res)
            }
        } catch (e: IllegalArgumentException) {
            FileResult.Error.IOFailure(message = e.message ?: "Invalid args", cause = e)
        } catch (e: Exception) {
            FileResult.Error.Unknown(message = e.message ?: "Root command failed", cause = e)
        }
    }

    suspend fun copyFile(source: String, destination: String): FileResult<Unit> {
        val r = execute("cp", args = listOf(source, destination), flags = listOf("-a"))
        return if (r is FileResult.Success) FileResult.Success(Unit) else (r as FileResult.Error)
    }

    suspend fun moveFile(source: String, destination: String): FileResult<Unit> {
        val r = execute("mv", args = listOf(source, destination))
        return if (r is FileResult.Success) FileResult.Success(Unit) else (r as FileResult.Error)
    }

    suspend fun deleteFile(path: String): FileResult<Unit> {
        if (isProtectedPath(path)) {
            return FileResult.Error.IOFailure("Cannot delete protected path: $path")
        }
        val r = execute("rm", args = listOf(path), flags = listOf("-rf"))
        return if (r is FileResult.Success) FileResult.Success(Unit) else (r as FileResult.Error)
    }

    suspend fun listDirectory(path: String): FileResult<List<String>> {
        val r = execute("ls", args = listOf(path), flags = listOf("-la"))
        return when (r) {
            is FileResult.Success -> FileResult.Success(r.data.stdout)
            is FileResult.Error -> r
        }
    }

    suspend fun stat(path: String): FileResult<ShellResult> {
        // örnek: size mtime perms
        return execute("stat", args = listOf(path), flags = listOf("-c", "%s %Y %a"))
    }
}
