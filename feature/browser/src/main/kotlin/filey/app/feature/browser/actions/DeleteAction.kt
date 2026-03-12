package filey.app.feature.browser.actions

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.graphics.vector.ImageVector
import filey.app.core.model.FileModel

class DeleteAction : FileAction {
    override val id = "delete"
    override val title = "Sil"
    override val icon: ImageVector = Icons.Outlined.Delete

    override suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    ): ActionResult {
        return ActionResult.RequestDelete(file.path)
    }
}
