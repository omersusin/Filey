package filey.app.feature.browser.actions

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.FileProvider
import filey.app.core.model.FileModel
import filey.app.core.model.FileUtils
import java.io.File

class OpenWithAction : FileAction {
    override val id = "open_with"
    override val title = "Birlikte Aç"
    override val icon: ImageVector = Icons.Outlined.OpenInNew

    override fun isVisible(file: FileModel): Boolean = !file.isDirectory

    override suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    ): ActionResult {
        return try {
            val src = File(file.path)
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val dst = File(sharedDir, src.name)
            src.inputStream().use { i -> dst.outputStream().use { o -> i.copyTo(o) } }

            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", dst
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, FileUtils.getMimeType(file.path))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, file.name))
            ActionResult.Success("opened")
        } catch (e: Exception) {
            ActionResult.Error("Açılamadı: ${e.message}")
        }
    }
}
