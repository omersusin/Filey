package filey.app.core.root

import filey.app.core.result.FileResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RootManager(
    private val shellRunner: ShellRunner = RealShellRunner()
) {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { RootManager() }

        fun createForTest(runner: ShellRunner) = RootManager(runner)

        // ── Korunan yollar ──
        private val PROTECTED_PATHS = setOf(
            "/", "/system", "/vendor", "/proc", "/sys", "/dev",
            "/data", "/apex", "/metadata", "/persist", "/efs",
            "/firmware", "/boot", "/recovery", "/cache",
            "/mnt", "/oem", "/product", "/system_ext"
        )

        // ── Komut başına izin verilen flag'ler ──
        private val ALLOWED_FLAGS = mapOf(
            "rm"    to setOf("-r", "-f", "-rf", "-fr", "-i", "-v"),
            "mv"    to setOf("-f", "-i", "-v", "-n"),
            "cp"    to setOf("-r", "-f", "-rf", "-fr", "-a", "-p", "-v"),
            "chmod" to setOf("-R", "-v"),
            "chown" to setOf("-R", "-v"),
            "ls"    to setOf("-l", "-a", "-la", "-al", "-lh", "-alh", "-R", "-1"),
            "cat"   to setOf("-n", "-b"),
            "mkdir" to setOf("-p", "-v"),
            "touch" to emptySet(),
            "stat"  to setOf("-c"),
        )

        val ALLOWED_COMMANDS: Set<String> = ALLOWED_FLAGS.keys
    }

    // Root erişim kontrolü — lazy, uygulama başlangıcını yavaşlatmaz
    val isRootAvailable: Boolean by lazy {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val result = process.inputStream.bufferedReader().readText()
            process.waitFor()
            result.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun execute(
        command: String,
        flags: List<String>,
        args: List<String>
    ): FileResult<List<String>> = withContext(Dispatchers.IO) {

        // 1) Komut whitelist
        if (command !in ALLOWED_COMMANDS) {
            return@withContext FileResult.Error.Unknown(
                SecurityException("İzin verilmeyen komut: $command")
            )
        }

        // 2) Flag doğrulaması
        val flagCheck = validateFlags(command, flags)
        if (flagCheck is FileResult.Error) return@withContext flagCheck as FileResult<List<String>>

        // 3) Path koruması
        for (arg in args) {
            if (isProtectedPath(arg)) {
                return@withContext FileResult.Error.PermissionDenied(arg)
            }
        }

        // 4) Komut oluştur ve çalıştır
        val fullCommand = buildCommand(command, flags, args)
        val result = shellRunner.run(fullCommand)

        if (result.exitCode == 0) {
            FileResult.Success(result.stdout)
        } else {
            FileResult.Error.ShellCommandFailed(
                command = fullCommand,
                exitCode = result.exitCode,
                stdout = result.stdout.joinToString("\n"),
                stderr = result.stderr.joinToString("\n")
            )
        }
    }

    // ── Yardımcı fonksiyonlar ──

    internal fun isProtectedPath(path: String): Boolean {
        val canonical = try {
            File(path).canonicalPath
        } catch (e: Exception) {
            return true // Canonical alınamazsa güvenli tarafta kal
        }
        return PROTECTED_PATHS.any { p ->
            canonical == p || canonical.startsWith("$p/")
        }
    }

    internal fun validateFlags(command: String, flags: List<String>): FileResult<Unit> {
        val allowed = ALLOWED_FLAGS[command]
            ?: return FileResult.Error.Unknown(IllegalArgumentException("Bilinmeyen komut: $command"))

        for (flag in flags) {
            if (!flag.matches(Regex("^-[a-zA-Z]{1,5}$"))) {
                return FileResult.Error.Unknown(SecurityException("Geçersiz flag formatı: $flag"))
            }
            if (flag !in allowed) {
                return FileResult.Error.Unknown(SecurityException("'$command' için izin verilmeyen flag: $flag"))
            }
        }
        return FileResult.Success(Unit)
    }

    internal fun shellEscape(arg: String): String {
        return "'" + arg.replace("'", "'\\''") + "'"
    }

    private fun buildCommand(command: String, flags: List<String>, args: List<String>): String =
        buildString {
            append(command)
            flags.forEach { append(" $it") }
            args.map { shellEscape(it) }.forEach { append(" $it") }
        }
}
