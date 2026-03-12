package filey.app.feature.browser.actions

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.FileProvider
import filey.app.core.model.FileModel
import filey.app.core.model.FileUtils
import java.io.File

class ShareAction : FileAction {
    override val id = "share"
    override val title = "Paylaş"
    override val icon: ImageVector = Icons.Outlined.Share

    override fun isVisible(file: FileModel): Boolean = !file.isDirectory

    override suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    ): ActionResult {
        return try {
            val src = File(file.path)
            if (!src.exists()) return ActionResult.Error("Dosya bulunamadı: ${file.name}")

            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val dst = File(sharedDir, src.name)
            src.inputStream().use { i -> dst.outputStream().use { o -> i.copyTo(o) } }

            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", dst
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = FileUtils.getMimeType(file.path)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Paylaş: ${file.name}"))
            ActionResult.Success("shared")
        } catch (e: Exception) {
            ActionResult.Error("Paylaşım hatası: ${e.message ?: "Bilinmeyen"}")
        }
    }
}
