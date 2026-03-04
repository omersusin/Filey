package filey.app.core.data

import android.content.Context
import android.os.StatFs
import android.provider.MediaStore
import filey.app.core.model.FileCategory
import filey.app.core.model.FileModel
import filey.app.core.model.FileType
import filey.app.core.model.FileUtils
import filey.app.core.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class NormalFileRepository(private val context: Context) : FileRepository {

    override suspend fun listFiles(path: String): Result<List<FileModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(path)
                if (!dir.exists()) error("Dizin bulunamadı: $path")
                if (!dir.isDirectory) error("Bir dizin değil: $path")
                if (!dir.canRead()) error("Okuma izni yok: $path")

                dir.listFiles()?.map { it.toFileModel() }?.sortedWith(
                    compareByDescending<FileModel> { it.isDirectory }
                        .thenBy { it.name.lowercase() }
                ) ?: emptyList()
            }
        }

    override suspend fun createDirectory(path: String, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val newDir = File(path, name)
                if (newDir.exists()) error("Zaten mevcut: ${newDir.path}")
                if (!newDir.mkdirs()) error("Klasör oluşturulamadı: ${newDir.path}")
            }
        }

    override suspend fun delete(path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                if (!file.exists()) error("Dosya bulunamadı: $path")
                if (!file.deleteRecursively()) error("Silinemedi: $path")
            }
        }

    override suspend fun delete(paths: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val failures = mutableListOf<String>()
                for (p in paths) {
                    val f = File(p)
                    if (f.exists() && !f.deleteRecursively()) {
                        failures.add(p)
                    }
                }
                if (failures.isNotEmpty()) {
                    error("Silinemeyenler: ${failures.joinToString()}")
                }
            }
        }

    override suspend fun rename(path: String, newName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val src = File(path)
                if (!src.exists()) error("Dosya bulunamadı: $path")
                val dst = File(src.parentFile, newName)
                if (dst.exists()) error("Zaten mevcut: ${dst.path}")
                if (!src.renameTo(dst)) error("Yeniden adlandırılamadı")
                dst.absolutePath
            }
        }

    override suspend fun copy(
        source: String,
        destination: String,
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val src = File(source)
            if (!src.exists()) error("Kaynak bulunamadı: $source")
            val dstDir = File(destination)
            val dst = File(dstDir, src.name)

            if (src.isDirectory) {
                copyDirectoryRecursive(src, dst, onProgress)
            } else {
                copySingleFile(src, dst, onProgress)
            }
        }
    }

    override suspend fun move(
        source: String,
        destination: String,
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val src = File(source)
            val dstDir = File(destination)
            val dst = File(dstDir, src.name)

            // Try rename first (same filesystem = instant)
            if (src.renameTo(dst)) return@runCatching

            // Fallback: copy + delete
            if (src.isDirectory) {
                copyDirectoryRecursive(src, dst, onProgress)
            } else {
                copySingleFile(src, dst, onProgress)
            }
            if (!src.deleteRecursively()) {
                error("Kaynak silinemedi: $source")
            }
        }
    }

    override suspend fun getFileInfo(path: String): Result<FileModel> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                if (!file.exists()) error("Dosya bulunamadı: $path")
                file.toFileModel()
            }
        }

    override suspend fun exists(path: String): Boolean =
        withContext(Dispatchers.IO) { File(path).exists() }

    override suspend fun getStorageInfo(path: String): Result<StorageInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val stat = StatFs(path)
                val total = stat.totalBytes
                val free = stat.availableBytes
                StorageInfo(
                    totalBytes = total,
                    freeBytes = free,
                    usedBytes = total - free
                )
            }
        }

    override suspend fun readText(path: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching { File(path).readText() }
        }

    override suspend fun writeText(path: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { File(path).writeText(content) }
        }

    override suspend fun calculateChecksum(path: String, algorithm: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val digest = MessageDigest.getInstance(algorithm)
                File(path).inputStream().use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }

    override suspend fun searchFiles(rootPath: String, query: String): Result<List<FileModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = File(rootPath)
                if (!root.exists() || !root.isDirectory) return@runCatching emptyList()

                val lowerQuery = query.lowercase()
                root.walkTopDown()
                    .maxDepth(5) // Limit depth for performance
                    .filter { it.name.lowercase().contains(lowerQuery) }
                    .take(100) // Limit results
                    .map { it.toFileModel() }
                    .toList()
            }
        }

    override suspend fun getCategoryFiles(category: FileCategory): Result<List<FileModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = when (category) {
                    FileCategory.IMAGES -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    FileCategory.VIDEOS -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    FileCategory.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Files.getContentUri("external")
                }

                val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
                
                val selection = when (category) {
                    FileCategory.DOCUMENTS -> {
                        "${MediaStore.Files.FileColumns.MIME_TYPE} = 'application/pdf' OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'text/%'"
                    }
                    FileCategory.APKS -> {
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.apk'"
                    }
                    FileCategory.ARCHIVES -> {
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.zip' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.rar' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.7z'"
                    }
                    else -> null
                }

                val result = mutableListOf<FileModel>()
                context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                    val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataIdx)
                        val file = File(path)
                        if (file.exists()) {
                            result.add(file.toFileModel())
                        }
                    }
                }
                result.sortedByDescending { it.lastModified }
            }
        }

    // ── Private helpers ─────────────────────────────────────

    private fun File.toFileModel(): FileModel {
        val childCount = if (isDirectory) (listFiles()?.size ?: 0) else 0
        return FileModel(
            name = name,
            path = absolutePath,
            isDirectory = isDirectory,
            size = if (isDirectory) 0L else length(),
            lastModified = lastModified(),
            isHidden = isHidden,
            extension = extension.lowercase(),
            mimeType = if (isDirectory) "" else FileUtils.getMimeType(absolutePath),
            childCount = childCount
        )
    }

    private fun copySingleFile(src: File, dst: File, onProgress: ((Float) -> Unit)?) {
        dst.parentFile?.mkdirs()
        val totalBytes = src.length()
        var copiedBytes = 0L

        src.inputStream().use { input ->
            dst.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    copiedBytes += bytesRead
                    if (totalBytes > 0) {
                        onProgress?.invoke(copiedBytes.toFloat() / totalBytes)
                    }
                }
            }
        }
    }

    private fun copyDirectoryRecursive(src: File, dst: File, onProgress: ((Float) -> Unit)?) {
        dst.mkdirs()
        val allFiles = src.walkTopDown().filter { it.isFile }.toList()
        val totalSize = allFiles.sumOf { it.length() }
        var copiedTotal = 0L

        for (file in allFiles) {
            val relativePath = file.relativeTo(src)
            val target = File(dst, relativePath.path)
            target.parentFile?.mkdirs()

            file.inputStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        copiedTotal += bytesRead
                        if (totalSize > 0) {
                            onProgress?.invoke(copiedTotal.toFloat() / totalSize)
                        }
                    }
                }
            }
        }
    }
}
