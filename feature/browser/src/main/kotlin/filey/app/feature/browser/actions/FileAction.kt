package filey.app.feature.browser.actions

import android.content.Context
import filey.app.core.model.FileModel

interface FileAction {
    val title: String
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    val isPrimary: Boolean get() = false

    suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    )
}

interface FileActionCallback {
    fun showSnackbar(message: String)
    fun refreshDirectory()
    fun navigateTo(path: String)
    fun copyToClipboard(text: String)
    fun setClipboard(paths: List<String>, isCut: Boolean)
    fun onSuccess(message: String)
    fun onError(message: String)
}
