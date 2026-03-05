package filey.app.core.data.root

import android.util.Base64
import com.topjohnwu.superuser.Shell
import filey.app.core.data.FileRepository
import filey.app.core.model.FileModel
import filey.app.core.model.FileUtils
import filey.app.core.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RootFileRepository : FileRepository {

    companion object {
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

    private fun exec(vararg cmds: String): Shell.Result = Shell.cmd(*cmds).exec()
    private fun execOut(vararg cmds: String): List<String> = Shell.cmd(*cmds).exec().out

    override suspend fun listFiles(path: String): Result<List<FileModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val escapedPath = path.shellEscape()
                val cmd = "find $escapedPath -maxdepth 1 -not -path $escapedPath -printf \"%y|%s|%T@|%p\\n\""
                val lines = execOut(cmd)

                lines.mapNotNull { parseFindLine(it) }
                    .sortedWith(
                        compareByDescending<FileModel> { it.isDirectory }
                            .thenBy { it.name.lowercase() }
                    )
            }
        }

    private fun parseFindLine(line: String): FileModel? {
        val parts = line.split("|")
        if (parts.size < 4) return null
        
        val typeChar = parts[0]
        val size = parts[1].toLongOrNull() ?: 0L
        val timestamp = (parts[2].toDoubleOrNull() ?: 0.0).toLong() * 1000
        val fullPath = parts.subList(3, parts.size).joinToString("|")
        
        val name = fullPath.substringAfterLast('/')
        if (name == "." || name == "..") return null
        
        val isDir = typeChar == "d"
        
        return FileModel(
            name = name,
            path = fullPath,
            isDirectory = isDir,
            size = if (isDir) 0L else size,
            lastModified = timestamp,
            isHidden = name.startsWith('.'),
            extension = if (isDir) "" else name.substringAfterLast('.', "").lowercase(),
            mimeType = if (isDir) "" else FileUtils.getMimeType(fullPath)
        )
    }

    override suspend fun createDirectory(path: String, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = "$path/$name".shellEscape()
                val result = exec("mkdir -p $target")
                if (!result.isSuccess) error("mkdir failed")
                Unit
            }
        }

    override suspend fun delete(path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = exec("rm -rf ${path.shellEscape()}")
                if (!result.isSuccess) error("rm failed")
                Unit
            }
        }

    override suspend fun delete(paths: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val joined = paths.joinToString(" ") { it.shellEscape() }
                val result = exec("rm -rf $joined")
                if (!result.isSuccess) error("rm failed")
                Unit
            }
        }

    override suspend fun rename(path: String, newName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val parent = path.substringBeforeLast('/')
                val newPath = if (parent.isEmpty()) "/$newName" else "$parent/$newName"
                val result = exec("mv ${path.shellEscape()} ${newPath.shellEscape()}")
                if (!result.isSuccess) error("mv failed")
                newPath
            }
        }

    override suspend fun copy(source: String, destination: String, onProgress: ((Float) -> Unit)?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = exec("cp -rf ${source.shellEscape()} ${destination.shellEscape()}/")
                if (!result.isSuccess) error("cp failed")
                onProgress?.invoke(1f)
                Unit
            }
        }

    override suspend fun move(source: String, destination: String, onProgress: ((Float) -> Unit)?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = exec("mv ${source.shellEscape()} ${destination.shellEscape()}/")
                if (!result.isSuccess) error("mv failed")
                onProgress?.invoke(1f)
                Unit
            }
        }

    override suspend fun getFileInfo(path: String): Result<FileModel> =
        withContext(Dispatchers.IO) {
            runCatching {
                val line = execOut("find ${path.shellEscape()} -maxdepth 0 -printf \"%y|%s|%T@|%p\"").firstOrNull()
                    ?: error("file not found")
                parseFindLine(line) ?: error("parse failed")
            }
        }

    override suspend fun exists(path: String): Boolean =
        withContext(Dispatchers.IO) { exec("test -e ${path.shellEscape()}").isSuccess }

    override suspend fun getStorageInfo(path: String): Result<StorageInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val lines = execOut("df ${path.shellEscape()}")
                val parts = lines.getOrNull(1)?.trim()?.split("\\s+".toRegex()) ?: error("df failed")
                val total = (parts.getOrNull(1)?.toLongOrNull() ?: 0L) * 1024
                val used = (parts.getOrNull(2)?.toLongOrNull() ?: 0L) * 1024
                val free = (parts.getOrNull(3)?.toLongOrNull() ?: 0L) * 1024
                StorageInfo(total, free, used)
            }
        }

    override suspend fun readText(path: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching { execOut("cat ${path.shellEscape()}").joinToString("\n") }
        }

    override suspend fun writeText(path: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val b64 = Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)
                val result = exec("echo '$b64' | base64 -d > ${path.shellEscape()}")
                if (!result.isSuccess) error("write failed: ${result.err.joinToString()}")
                Unit
            }
        }

    override suspend fun calculateChecksum(path: String, algorithm: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cmd = when (algorithm.uppercase().replace("-", "")) {
                    "SHA256" -> "sha256sum"
                    "SHA1" -> "sha1sum"
                    "MD5" -> "md5sum"
                    else -> error("Unsupported algorithm")
                }
                execOut("$cmd ${path.shellEscape()}").firstOrNull()?.split(" ")?.firstOrNull() ?: error("failed")
            }
        }

    override suspend fun searchFiles(rootPath: String, query: String): Result<List<FileModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val lines = execOut("find ${rootPath.shellEscape()} -maxdepth 5 -iname ${("*$query*").shellEscape()} -printf \"%y|%s|%T@|%p\\n\" | head -n 100")
                lines.mapNotNull { parseFindLine(it) }
            }
        }

    override suspend fun getCategoryFiles(category: filey.app.core.model.FileCategory): Result<List<FileModel>> =
        Result.success(emptyList())

    override suspend fun moveToTrash(path: String): Result<Unit> = delete(path)
    override suspend fun restoreFromTrash(path: String): Result<Unit> = Result.failure(Exception("Not supported"))
    override suspend fun getTrashFiles(): Result<List<FileModel>> = Result.success(emptyList())
    override suspend fun emptyTrash(): Result<Unit> = Result.success(Unit)

    override suspend fun getOwnerApp(path: String): String? = null

    private fun String.shellEscape(): String = "'${this.replace("'", "'\\''")}'"
}
