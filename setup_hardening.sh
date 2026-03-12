#!/bin/bash
# Filey Sprint1 Hardening — Tüm dosyaları oluştur
# Termux'ta Filey/ klasöründeyken çalıştır:
#   cd ~/Filey && bash setup_hardening.sh

set -e
CYAN='\033[0;36m'; GREEN='\033[0;32m'; NC='\033[0m'
log() { echo -e "${CYAN}[Filey]${NC} $1"; }
ok()  { echo -e "${GREEN}[OK]${NC} $1"; }

ROOT="$(pwd)"
CORE_PKG="core/src/main/kotlin/filey/app/core"
APP_PKG="app/src/main/kotlin/filey/app"
FEAT_PKG="feature/browser/src/main/kotlin/filey/app/feature/browser"
CORE_TEST="core/src/test/kotlin/filey/app/core"
CORE_RES="core/src/main/res/values"
CI=".github/workflows"

mkdir -p "$CORE_PKG/root" "$CORE_PKG/result" "$CORE_PKG/model" \
         "$CORE_PKG/navigation" "$CORE_PKG/storage" "$CORE_PKG/safety" \
         "$APP_PKG" "$FEAT_PKG/navigation" "$FEAT_PKG/search" "$FEAT_PKG/sort" \
         "$CORE_TEST/root" "$CORE_RES" "$CI"

log "Dosyalar oluşturuluyor..."

# ─────────────────────────────────────────────
# 1. FileyApp.kt — Application init düzeltmesi (P0)
# ─────────────────────────────────────────────
cat > "$APP_PKG/FileyApp.kt" << 'KOTLIN'
package filey.app

import android.app.Application

class FileyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // RootManager.init() KALDIRILDI
        // Root kontrolü lazy olarak ilk ihtiyaç anında yapılır
    }
}
KOTLIN
ok "FileyApp.kt"

# ─────────────────────────────────────────────
# 2. ShellRunner.kt — Testable interface (P1)
# ─────────────────────────────────────────────
cat > "$CORE_PKG/root/ShellRunner.kt" << 'KOTLIN'
package filey.app.core.root

interface ShellRunner {
    suspend fun run(command: String): ShellResult
}

data class ShellResult(
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>
)
KOTLIN
ok "ShellRunner.kt"

# ─────────────────────────────────────────────
# 3. RealShellRunner.kt
# ─────────────────────────────────────────────
cat > "$CORE_PKG/root/RealShellRunner.kt" << 'KOTLIN'
package filey.app.core.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealShellRunner : ShellRunner {
    override suspend fun run(command: String): ShellResult = withContext(Dispatchers.IO) {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        val stdout = process.inputStream.bufferedReader().readLines()
        val stderr = process.errorStream.bufferedReader().readLines()
        val exitCode = process.waitFor()
        ShellResult(exitCode, stdout, stderr)
    }
}
KOTLIN
ok "RealShellRunner.kt"

# ─────────────────────────────────────────────
# 4. FileResult.kt — Hata tipleri (P1)
# ─────────────────────────────────────────────
cat > "$CORE_PKG/result/FileResult.kt" << 'KOTLIN'
package filey.app.core.result

sealed class FileResult<out T> {
    data class Success<T>(val data: T) : FileResult<T>()

    sealed class Error : FileResult<Nothing>() {
        data class IOFailure(val cause: Throwable) : Error()
        data class PermissionDenied(val path: String) : Error()
        data class NotFound(val path: String) : Error()
        object RootRequired : Error()
        data class ShellCommandFailed(
            val command: String,
            val exitCode: Int,
            val stdout: String,
            val stderr: String
        ) : Error()
        data class Unknown(val cause: Throwable) : Error()
    }
}
KOTLIN
ok "FileResult.kt"

# ─────────────────────────────────────────────
# 5. RootManager.kt — Güvenlik sertleştirmesi (P0)
# ─────────────────────────────────────────────
cat > "$CORE_PKG/root/RootManager.kt" << 'KOTLIN'
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
KOTLIN
ok "RootManager.kt"

# ─────────────────────────────────────────────
# 6. RootFileOperations.kt — Yüksek seviye API (P0)
# ─────────────────────────────────────────────
cat > "$CORE_PKG/root/RootFileOperations.kt" << 'KOTLIN'
package filey.app.core.root

import filey.app.core.result.FileResult

