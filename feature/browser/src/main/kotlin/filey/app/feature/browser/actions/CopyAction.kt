package filey.app.feature.browser.actions

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.ui.graphics.vector.ImageVector
import filey.app.core.model.FileModel

class CopyAction : FileAction {
    override val id = "copy"
    override val title = "Kopyala"
    override val icon: ImageVector = Icons.Outlined.ContentCopy

    override suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    ): ActionResult {
        callback.setClipboard(listOf(file.path), isCut = false)
        callback.showSnackbar("Panoya kopyalandı: ${file.name}")
        return ActionResult.Success("copied")
    }
}
