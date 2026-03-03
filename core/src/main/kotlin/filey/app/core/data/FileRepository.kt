package filey.app.core.data

import filey.app.core.model.FileModel
import filey.app.core.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileRepository {

    suspend fun listFiles(path: String, showHidden: Boolean): List<FileModel> =
        withContext(Dispatchers.IO) {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
            val files = dir.listFiles() ?: return@withContext emptyList()
            files.filter { showHidden || !it.isHidden }
                .map { FileUtils.fileToModel(it) }
        }

    suspend fun createFolder(parentPath: String, name: String) = withContext(Dispatchers.IO) {
        val folder = File(parentPath, name)
        if (!folder.mkdirs() && !folder.exists()) throw Exception("Klasör oluşturulamadı")
    }

    suspend fun createFile(parentPath: String, name: String) = withContext(Dispatchers.IO) {
        val file = File(parentPath, name)
        if (!file.createNewFile() && !file.exists()) throw Exception("Dosya oluşturulamadı")
    }

    suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        val ok = if (file.isDirectory) file.deleteRecursively() else file.delete()
        if (!ok) throw Exception("Silinemedi")
    }

    suspend fun renameFile(path: String, newName: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        val target = File(file.parent, newName)
        if (!file.renameTo(target)) throw Exception("Yeniden adlandırılamadı")
    }

    suspend fun copyFile(sourcePath: String, destDir: String) = withContext(Dispatchers.IO) {
        val src = File(sourcePath)
        val dst = File(destDir, src.name)
        if (src.isDirectory) src.copyRecursively(dst, overwrite = true)
        else src.copyTo(dst, overwrite = true)
    }

    suspend fun moveFile(sourcePath: String, destDir: String) = withContext(Dispatchers.IO) {
        copyFile(sourcePath, destDir)
        deleteFile(sourcePath)
    }
}