/**
 * UI ve ViewModel katmanları SADECE bu sınıfı kullanmalı.
 * RootManager.execute() doğrudan çağrılmamalı.
 */
class RootFileOperations internal constructor(
    private val rootManager: RootManager
) {
    companion object {
        val instance by lazy { RootFileOperations(RootManager.instance) }
    }

    suspend fun listDirectory(path: String) =
        rootManager.execute("ls", listOf("-la"), listOf(path))

    suspend fun copyFile(source: String, destination: String) =
        rootManager.execute("cp", listOf("-r"), listOf(source, destination))

    suspend fun moveFile(source: String, destination: String) =
        rootManager.execute("mv", emptyList(), listOf(source, destination))

    suspend fun deleteFile(path: String, recursive: Boolean = false): FileResult<List<String>> {
        val flags = if (recursive) listOf("-rf") else listOf("-f")
        return rootManager.execute("rm", flags, listOf(path))
    }

    suspend fun stat(path: String) =
        rootManager.execute("stat", emptyList(), listOf(path))

    suspend fun createDirectory(path: String) =
        rootManager.execute("mkdir", listOf("-p"), listOf(path))

    suspend fun changePermissions(
        path: String,
        mode: String,
        recursive: Boolean = false
    ): FileResult<List<String>> {
        if (!mode.matches(Regex("^[0-7]{3,4}$|^[ugoa]+[=+\\-][rwxXst]+$"))) {
            return FileResult.Error.Unknown(
                IllegalArgumentException("Geçersiz permission mode: $mode")
            )
        }
        val flags = if (recursive) listOf("-R") else emptyList()
        return rootManager.execute("chmod", flags, listOf(mode, path))
    }
}
KOTLIN
ok "RootFileOperations.kt"

# ─────────────────────────────────────────────
# 7. ResultMapper.kt (P1)
# ─────────────────────────────────────────────
cat > "$CORE_PKG/result/ResultMapper.kt" << 'KOTLIN'
package filey.app.core.result

import android.content.Context
import android.util.Log
import filey.app.core.R

object ResultMapper {

    fun mapErrorToUserMessage(error: FileResult.Error, context: Context): String = when (error) {
        is FileResult.Error.IOFailure ->
            context.getString(R.string.error_io, error.cause.localizedMessage ?: "")
        is FileResult.Error.PermissionDenied ->
            context.getString(R.string.error_permission, error.path)
        is FileResult.Error.NotFound ->
            context.getString(R.string.error_not_found, error.path)
        is FileResult.Error.RootRequired ->
            context.getString(R.string.error_root_required)
        is FileResult.Error.ShellCommandFailed ->
            context.getString(R.string.error_shell_failed, error.exitCode.toString())
        is FileResult.Error.Unknown ->
            context.getString(R.string.error_unknown, error.cause.localizedMessage ?: "")
    }

    fun mapErrorToDebugInfo(error: FileResult.Error): String = when (error) {
        is FileResult.Error.ShellCommandFailed -> buildString {
            appendLine("Command  : ${error.command}")
            appendLine("Exit Code: ${error.exitCode}")
            appendLine("STDOUT   : ${error.stdout}")
            appendLine("STDERR   : ${error.stderr}")
        }
        is FileResult.Error.IOFailure -> "IOException: ${error.cause.stackTraceToString()}"
        is FileResult.Error.Unknown   -> "Unknown: ${error.cause.stackTraceToString()}"
        else -> error.toString()
    }

    fun logIfDebug(tag: String, error: FileResult.Error, isDebug: Boolean) {
        if (isDebug) Log.e(tag, mapErrorToDebugInfo(error))
    }
}
KOTLIN
ok "ResultMapper.kt"

# ─────────────────────────────────────────────
# 8. FileType.kt (P2)
# ─────────────────────────────────────────────
cat > "$CORE_PKG/model/FileType.kt" << 'KOTLIN'
package filey.app.core.model

enum class FileType {
    IMAGE, VIDEO, AUDIO, TEXT, ARCHIVE, UNKNOWN;

