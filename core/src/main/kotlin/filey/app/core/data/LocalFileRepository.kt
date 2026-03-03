package filey.app.core.data

import filey.app.core.model.FileModel
import filey.app.core.model.FileResult
import filey.app.core.model.FileType
import filey.app.core.model.fileResultOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Sprint 1 için minimal local (java.io.File) repository implementasyonu.
 * Sprint 2'de DataSource/Strategy + Root/SAF'e bölünecek.
 */
class LocalFileRepository : FileRepository {

    override suspend fun listFiles(path: String): FileResult<List<FileModel>> = withContext(Dispatchers.IO) {
        fileResultOf {
            val dir = File(path)
            if (!dir.exists()) throw java.io.FileNotFoundException(path)
            val children = dir.listFiles() ?: throw SecurityException("Cannot list: $path")
            children.map { it.toFileModel() }
        }
    }

    override suspend fun copy(
        source: String,
        destination: String,
        onProgress: (FileProgress) -> Unit
    ): FileResult<Unit> = withContext(Dispatchers.IO) {
        fileResultOf {
            val src = File(source)
            if (!src.exists()) throw java.io.FileNotFoundException(source)

            val dst = File(destination)
            val totalBytes = if (src.isDirectory) dirSize(src) else src.length()
            var processed = 0L

            if (src.isDirectory) {
                copyDir(src, dst) { bytesDelta ->
                    processed += bytesDelta
                    onProgress(progressOf(src.name, processed, totalBytes))
                }
            } else {
                dst.parentFile?.mkdirs()
                src.inputStream().use { input ->
                    dst.outputStream().use { output ->
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            output.write(buf, 0, read)
                            processed += read
                            onProgress(progressOf(src.name, processed, totalBytes))
                        }
                    }
                }
            }
        }
    }

    override suspend fun move(source: String, destination: String): FileResult<Unit> = withContext(Dispatchers.IO) {
        fileResultOf {
            val src = File(source)
            if (!src.exists()) throw java.io.FileNotFoundException(source)
            val dst = File(destination)
            dst.parentFile?.mkdirs()
            val ok = src.renameTo(dst)
            if (!ok) {
                // renameTo bazen cross-volume başarısız olur: copy + delete fallback
                val copyRes = copy(source, destination) {}
                if (copyRes is FileResult.Error) throw java.io.IOException(copyRes.message)
                val delRes = delete(source)
                if (delRes is FileResult.Error) throw java.io.IOException(delRes.message)
            }
        }
    }

    override suspend fun delete(path: String): FileResult<Unit> = withContext(Dispatchers.IO) {
        fileResultOf {
            val f = File(path)
            if (!f.exists()) throw java.io.FileNotFoundException(path)
            val ok = if (f.isDirectory) f.deleteRecursively() else f.delete()
            if (!ok) throw java.io.IOException("Failed to delete: $path")
        }
    }

    override suspend fun rename(oldPath: String, newName: String): FileResult<String> = withContext(Dispatchers.IO) {
        fileResultOf {
            val src = File(oldPath)
            if (!src.exists()) throw java.io.FileNotFoundException(oldPath)
            val dst = File(src.parentFile, newName)
            val ok = src.renameTo(dst)
            if (!ok) throw java.io.IOException("Failed to rename: $oldPath")
            dst.absolutePath
        }
    }

    override suspend fun createFile(parentPath: String, name: String): FileResult<String> = withContext(Dispatchers.IO) {
        fileResultOf {
            val parent = File(parentPath)
            parent.mkdirs()
            val f = File(parent, name)
            val ok = if (f.exists()) true else f.createNewFile()
            if (!ok) throw java.io.IOException("Failed to create file: ${f.absolutePath}")
            f.absolutePath
        }
    }

    override suspend fun createDirectory(parentPath: String, name: String): FileResult<String> = withContext(Dispatchers.IO) {
        fileResultOf {
            val parent = File(parentPath)
            parent.mkdirs()
            val d = File(parent, name)
            val ok = if (d.exists()) true else d.mkdirs()
            if (!ok) throw java.io.IOException("Failed to create directory: ${d.absolutePath}")
            d.absolutePath
        }
    }

    override suspend fun getFileInfo(path: String): FileResult<FileModel> = withContext(Dispatchers.IO) {
        fileResultOf {
            val f = File(path)
            if (!f.exists()) throw java.io.FileNotFoundException(path)
            f.toFileModel()
        }
    }

    override fun search(rootPath: String, query: String): Flow<FileModel> = flow {
        val root = File(rootPath)
        if (!root.exists()) return@flow
        root.walkTopDown().forEach { f ->
            if (f.name.contains(query, ignoreCase = true)) emit(f.toFileModel())
        }
    }

    override suspend fun calculateSize(path: String): FileResult<Long> = withContext(Dispatchers.IO) {
        fileResultOf {
            val f = File(path)
            if (!f.exists()) throw java.io.FileNotFoundException(path)
            if (f.isDirectory) dirSize(f) else f.length()
        }
    }

    private fun progressOf(name: String, processed: Long, total: Long): FileProgress {
        return FileProgress(
            currentFile = name,
            currentFileIndex = 0,
            totalFiles = 1,
            bytesProcessed = processed,
            totalBytes = total
        )
    }

    private fun File.toFileModel(): FileModel {
        val ext = extensionOrEmpty()
        val lm = lastModified()
        val isDir = isDirectory
        val sizeBytes = if (isDir) 0L else length()
        val count = if (isDir) (list()?.size ?: 0) else 0

        return FileModel(
            name = name,
            path = absolutePath,
            size = sizeBytes,
            lastModified = lm,
            isDirectory = isDir,
            isHidden = isHidden,
            extension = ext,
            type = guessType(ext, isDir),
            sizeFormatted = formatBytes(sizeBytes),
            dateFormatted = formatDate(lm),
            childCount = count
        )
    }

    private fun File.extensionOrEmpty(): String {
        val n = name
        val dot = n.lastIndexOf('.')
        return if (dot > 0 && dot < n.length - 1 && !isDirectory) n.substring(dot + 1) else ""
    }

    private fun guessType(ext: String, isDir: Boolean): FileType {
        if (isDir) return FileType.DIRECTORY
        return when (ext.lowercase()) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic" -> FileType.IMAGE
            "mp4", "mkv", "webm", "avi", "mov" -> FileType.VIDEO
            "mp3", "wav", "m4a", "flac", "ogg" -> FileType.AUDIO
            "pdf" -> FileType.PDF
            "zip", "rar", "7z", "tar", "gz" -> FileType.ARCHIVE
            "txt", "md", "json", "xml", "csv", "log" -> FileType.TEXT
            "apk" -> FileType.APK
            else -> FileType.OTHER
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "${bytes} B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.getDefault(), "%.1f GB", gb)
    }

    private fun formatDate(millis: Long): String {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return df.format(Date(millis))
    }

    private fun dirSize(dir: File): Long {
        var total = 0L
        dir.walkTopDown().forEach { f ->
            if (f.isFile) total += f.length()
        }
        return total
    }

    private fun copyDir(src: File, dst: File, onBytes: (Long) -> Unit) {
        if (!dst.exists()) dst.mkdirs()
        src.listFiles()?.forEach { child ->
            val target = File(dst, child.name)
            if (child.isDirectory) {
                copyDir(child, target, onBytes)
            } else {
                target.parentFile?.mkdirs()
                child.inputStream().use { input ->
                    target.outputStream().use { output ->
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            output.write(buf, 0, read)
                            onBytes(read.toLong())
                        }
                    }
                }
            }
        }
    }
}
