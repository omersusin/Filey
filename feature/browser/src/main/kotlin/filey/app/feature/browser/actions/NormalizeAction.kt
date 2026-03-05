package filey.app.feature.browser.actions

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.ui.graphics.vector.ImageVector
import filey.app.core.model.FileModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder

class NormalizeAction : FileAction {
    override val id = "normalize"
    override val title = "İsim Temizle"
    override val icon: ImageVector = Icons.Outlined.AutoFixHigh

    override fun isVisible(file: FileModel): Boolean = file.name != "^"

    override suspend fun execute(
        context: Context,
        file: FileModel,
        callback: FileActionCallback
    ): ActionResult = withContext(Dispatchers.IO) {
        try {
            val originalName = file.name
            // 1. URL Decoding (%20 -> space etc)
            var cleanName = try { URLDecoder.decode(originalName, "UTF-8") } catch (_: Exception) { originalName }
            
            // 2. Remove common mess patterns like (1), _copy, -1 etc
            cleanName = cleanName.replace(Regex("""\s*\(\d+\)\s*"""), "")
                .replace(Regex("""\s*_copy\s*"""), "")
                .replace("_", " ")
                .replace("-", " ")
            
            // 3. Normalize spaces
            cleanName = cleanName.trim().replace(Regex("""\s+"""), " ")
            
            // 4. Capitalize (optional but looks better)
            cleanName = cleanName.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

            if (cleanName == originalName) {
                return@withContext ActionResult.Success("already_clean")
            }

            val parent = File(file.path).parent
            val result = callback.refreshDirectory() // trigger refresh before rename to avoid conflicts
            
            val renameResult = filey.app.core.di.AppContainer.Instance.fileRepository.rename(file.path, cleanName)
            
            renameResult.fold(
                onSuccess = { 
                    callback.refreshDirectory()
                    ActionResult.Success("normalized") 
                },
                onFailure = { ActionResult.Error("Temizleme hatası: ${it.message}") }
            )
        } catch (e: Exception) {
            ActionResult.Error("Hata: ${e.message}")
        }
    }
}
