package filey.app.core.data

import android.content.Context
import android.os.Build
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
                if (!file.canWrite()) {
                    val parent = file.parentFile
                    if (parent != null && !parent.canWrite()) {
                        error("Klasör yazma iznine sahip değil (Android kısıtlaması)")
                    } else {
                        error("Dosya yazma korumalı veya başka bir uygulama tarafından kilitli")
                    }
                }
                if (!file.deleteRecursively()) error("Silme işlemi başarısız (Dosya meşgul olabilir)")
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

    override suspend fun copy(source: String, destination: String, onProgress: ((Float) -> Unit)?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val src = File(source)
                if (!src.exists()) error("Kaynak bulunamadı: $source")
                val dstDir = File(destination)
                val dst = File(dstDir, src.name)
                if (src.isDirectory) copyDirectoryRecursive(src, dst, onProgress)
                else copySingleFile(src, dst, onProgress)
            }
        }

    override suspend fun move(source: String, destination: String, onProgress: ((Float) -> Unit)?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val src = File(source)
                val dstDir = File(destination)
                val dst = File(dstDir, src.name)
                if (src.renameTo(dst)) return@runCatching
                if (src.isDirectory) copyDirectoryRecursive(src, dst, onProgress)
                else copySingleFile(src, dst, onProgress)
                if (!src.deleteRecursively()) error("Kaynak silinemedi")
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
                StorageInfo(total, free, total - free)
            }
        }

    override suspend fun readText(path: String): Result<String> =
        withContext(Dispatchers.IO) { runCatching { File(path).readText() } }

    override suspend fun writeText(path: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { File(path).writeText(content) } }

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
                root.walkTopDown().maxDepth(5).filter { it.name.lowercase().contains(lowerQuery) }.take(100).map { it.toFileModel() }.toList()
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
                    FileCategory.DOCUMENTS -> "${MediaStore.Files.FileColumns.MIME_TYPE} = 'application/pdf' OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'text/%'"
                    FileCategory.APKS -> "${MediaStore.Files.FileColumns.DATA} LIKE '%.apk'"
                    FileCategory.ARCHIVES -> "${MediaStore.Files.FileColumns.DATA} LIKE '%.zip' OR ${MediaStore.Files.FileColumns.DATA} LIKE '%.rar' OR ${MediaStore.Files.FileColumns.DATA} LIKE '%.7z'"
                    else -> null
                }
                val result = mutableListOf<FileModel>()
                context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                    val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataIdx)
                        val file = File(path)
                        if (file.exists()) result.add(file.toFileModel())
                    }
                }
                result.sortedByDescending { it.lastModified }
            }
        }

    private val trashDir by lazy { File(context.getExternalFilesDir(null), ".trash").apply { mkdirs() } }

    override suspend fun moveToTrash(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val src = File(path)
            if (!src.exists()) error("Dosya bulunamadı")
            val encodedPath = android.util.Base64.encodeToString(src.absolutePath.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val trashFile = File(trashDir, "${System.currentTimeMillis()}_$encodedPath")
            if (!src.renameTo(trashFile)) error("Çöp kutusuna taşınamadı")
        }
    }

    override suspend fun restoreFromTrash(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val trashFile = File(path)
            val name = trashFile.name
            val encodedPath = name.substringAfter('_')
            val originalPath = String(android.util.Base64.decode(encodedPath, android.util.Base64.URL_SAFE))
            val dst = File(originalPath)
            dst.parentFile?.mkdirs()
            if (!trashFile.renameTo(dst)) error("Geri yüklenemedi")
        }
    }

    override suspend fun getTrashFiles(): Result<List<FileModel>> = withContext(Dispatchers.IO) {
        runCatching {
            trashDir.listFiles()?.map { file ->
                val name = file.name
                val encodedPath = name.substringAfter('_')
                val originalPath = String(android.util.Base64.decode(encodedPath, android.util.Base64.URL_SAFE))
                FileModel(originalPath.substringAfterLast('/'), file.absolutePath, false, file.length(), file.lastModified())
            }?.sortedByDescending { it.lastModified } ?: emptyList()
        }
    }

    override suspend fun emptyTrash(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { trashDir.deleteRecursively(); trashDir.mkdirs(); Unit }
    }

    override suspend fun getOwnerApp(path: String): String? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.OWNER_PACKAGE_NAME)
        val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
        val selectionArgs = arrayOf(path)
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun File.toFileModel(): FileModel = FileModel(
        name = name, path = absolutePath, isDirectory = isDirectory,
        size = if (isDirectory) 0L else length(), lastModified = lastModified(),
        isHidden = isHidden, extension = extension.lowercase(),
        mimeType = if (isDirectory) "" else FileUtils.getMimeType(absolutePath),
        childCount = 0
    )

    private fun copySingleFile(src: File, dst: File, onProgress: ((Float) -> Unit)?) {
        dst.parentFile?.mkdirs()
        val total = src.length()
        var copied = 0L
        src.inputStream().use { input ->
            dst.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var n: Int
                while (input.read(buffer).also { n = it } != -1) {
                    output.write(buffer, 0, n)
                    copied += n
                    if (total > 0) onProgress?.invoke(copied.toFloat() / total)
                }
            }
        }
    }

    private fun copyDirectoryRecursive(src: File, dst: File, onProgress: ((Float) -> Unit)?) {
        dst.mkdirs()
        val total = src.walkTopDown().filter { it.isFile }.fold(0L) { acc, f -> acc + f.length() }
        var copied = 0L
        src.walkTopDown().filter { it.isFile }.forEach { file ->
            val target = File(dst, file.relativeTo(src).path)
            target.parentFile?.mkdirs()
            file.inputStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        output.write(buffer, 0, n)
                        copied += n
                        if (total > 0) onProgress?.invoke(copied.toFloat() / total)
                    }
                }
            }
        }
    }
}
