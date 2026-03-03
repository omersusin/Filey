package filey.app.feature.browser.actions

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.ui.graphics.vector.ImageVector
import filey.app.core.model.FileModel

class CutAction : FileAction {
    override val id = "cut"
    override val title = "Kes"
    override val icon: ImageVector = Icons.Outlined.ContentCut

    override suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    ): ActionResult {
        callback.setClipboard(listOf(file.path), isCut = true)
        callback.showSnackbar("Kesildi: ${file.name}")
        return ActionResult.Success("cut")
    }
}
