package filey.app.core.data.shizuku

import android.util.Base64
import filey.app.core.data.FileRepository
import filey.app.core.model.FileModel
import filey.app.core.model.FileUtils
import filey.app.core.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShizukuFileRepository : FileRepository {

    private fun exec(cmd: String): Triple<List<String>, List<String>, Int> =
        ShizukuManager.exec(cmd)

    override suspend fun listFiles(path: String): Result<List<FileModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val escapedPath = path.shellEscape()
                val cmd = "find $escapedPath -maxdepth 1 -not -path $escapedPath -printf \"%y|%s|%T@|%p\\n\""
                val (stdout, stderr, code) = exec(cmd)
                if (code != 0 && stdout.isEmpty()) {
                    error("find failed: ${stderr.joinToString()}")
                }
                stdout.mapNotNull { parseFindLine(it) }
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
            name = name, path = fullPath, isDirectory = isDir,
            size = if (isDir) 0L else size, lastModified = timestamp,
            isHidden = name.startsWith('.'),
            extension = if (isDir) "" else name.substringAfterLast('.', "").lowercase(),
            mimeType = if (isDir) "" else FileUtils.getMimeType(fullPath)
        )
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

    override suspend fun copy(source: String, destination: String, onProgress: ((Float) -> Unit)?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (_, stderr, code) = exec("cp -rf ${source.shellEscape()} ${destination.shellEscape()}/")
                if (code != 0) error("cp failed: ${stderr.joinToString()}")
                onProgress?.invoke(1f)
            }
        }

    override suspend fun move(source: String, destination: String, onProgress: ((Float) -> Unit)?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (_, stderr, code) = exec("mv ${source.shellEscape()} ${destination.shellEscape()}/")
                if (code != 0) error("mv failed: ${stderr.joinToString()}")
                onProgress?.invoke(1f)
            }
        }

    override suspend fun getFileInfo(path: String): Result<FileModel> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (stdout, _, _) = exec("find ${path.shellEscape()} -maxdepth 0 -printf \"%y|%s|%T@|%p\"")
                val line = stdout.firstOrNull() ?: error("stat failed")
                parseFindLine(line) ?: error("parse failed")
            }
        }

    override suspend fun exists(path: String): Boolean =
        withContext(Dispatchers.IO) { exec("test -e ${path.shellEscape()}").third == 0 }

    override suspend fun getStorageInfo(path: String): Result<StorageInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (stdout, _, _) = exec("df ${path.shellEscape()}")
                val parts = stdout.getOrNull(1)?.trim()?.split("\\s+".toRegex()) ?: error("df failed")
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
                val b64 = Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)
                val (_, stderr, code) = exec("echo '$b64' | base64 -d > ${path.shellEscape()}")
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
                    else -> error("Unsupported algorithm")
                }
                val (stdout, _, _) = exec("$cmd ${path.shellEscape()}")
                stdout.firstOrNull()?.split(" ")?.firstOrNull() ?: error("checksum failed")
            }
        }

    override suspend fun searchFiles(rootPath: String, query: String): Result<List<FileModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (stdout, _, _) = exec("find ${rootPath.shellEscape()} -maxdepth 5 -iname ${("*$query*").shellEscape()} -printf \"%y|%s|%T@|%p\\n\" | head -n 100")
                stdout.mapNotNull { parseFindLine(it) }
            }
        }

    override suspend fun getCategoryFiles(category: filey.app.core.model.FileCategory): Result<List<FileModel>> =
        Result.success(emptyList())

    override suspend fun moveToTrash(path: String) = delete(path)
    override suspend fun restoreFromTrash(path: String) = Result.failure<Unit>(Exception("Not supported"))
    override suspend fun getTrashFiles() = Result.success(emptyList<FileModel>())
    override suspend fun emptyTrash() = Result.success(Unit)

    private fun String.shellEscape(): String = "'${this.replace("'", "'\\''")}'"
}
