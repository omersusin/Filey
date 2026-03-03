package filey.app.feature.browser.actions

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.vector.ImageVector
import filey.app.core.model.FileModel

class PropertiesAction : FileAction {
    override val id = "properties"
    override val title = "Özellikler"
    override val icon: ImageVector = Icons.Outlined.Info

    override suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    ): ActionResult {
        return ActionResult.RequestProperties(file.path)
    }
}
