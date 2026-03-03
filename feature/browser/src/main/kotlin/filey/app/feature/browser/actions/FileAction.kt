package filey.app.feature.browser.actions

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import filey.app.core.model.FileModel

/**
 * A single action that can be performed on a file.
 * Register new actions → they automatically appear in FileOptionsSheet.
 */
interface FileAction {
    val id: String
    val title: String
    val icon: ImageVector

    fun isVisible(file: FileModel): Boolean = true

    /**
     * Execute the action.
     * Return an [ActionResult] so the UI knows what happened.
     */
    suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    ): ActionResult
}

interface FileActionCallback {
    fun showSnackbar(message: String)
    fun refreshDirectory()
    fun navigateTo(path: String)
    fun copyToClipboard(text: String)
    fun setClipboard(paths: List<String>, isCut: Boolean)
}
