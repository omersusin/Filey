package filey.app.core.data

import filey.app.core.model.FileModel
import filey.app.core.model.FileResult
import filey.app.core.model.fileResultOf
import java.io.File
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
                    onProgress(
                        FileProgress(
                            currentFile = src.name,
                            currentFileIndex = 0,
                            totalFiles = 1,
                            bytesProcessed = processed,
                            totalBytes = totalBytes
                        )
                    )
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
                            onProgress(
                                FileProgress(
                                    currentFile = src.name,
                                    currentFileIndex = 0,
                                    totalFiles = 1,
                                    bytesProcessed = processed,
                                    totalBytes = totalBytes
                                )
                            )
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

    private fun File.toFileModel(): FileModel {
        // Projedeki FileModel constructor'ı farklıysa, bunu çıktına göre düzeltiriz.
        return FileModel(
            path = absolutePath,
            name = name,
            size = if (isDirectory) 0L else length(),
            lastModified = lastModified(),
            isDirectory = isDirectory
        )
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
