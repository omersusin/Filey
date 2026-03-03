package filey.app.core.data

import filey.app.core.model.FileModel
import filey.app.core.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileRepository {

    suspend fun listFiles(path: String, showHidden: Boolean): List<FileModel> =
        withContext(Dispatchers.IO) {
            val dir = File(path)
            
            // Standart listeleme dene
            val files = dir.listFiles()
            if (files != null) {
                return@withContext files.filter { showHidden || !it.isHidden }
                    .map { FileUtils.fileToModel(it) }
            }

            // Standart listeleme başarısızsa ve Root varsa Root ile dene
            if (RootManager.isRootAvailable) {
                return@withContext RootManager.listAsRoot(path).map { name ->
                    val fullPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
                    // Root üzerinden meta veri çekmek maliyetli olduğu için temel bir model oluşturuyoruz
                    val isDirectory = RootManager.isDirectory(fullPath)
                    FileModel(
                        name = name,
                        path = fullPath,
                        size = 0L,
                        lastModified = 0L,
                        isDirectory = isDirectory,
                        isHidden = name.startsWith("."),
                        extension = name.substringAfterLast(".", ""),
                        type = FileUtils.getFileType(name.substringAfterLast(".", ""), isDirectory),
                        sizeFormatted = if (isDirectory) "--" else "Root Dosyası",
                        dateFormatted = "--",
                        childCount = 0
                    )
                }.filter { showHidden || !it.isHidden }
            }

            emptyList()
        }

    suspend fun copyFile(
        sourcePath: String, 
        destDir: String, 
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val src = File(sourcePath)
        val dst = File(destDir, src.name)
        
        if (src.isDirectory) {
            copyDirectory(src, dst, onProgress)
        } else {
            copySingleFile(src, dst, onProgress)
        }
    }

    private suspend fun copySingleFile(src: File, dst: File, onProgress: (Float) -> Unit) {
        try {
            val totalSize = src.length()
            if (totalSize == 0L) {
                if (src.exists()) dst.createNewFile()
                onProgress(1f)
                return
            }

            FileInputStream(src).use { fis ->
                FileOutputStream(dst).use { fos ->
                    val sourceChannel = fis.channel
                    val destChannel = fos.channel
                    val bufferSize = 1024 * 1024 // 1MB Buffer
                    var transferred = 0L
                    var lastReportedProgress = 0f

                    while (transferred < totalSize) {
                        val count = sourceChannel.transferTo(transferred, bufferSize.toLong(), destChannel)
                        if (count <= 0) break // Sonsuz döngü engeli
                        transferred += count
                        val currentProgress = transferred.toFloat() / totalSize
                        
                        if (currentProgress - lastReportedProgress >= 0.01f || currentProgress >= 0.99f) {
                            onProgress(currentProgress)
                            lastReportedProgress = currentProgress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (RootManager.isRootAvailable) {
                if (!RootManager.copyAsRoot(src.absolutePath, dst.absolutePath)) throw e
                onProgress(1f)
            } else throw e
        }
    }

    private suspend fun copyDirectory(src: File, dst: File, onProgress: (Float) -> Unit) {
        dst.mkdirs()
        val files = src.listFiles() ?: return
        files.forEach { file ->
            val target = File(dst, file.name)
            if (file.isDirectory) copyDirectory(file, target, onProgress)
            else copySingleFile(file, target, onProgress)
        }
    }

    suspend fun moveFile(sourcePath: String, destDir: String, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val src = File(sourcePath)
        val dst = File(destDir, src.name)
        
        if (src.renameTo(dst)) {
            onProgress(1f)
        } else {
            copyFile(sourcePath, destDir, onProgress)
            deleteFile(sourcePath)
        }
    }

    suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.deleteRecursively()) {
            if (RootManager.isRootAvailable) {
                RootManager.deleteAsRoot(path)
            }
        }
    }

    suspend fun renameFile(path: String, newName: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        val target = File(file.parent, newName)
        if (!file.renameTo(target)) {
            if (RootManager.isRootAvailable) {
                RootManager.moveAsRoot(path, target.absolutePath)
            }
        }
    }

    suspend fun createFolder(parentPath: String, name: String) = withContext(Dispatchers.IO) {
        if (!File(parentPath, name).mkdirs() && RootManager.isRootAvailable) {
            RootManager.execute("mkdir -p '$parentPath/$name'")
        }
    }

    suspend fun createFile(parentPath: String, name: String) = withContext(Dispatchers.IO) {
        if (!File(parentPath, name).createNewFile() && RootManager.isRootAvailable) {
            RootManager.execute("touch '$parentPath/$name'")
        }
    }
}
