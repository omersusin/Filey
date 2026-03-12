package filey.app.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class FileCategory(
    val label: String,
    val icon: ImageVector,
    val types: Set<FileType>
) {
    IMAGES("Resimler", Icons.Outlined.Image, setOf(FileType.IMAGE)),
    VIDEOS("Videolar", Icons.Outlined.Movie, setOf(FileType.VIDEO)),
    AUDIO("Müzikler", Icons.Outlined.MusicNote, setOf(FileType.AUDIO)),
    DOCUMENTS("Belgeler", Icons.Outlined.Description, setOf(FileType.TEXT, FileType.PDF)),
    APKS("Uygulamalar", Icons.Outlined.Android, setOf(FileType.APK)),
    ARCHIVES("Arşivler", Icons.Outlined.FolderZip, setOf(FileType.ARCHIVE)),
    OTHER("Diğer", Icons.Outlined.InsertDriveFile, setOf(FileType.OTHER)),
    ALL("Tümü", Icons.Outlined.Folder, emptySet())
}