    companion object {
        private val IMAGE_EXT   = setOf("jpg","jpeg","png","gif","bmp","webp","svg","heic","heif")
        private val VIDEO_EXT   = setOf("mp4","mkv","avi","mov","webm","flv","3gp","ts","wmv")
        private val AUDIO_EXT   = setOf("mp3","wav","flac","aac","ogg","m4a","wma","opus")
        private val TEXT_EXT    = setOf("txt","md","json","xml","csv","log","yaml","yml",
                                        "kt","java","py","js","ts","html","css","sh","conf","toml","ini")
        private val ARCHIVE_EXT = setOf("zip","tar","gz","bz2","7z","rar","xz")

        fun fromFileName(name: String): FileType {
            val ext = name.substringAfterLast('.', "").lowercase()
            return when {
                ext in IMAGE_EXT   -> IMAGE
                ext in VIDEO_EXT   -> VIDEO
                ext in AUDIO_EXT   -> AUDIO
                ext in TEXT_EXT    -> TEXT
                ext in ARCHIVE_EXT -> ARCHIVE
                else               -> UNKNOWN
            }
        }
    }
}
KOTLIN
ok "FileType.kt"

# ─────────────────────────────────────────────
# 9. Routes.kt (P2)
# ─────────────────────────────────────────────
cat > "$CORE_PKG/navigation/Routes.kt" << 'KOTLIN'
package filey.app.core.navigation

import android.net.Uri

object Routes {
    const val BROWSER = "browser"
    const val VIEWER  = "viewer/{encodedPath}"
    const val EDITOR  = "editor/{encodedPath}"
    const val PLAYER  = "player/{encodedPath}"
    const val ARCHIVE = "archive/{encodedPath}"

    fun viewer(path: String)  = "viewer/${Uri.encode(path)}"
    fun editor(path: String)  = "editor/${Uri.encode(path)}"
    fun player(path: String)  = "player/${Uri.encode(path)}"
    fun archive(path: String) = "archive/${Uri.encode(path)}"
}
KOTLIN
ok "Routes.kt"

# ─────────────────────────────────────────────
# 10. FileRouter.kt (P2)
# ─────────────────────────────────────────────
cat > "$FEAT_PKG/navigation/FileRouter.kt" << 'KOTLIN'
package filey.app.feature.browser.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileChooserActivity
import androidx.navigation.NavController
import filey.app.core.model.FileType
import filey.app.core.navigation.Routes
import java.io.File

class FileRouter(
    private val navController: NavController,
    private val context: Context
) {
    fun openFile(filePath: String) {
        val fileName = filePath.substringAfterLast('/')
        when (FileType.fromFileName(fileName)) {
            FileType.IMAGE              -> navController.navigate(Routes.viewer(filePath))
            FileType.TEXT               -> navController.navigate(Routes.editor(filePath))
            FileType.VIDEO, FileType.AUDIO -> navController.navigate(Routes.player(filePath))
            FileType.ARCHIVE            -> navController.navigate(Routes.archive(filePath))
            FileType.OTHER            -> openWithSystemChooser(filePath)
        }
    }

    private fun openWithSystemChooser(filePath: String) {
        val file = File(filePath)
        val uri  = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Şununla aç…"))
    }
}
KOTLIN
ok "FileRouter.kt"

# ─────────────────────────────────────────────
# 11. SortOption.kt (P2)
# ─────────────────────────────────────────────
cat > "$FEAT_PKG/sort/SortOption.kt" << 'KOTLIN'
package filey.app.feature.browser.sort

import filey.app.core.model.FileItem

enum class SortField     { NAME, SIZE, DATE, EXTENSION }
enum class SortDirection { ASC, DESC }

data class SortOption(
    val field: SortField = SortField.NAME,
    val direction: SortDirection = SortDirection.ASC,
    val foldersFirst: Boolean = true
)

fun List<FileItem>.sorted(option: SortOption): List<FileItem> {
    val base: Comparator<FileItem> = when (option.field) {
        SortField.NAME      -> compareBy { it.name.lowercase() }
        SortField.SIZE      -> compareBy { it.size }
        SortField.DATE      -> compareBy { it.lastModified }
        SortField.EXTENSION -> compareBy { it.name.substringAfterLast('.', "").lowercase() }
    }
    val directed = if (option.direction == SortDirection.DESC) base.reversed() else base
    val final    = if (option.foldersFirst)
        compareByDescending<FileItem> { it.isDirectory }.then(directed)
    else directed
    return sortedWith(final)
}
KOTLIN
ok "SortOption.kt"

# ─────────────────────────────────────────────
# 12. FileSearchEngine.kt (P3)
# ─────────────────────────────────────────────
cat > "$FEAT_PKG/search/FileSearchEngine.kt" << 'KOTLIN'
package filey.app.feature.browser.search

