package filey.app.core.data.root

import com.topjohnwu.superuser.Shell
import filey.app.core.data.FileRepository
import filey.app.core.model.FileModel
import filey.app.core.model.FileUtils
import filey.app.core.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RootFileRepository : FileRepository {

    companion object {
        /** Call once at app start. Returns true if root is available. */
        fun isRootAvailable(): Boolean {
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(10)
            )
            return Shell.getShell().isRoot
        }
    }

    private fun exec(vararg cmds: String): Shell.Result =
        Shell.cmd(*cmds).exec()

    private fun execOut(vararg cmds: String): List<String> =
        Shell.cmd(*cmds).exec().out

    // ── Interface implementation ─────────────────────────────

    override suspend fun listFiles(path: String): Result<List<FileModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val escapedPath = path.shellEscape()
                // Use -a to show hidden, -l for long format, -e for ISO-like dates (easier to parse)
                val lines = execOut("ls -la $escapedPath")

                lines.mapNotNull { parseLsLine(it, path) }
                    .sortedWith(
                        compareByDescending<FileModel> { it.isDirectory }
                            .thenBy { it.name.lowercase() }
                    )
            }
        }

    override suspend fun createDirectory(path: String, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = "$path/$name".shellEscape()
                val result = exec("mkdir -p $target")
                if (!result.isSuccess) error("mkdir failed: ${result.err.joinToString()}")
            }
        }

    override suspend fun delete(path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = exec("rm -rf ${path.shellEscape()}")
                if (!result.isSuccess) error("rm failed: ${result.err.joinToString()}")
            }
        }

    override suspend fun delete(paths: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val joined = paths.joinToString(" ") { it.shellEscape() }
                val result = exec("rm -rf $joined")
                if (!result.isSuccess) error("rm failed: ${result.err.joinToString()}")
            }
        }

    override suspend fun rename(path: String, newName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val parent = path.substringBeforeLast('/')
                val newPath = "$parent/$newName"
                val result = exec("mv ${path.shellEscape()} ${newPath.shellEscape()}")
                if (!result.isSuccess) error("mv failed: ${result.err.joinToString()}")
                newPath
            }
        }

    override suspend fun copy(
        source: String,
        destination: String,
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val result = exec("cp -rf ${source.shellEscape()} ${destination.shellEscape()}/")
            if (!result.isSuccess) error("cp failed: ${result.err.joinToString()}")
            onProgress?.invoke(1f)
            Unit
        }
    }

    override suspend fun move(
        source: String,
        destination: String,
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val result = exec("mv ${source.shellEscape()} ${destination.shellEscape()}/")
            if (!result.isSuccess) error("mv failed: ${result.err.joinToString()}")
            onProgress?.invoke(1f)
            Unit
        }
    }

    override suspend fun getFileInfo(path: String): Result<FileModel> =
        withContext(Dispatchers.IO) {
            runCatching {
                val lines = execOut("ls -lad ${path.shellEscape()}")
                val line = lines.firstOrNull() ?: error("stat failed: $path")
                val parentDir = path.substringBeforeLast('/')
                parseLsLine(line, parentDir) ?: error("parse failed: $line")
            }
        }

    override suspend fun exists(path: String): Boolean =
        withContext(Dispatchers.IO) {
            exec("test -e ${path.shellEscape()}").isSuccess
        }

    override suspend fun getStorageInfo(path: String): Result<StorageInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                // df outputs: Filesystem 1K-blocks Used Available Use% Mounted on
                val lines = execOut("df ${path.shellEscape()}")
                val parts = lines.getOrNull(1)?.trim()?.split("\\s+".toRegex())
                    ?: error("df failed for $path")
                val total = (parts.getOrNull(1)?.toLongOrNull() ?: 0L) * 1024
                val used = (parts.getOrNull(2)?.toLongOrNull() ?: 0L) * 1024
                val free = (parts.getOrNull(3)?.toLongOrNull() ?: 0L) * 1024
                StorageInfo(totalBytes = total, freeBytes = free, usedBytes = used)
            }
        }

    override suspend fun readText(path: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val lines = execOut("cat ${path.shellEscape()}")
                lines.joinToString("\n")
            }
        }

    override suspend fun writeText(path: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Write via a temporary file since libsu doesn't have a direct "write string to root file" easily without pipes
                // We'll use a shell redirection
                val escapedContent = content.replace("'", "'\\''")
                val result = exec("echo '$escapedContent' > ${path.shellEscape()}")
                if (!result.isSuccess) error("write failed: ${result.err.joinToString()}")
            }
        }

    override suspend fun calculateChecksum(path: String, algorithm: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cmd = when (algorithm.uppercase().replace("-", "")) {
                    "SHA256" -> "sha256sum"
                    "SHA1" -> "sha1sum"
                    "MD5" -> "md5sum"
                    else -> error("Unsupported algorithm: $algorithm")
                }
                val result = exec("$cmd ${path.shellEscape()}")
                if (!result.isSuccess) error("checksum failed: ${result.err.joinToString()}")
                result.out.firstOrNull()?.split(" ")?.firstOrNull()
                    ?: error("parse failed: ${result.out.joinToString()}")
            }
        }

    override suspend fun searchFiles(rootPath: String, query: String): Result<List<FileModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val escapedRoot = rootPath.shellEscape()
                val escapedQuery = "*${query}*".shellEscape()
                // find /path -maxdepth 5 -iname '*query*' -limit 100
                // Note: toybox find might not support -limit, we'll use head
                val lines = execOut("find $escapedRoot -maxdepth 5 -iname $escapedQuery | head -n 100")
                
                lines.mapNotNull { path ->
                    val fileResult = getFileInfo(path)
                    fileResult.getOrNull()
                }
            }
        }

    override suspend fun getCategoryFiles(category: filey.app.core.model.FileCategory): Result<List<FileModel>> =
        withContext(Dispatchers.IO) {
            // Root categories are harder without MediaStore access for root
            // We could use 'find' but it would be very slow for the entire storage.
            // For now, return empty or implement a limited search.
            Result.success(emptyList())
        }

    // ── Parsing helpers ─────────────────────────────────────

    private fun parseLsLine(line: String, parentPath: String): FileModel? {
        if (line.isBlank() || line.startsWith("total")) return null

        // Support both 8 and 9 column formats
        // Format A (Toybox): perms links owner group size date time name
        // Format B (Standard): perms links owner group size month day time name
        val parts = line.trim().split("\\s+".toRegex())
        if (parts.size < 7) return null

        val perms = parts[0]
        val name = parts.last()

        if (name == "." || name == "..") return null

        // Try to find indices by looking for typical patterns
        val isDir = perms.startsWith('d')
        val isLink = perms.startsWith('l')
        val isHidden = name.startsWith('.')

        // Size is usually at index 4 (0-based) in both 8 and 9 column formats
        // But let's be safer and take the one before the date/name
        val size = parts.getOrNull(4)?.toLongOrNull() ?: 0L
        val owner = parts.getOrNull(2) ?: "root"
        val group = parts.getOrNull(3) ?: "root"

        val actualName = if (isLink && name.contains(" -> ")) {
            name.substringBefore(" -> ")
        } else name

        val actualPath = if (parentPath == "/") "/$actualName" else "$parentPath/$actualName"
        val ext = if (isDir) "" else actualName.substringAfterLast('.', "").lowercase()

        // Attempt to parse date from common positions
        val lastMod = if (parts.size >= 7) {
            val dateIdx = parts.size - 3
            val timeIdx = parts.size - 2
            parseDateFromLs(parts[dateIdx], parts[timeIdx])
        } else 0L

        return FileModel(
            name = actualName,
            path = actualPath,
            isDirectory = isDir,
            size = if (isDir) 0L else size,
            lastModified = lastMod,
            isHidden = isHidden,
            extension = ext,
            mimeType = if (isDir) "" else FileUtils.getMimeType(actualPath),
            permissions = perms,
            owner = "$owner:$group"
        )
    }

    private fun parseDateFromLs(date: String, time: String): Long {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
            sdf.parse("$date $time")?.time ?: 0L
        } catch (_: Exception) { 0L }
    }

    private fun String.shellEscape(): String =
        "'${this.replace("'", "'\\''")}'"
}
