package filey.app.feature.browser.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import filey.app.core.model.FileModel
import filey.app.core.model.FileType
import filey.app.core.navigation.Routes
import java.io.File

class FileRouter(
    private val navController: NavController,
    private val context: Context
) {
    fun openFile(file: FileModel) {
        when (file.type) {
            FileType.IMAGE         -> navController.navigate(Routes.viewer(file.path))
            FileType.TEXT          -> navController.navigate(Routes.editor(file.path))
            FileType.VIDEO,
            FileType.AUDIO         -> navController.navigate(Routes.player(file.path))
            FileType.ARCHIVE       -> navController.navigate(Routes.archive(file.path))
            FileType.DIRECTORY,
            FileType.PDF,
            FileType.APK,
            FileType.OTHER,
            FileType.UNKNOWN       -> openWithSystemChooser(file.path)
        }
    }

    private fun openWithSystemChooser(filePath: String) {
        val uri = Uri.fromFile(File(filePath))
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Şununla aç…"))
    }
}
