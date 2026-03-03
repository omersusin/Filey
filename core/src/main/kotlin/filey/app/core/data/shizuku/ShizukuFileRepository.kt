package filey.app.core.data.shizuku

import filey.app.core.data.FileRepository
import filey.app.core.model.FileModel
import filey.app.core.model.FileUtils
import filey.app.core.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * File operations executed with Shizuku (ADB-level) permissions.
 * Very similar to RootFileRepository but uses ShizukuManager.exec().
 */
class ShizukuFileRepository : FileRepository {

    private fun exec(cmd: String): Triple<List<String>, List<String>, Int> =
        ShizukuManager.exec(cmd)

    override suspend fun listFiles(path: String): Result<List<FileModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val escapedPath = path.shellEscape()
                val (stdout, stderr, code) = exec("ls -la $escapedPath")
                if (code != 0 && stdout.isEmpty()) {
                    error("ls failed: ${stderr.joinToString()}")
                }
                stdout.drop(1).mapNotNull { parseLsLine(it, path) }
                    .sortedWith(
                        compareByDescending<FileModel> { it.isDirectory }
                            .thenBy { it.name.lowercase() }
                    )
            }
        }

    override suspend fun createDirectory(path: String, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (_, stderr, code) = exec("mkdir -p ${("$path/$name").shellEscape()}")
                if (code != 0) error("mkdir failed: ${stderr.joinToString()}")
            }
        }

    override suspend fun delete(path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (_, stderr, code) = exec("rm -rf ${path.shellEscape()}")
                if (code != 0) error("rm failed: ${stderr.joinToString()}")
            }
        }

    override suspend fun delete(paths: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val joined = paths.joinToString(" ") { it.shellEscape() }
                val (_, stderr, code) = exec("rm -rf $joined")
                if (code != 0) error("rm failed: ${stderr.joinToString()}")
            }
        }

    override suspend fun rename(path: String, newName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val parent = path.substringBeforeLast('/')
                val newPath = if (parent == "") "/$newName" else "$parent/$newName"
                val (_, stderr, code) = exec("mv ${path.shellEscape()} ${newPath.shellEscape()}")
                if (code != 0) error("mv failed: ${stderr.joinToString()}")
                newPath
            }
        }

    override suspend fun copy(
        source: String,
        destination: String,
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (_, stderr, code) = exec("cp -rf ${source.shellEscape()} ${destination.shellEscape()}/")
            if (code != 0) error("cp failed: ${stderr.joinToString()}")
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
            val (_, stderr, code) = exec("mv ${source.shellEscape()} ${destination.shellEscape()}/")
            if (code != 0) error("mv failed: ${stderr.joinToString()}")
            onProgress?.invoke(1f)
            Unit
        }
    }

    override suspend fun getFileInfo(path: String): Result<FileModel> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (stdout, _, _) = exec("ls -lad ${path.shellEscape()}")
                val line = stdout.firstOrNull() ?: error("stat failed")
                val parentDir = path.substringBeforeLast('/')
                parseLsLine(line, parentDir) ?: error("parse failed: $line")
            }
        }

    override suspend fun exists(path: String): Boolean =
        withContext(Dispatchers.IO) {
            exec("test -e ${path.shellEscape()}").third == 0
        }

    override suspend fun getStorageInfo(path: String): Result<StorageInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (stdout, _, _) = exec("df ${path.shellEscape()}")
                val parts = stdout.getOrNull(1)?.trim()?.split("\\s+".toRegex())
                    ?: error("df failed")
                val total = (parts.getOrNull(1)?.toLongOrNull() ?: 0L) * 1024
                val used = (parts.getOrNull(2)?.toLongOrNull() ?: 0L) * 1024
                val free = (parts.getOrNull(3)?.toLongOrNull() ?: 0L) * 1024
                StorageInfo(total, free, used)
            }
        }

    override suspend fun readText(path: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (stdout, stderr, code) = exec("cat ${path.shellEscape()}")
                if (code != 0) error("cat failed: ${stderr.joinToString()}")
                stdout.joinToString("\n")
            }
        }

    override suspend fun writeText(path: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val escapedContent = content.replace("'", "'\\''")
                val (_, stderr, code) = exec("echo '$escapedContent' > ${path.shellEscape()}")
                if (code != 0) error("write failed: ${stderr.joinToString()}")
            }
        }

    override suspend fun calculateChecksum(path: String, algorithm: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cmd = when (algorithm.uppercase().replace("-", "")) {
                    "SHA256" -> "sha256sum"
                    "SHA1" -> "sha1sum"
                    "MD5" -> "md5sum"
                    else -> error("Unsupported: $algorithm")
                }
                val (stdout, _, _) = exec("$cmd ${path.shellEscape()}")
                stdout.firstOrNull()?.split(" ")?.firstOrNull() ?: error("checksum failed")
            }
        }

    // ── Parsing (same logic as RootFileRepository) ──────────

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
        val actualName = if (isLink && name.contains(" -> ")) name.substringBefore(" -> ") else name
        val actualPath = if (parentPath == "/") "/$actualName" else "$parentPath/$actualName"
        val ext = if (isDir) "" else actualName.substringAfterLast('.', "").lowercase()

        return FileModel(
            name = actualName, path = actualPath, isDirectory = isDir,
            size = if (isDir) 0L else size,
            lastModified = parseDateFromLs(parts[5], parts[6]),
            isHidden = actualName.startsWith('.'),
            extension = ext,
            mimeType = if (isDir) "" else FileUtils.getMimeType(actualPath),
            permissions = perms, owner = "$owner:$group"
        )
    }

    private fun parseDateFromLs(date: String, time: String): Long = try {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
            .parse("$date $time")?.time ?: 0L
    } catch (_: Exception) { 0L }

    private fun String.shellEscape(): String = "'${this.replace("'", "'\\''")}'"
}