import filey.app.core.model.FileItem
import filey.app.core.model.toFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield
import java.io.File

data class SearchFilter(
    val query: String = "",
    val extensions: Set<String> = emptySet(),
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val modifiedAfter: Long? = null,
    val modifiedBefore: Long? = null,
)

class FileSearchEngine {

    fun search(rootPath: String, filter: SearchFilter): Flow<FileItem> = flow {
        val rootDir = File(rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return@flow

        val queue = ArrayDeque<File>()
        queue.add(rootDir)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val children = current.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory) queue.add(child)
                if (matchesFilter(child, filter)) emit(child.toFileItem())
            }
            yield()
        }
    }.flowOn(Dispatchers.IO)

    private fun matchesFilter(file: File, f: SearchFilter): Boolean {
        if (f.query.isNotBlank() && !file.name.contains(f.query, ignoreCase = true)) return false
        if (f.extensions.isNotEmpty() && file.extension.lowercase() !in f.extensions) return false
        f.minSize?.let { if (file.length() < it) return false }
        f.maxSize?.let { if (file.length() > it) return false }
        f.modifiedAfter?.let { if (file.lastModified() < it) return false }
        f.modifiedBefore?.let { if (file.lastModified() > it) return false }
        return true
    }
}
KOTLIN
ok "FileSearchEngine.kt"

# ─────────────────────────────────────────────
# 13. strings.xml — Hata mesajları (P1)
# ─────────────────────────────────────────────
cat > "$CORE_RES/strings_errors.xml" << 'XML'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="error_io">Dosya işlemi başarısız: %s</string>
    <string name="error_permission">Erişim reddedildi: %s</string>
    <string name="error_not_found">Dosya bulunamadı: %s</string>
    <string name="error_root_required">Bu işlem root yetkisi gerektiriyor</string>
    <string name="error_shell_failed">Komut başarısız (kod: %s)</string>
    <string name="error_unknown">Beklenmeyen hata: %s</string>
</resources>
XML
ok "strings_errors.xml"

# ─────────────────────────────────────────────
# 14. TEST — FakeShellRunner.kt (P1)
# ─────────────────────────────────────────────
cat > "$CORE_TEST/root/FakeShellRunner.kt" << 'KOTLIN'
package filey.app.core.root

class FakeShellRunner : ShellRunner {
    private val responses = mutableMapOf<String, ShellResult>()
    val executedCommands = mutableListOf<String>()

    fun givenResponse(commandContains: String, result: ShellResult) {
        responses[commandContains] = result
    }

    override suspend fun run(command: String): ShellResult {
        executedCommands.add(command)
        val key = responses.keys.firstOrNull { command.contains(it) }
        return responses[key] ?: ShellResult(127, emptyList(), listOf("command not found (fake)"))
    }
}
KOTLIN
ok "FakeShellRunner.kt"

# ─────────────────────────────────────────────
# 15. TEST — RootManagerTest.kt (P1)
# ─────────────────────────────────────────────
cat > "$CORE_TEST/root/RootManagerTest.kt" << 'KOTLIN'
package filey.app.core.root

import filey.app.core.result.FileResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RootManagerTest {

    private lateinit var fake: FakeShellRunner
    private lateinit var rm: RootManager

    @Before fun setup() {
        fake = FakeShellRunner()
        rm   = RootManager.createForTest(fake)
    }

    @Test fun `ls success returns Success`() = runTest {
        fake.givenResponse("ls", ShellResult(0, listOf("a.txt", "b.jpg"), emptyList()))
        val result = rm.execute("ls", listOf("-la"), listOf("/sdcard"))
        assertTrue(result is FileResult.Success)
        assertEquals(listOf("a.txt", "b.jpg"), (result as FileResult.Success).data)
    }

    @Test fun `disallowed command returns Error`() = runTest {
        val result = rm.execute("reboot", emptyList(), emptyList())
        assertTrue(result is FileResult.Error)
    }

    @Test fun `protected path returns PermissionDenied`() = runTest {
        val result = rm.execute("rm", listOf("-rf"), listOf("/system"))
        assertTrue(result is FileResult.Error.PermissionDenied)
    }

    @Test fun `shell failure returns ShellCommandFailed`() = runTest {
        fake.givenResponse("ls", ShellResult(2, emptyList(), listOf("No such file")))
        val result = rm.execute("ls", emptyList(), listOf("/nonexistent"))
        assertTrue(result is FileResult.Error.ShellCommandFailed)
        assertEquals(2, (result as FileResult.Error.ShellCommandFailed).exitCode)
    }

    @Test fun `invalid flag returns Error`() = runTest {
        val result = rm.execute("rm", listOf("--no-preserve-root"), listOf("/sdcard/file.txt"))
        assertTrue(result is FileResult.Error)
    }
}
KOTLIN
ok "RootManagerTest.kt"

