package filey.app.feature.browser.actions

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.ui.graphics.vector.ImageVector
import filey.app.core.model.FileModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CompressAction : FileAction {
    override val id = "compress"
    override val title = "Zip'le"
    override val icon: ImageVector = Icons.Outlined.FolderZip

    override fun isVisible(file: FileModel): Boolean = file.name != "^"

    override suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    ): ActionResult = withContext(Dispatchers.IO) {
        try {
            val src = File(file.path)
            val zipFile = File(src.parent, "${src.name}.zip")
            
            // Avoid overwriting if possible or handle naming
            var targetZip = zipFile
            var counter = 1
            while (targetZip.exists()) {
                targetZip = File(src.parent, "${src.nameWithoutExtension}_$counter.zip")
                counter++
            }

            ZipOutputStream(FileOutputStream(targetZip)).use { zos ->
                if (src.isDirectory) {
                    compressDirectory(src, src, zos)
                } else {
                    compressFile(src, "", zos)
                }
            }
            
            callback.refreshDirectory()
            ActionResult.Success("compressed")
        } catch (e: Exception) {
            ActionResult.Error("Sıkıştırma hatası: ${e.message}")
        }
    }

    private fun compressDirectory(root: File, source: File, zos: ZipOutputStream) {
        source.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                compressDirectory(root, file, zos)
            } else {
                val relativePath = file.absolutePath.substring(root.absolutePath.length + 1)
                compressFile(file, relativePath, zos)
            }
        }
    }

    private fun compressFile(file: File, entryName: String, zos: ZipOutputStream) {
        val name = entryName.ifEmpty { file.name }
        val entry = ZipEntry(name)
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }
}
