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
            if (!dir.exists() || !dir.isDirectory) {
                if (RootManager.isRootAvailable) {
                    // Root ile listeleme mantığı buraya eklenebilir
                }
                return@withContext emptyList()
            }
            val files = dir.listFiles() ?: return@withContext emptyList()
            files.filter { showHidden || !it.isHidden }
                .map { FileUtils.fileToModel(it) }
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
            var bytesCopied = 0L
            
            FileInputStream(src).use { input ->
                FileOutputStream(dst).use { output ->
                    val buffer = ByteArray(8192)
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        onProgress(bytesCopied.toFloat() / totalSize)
                        bytes = input.read(buffer)
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

    suspend fun moveFile(sourcePath: String, destDir: String, onProgress: (Float) -> Unit) {
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
        File(parentPath, name).mkdirs()
    }

    suspend fun createFile(parentPath: String, name: String) = withContext(Dispatchers.IO) {
        File(parentPath, name).createNewFile()
    }
}