# ─────────────────────────────────────────────
# 16. TEST — ShellEscapeTest.kt (P1)
# ─────────────────────────────────────────────
cat > "$CORE_TEST/root/ShellEscapeTest.kt" << 'KOTLIN'
package filey.app.core.root

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellEscapeTest {
    private val rm = RootManager.createForTest(FakeShellRunner())

    @Test fun `normal path stays wrapped`() =
        assertEquals("'/sdcard/file.txt'", rm.shellEscape("/sdcard/file.txt"))

    @Test fun `space in path is handled`() =
        assertEquals("'/sdcard/My Files/a.txt'", rm.shellEscape("/sdcard/My Files/a.txt"))

    @Test fun `single quote is escaped`() =
        assertEquals("'/sdcard/it'\\''s.txt'", rm.shellEscape("/sdcard/it's.txt"))

    @Test fun `injection attempt is neutralised`() {
        val result = rm.shellEscape("/sdcard/file; rm -rf /")
        assertEquals("'/sdcard/file; rm -rf /'", result)
    }

    @Test fun `empty string gives empty quotes`() =
        assertEquals("''", rm.shellEscape(""))
}
KOTLIN
ok "ShellEscapeTest.kt"

# ─────────────────────────────────────────────
# 17. TEST — ProtectedPathTest.kt (P1)
# ─────────────────────────────────────────────
cat > "$CORE_TEST/root/ProtectedPathTest.kt" << 'KOTLIN'
package filey.app.core.root

import org.junit.Assert.*
import org.junit.Test

class ProtectedPathTest {
    private val rm = RootManager.createForTest(FakeShellRunner())

    @Test fun `root is protected`()              = assertTrue(rm.isProtectedPath("/"))
    @Test fun `system is protected`()            = assertTrue(rm.isProtectedPath("/system"))
    @Test fun `system sub-path is protected`()   = assertTrue(rm.isProtectedPath("/system/app"))
    @Test fun `data is protected`()              = assertTrue(rm.isProtectedPath("/data"))
    @Test fun `apex is protected`()              = assertTrue(rm.isProtectedPath("/apex"))
    @Test fun `systemui is NOT protected`()      = assertFalse(rm.isProtectedPath("/systemui"))
    @Test fun `sdcard is NOT protected`()        = assertFalse(rm.isProtectedPath("/sdcard/Documents"))
    @Test fun `storage emulated NOT protected`() =
        assertFalse(rm.isProtectedPath("/storage/emulated/0/Download"))
}
KOTLIN
ok "ProtectedPathTest.kt"

# ─────────────────────────────────────────────
# 18. CI/CD — build.yml (P2)
# ─────────────────────────────────────────────
cat > "$CI/build.yml" << 'YAML'
name: Filey CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  lint-and-test:
    if: github.event_name == 'pull_request'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          cache-read-only: ${{ github.ref != 'refs/heads/main' }}
      - name: Lint
        run: ./gradlew lint
      - name: Unit Tests
        run: ./gradlew testDebugUnitTest
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: '**/build/reports/tests/'

  build:
    if: github.event_name == 'push'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      - name: Build Debug APK
        run: ./gradlew assembleDebug
      - name: Run Tests
        run: ./gradlew test
      - name: Build Release APK
        run: ./gradlew assembleRelease
      - name: Upload APKs
        uses: actions/upload-artifact@v4
        with:
          name: apks
          path: |
            app/build/outputs/apk/debug/*.apk
            app/build/outputs/apk/release/*.apk
YAML
ok "build.yml"

echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}  Tüm dosyalar oluşturuldu!${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo ""
echo "Şimdi commit & push için şunu çalıştır:"
echo ""
echo "  git add -A"
echo "  git commit -m 'feat: sprint1 hardening — security, error handling, tests, CI'"
echo "  git push origin feat/sprint1-hardening"
