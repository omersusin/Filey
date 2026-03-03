package filey.app.feature.browser.actions

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.ui.graphics.vector.ImageVector
import filey.app.core.model.FileModel
import java.io.FileInputStream
import java.security.MessageDigest

class ChecksumAction : FileAction {
    override val id = "checksum"
    override val title = "SHA-256 Checksum"
    override val icon: ImageVector = Icons.Outlined.Fingerprint

    override fun isVisible(file: FileModel): Boolean = !file.isDirectory

    override suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    ): ActionResult {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val javaFile = java.io.File(file.path)
            if (!javaFile.exists()) return ActionResult.Error("Dosya bulunamadı")
            
            FileInputStream(javaFile).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            callback.copyToClipboard(hash)
            ActionResult.Success("SHA-256 panoya kopyalandı")
        } catch (e: Exception) {
            ActionResult.Error("Checksum hatası: ${e.message}")
        }
    }
}
