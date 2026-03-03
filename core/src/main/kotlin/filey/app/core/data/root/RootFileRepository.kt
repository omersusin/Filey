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
                    .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
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
                val lines = execOut("ls -la $escapedPath")

                // first line is usually "total ..."
                lines.drop(1).mapNotNull { parseLsLine(it, path) }
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
                val lines = execOut("$cmd ${path.shellEscape()}")
                lines.firstOrNull()?.split(" ")?.firstOrNull()
                    ?: error("checksum failed")
            }
        }

    // ── Parsing helpers ─────────────────────────────────────

    private fun parseLsLine(line: String, parentPath: String): FileModel? {
        if (line.isBlank() || line.startsWith("total")) return null

        val parts = line.trim().split("\\s+".toRegex(), limit = 9)
        if (parts.size < 9) return null

        val perms = parts[0]
        val owner = parts[2]
        val group = parts[3]
        val size = parts[4].toLongOrNull() ?: 0L
        val name = parts[8]

        if (name == "." || name == "..") return null

        val isDir = perms.startsWith('d')
        val isLink = perms.startsWith('l')
        val isHidden = name.startsWith('.')

        val actualName = if (isLink && name.contains(" -> ")) {
            name.substringBefore(" -> ")
        } else name

        val actualPath = if (parentPath == "/") "/$actualName" else "$parentPath/$actualName"
        val ext = if (isDir) "" else actualName.substringAfterLast('.', "").lowercase()

        return FileModel(
            name = actualName,
            path = actualPath,
            isDirectory = isDir,
            size = if (isDir) 0L else size,
            lastModified = parseDateFromLs(parts[5], parts[6]),
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
