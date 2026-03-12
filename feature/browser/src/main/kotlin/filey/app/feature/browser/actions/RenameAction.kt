package filey.app.feature.browser.actions

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.ui.graphics.vector.ImageVector
import filey.app.core.model.FileModel

class RenameAction : FileAction {
    override val id = "rename"
    override val title = "Yeniden Adlandır"
    override val icon: ImageVector = Icons.Outlined.DriveFileRenameOutline

    override suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    ): ActionResult {
        return ActionResult.RequestRename(file.path)
    }
}
