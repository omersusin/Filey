package filey.app.feature.browser.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import filey.app.core.model.FileType
import filey.app.core.navigation.Routes
import java.io.File

class FileRouter(
    private val navController: NavController,
    private val context: Context
) {
    fun openFile(filePath: String) {
        val fileName = filePath.substringAfterLast('/')
        when (FileType.fromFileName(fileName)) {
            FileType.IMAGE              -> navController.navigate(Routes.viewer(filePath))
            FileType.TEXT               -> navController.navigate(Routes.editor(filePath))
            FileType.VIDEO, FileType.AUDIO -> navController.navigate(Routes.player(filePath))
            FileType.ARCHIVE            -> navController.navigate(Routes.archive(filePath))
            FileType.OTHER, FileType.DIRECTORY, FileType.PDF, FileType.APK -> openWithSystemChooser(filePath)
        }
    }

    private fun openWithSystemChooser(filePath: String) {
        val file = File(filePath)
        val uri  = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Şununla aç…"))
    }
}
