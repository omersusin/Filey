package filey.app.core.model

import android.webkit.MimeTypeMap
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    private val textExtensions = setOf(
        "txt", "md", "json", "xml", "html", "htm", "css", "js", "ts",
        "kt", "java", "py", "c", "cpp", "h", "hpp", "sh", "bat",
        "yml", "yaml", "toml", "ini", "cfg", "conf", "log", "csv",
        "gradle", "properties", "pro", "gitignore", "env"
    )

    private val archiveExtensions = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "lz4", "zst"
    )

    fun getMimeType(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }

    fun getFileType(path: String, isDirectory: Boolean): FileType {
        if (isDirectory) return FileType.DIRECTORY
        val ext = path.substringAfterLast('.', "").lowercase()
        val mime = getMimeType(path)
        return when {
            mime.startsWith("image/") -> FileType.IMAGE
            mime.startsWith("video/") -> FileType.VIDEO
            mime.startsWith("audio/") -> FileType.AUDIO
            mime.startsWith("text/") || ext in textExtensions -> FileType.TEXT
            ext in archiveExtensions -> FileType.ARCHIVE
            ext == "apk" -> FileType.APK
            ext == "pdf" || mime == "application/pdf" -> FileType.PDF
            else -> FileType.OTHER
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val group = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceAtMost(units.lastIndex)
        val value = bytes / Math.pow(1024.0, group.toDouble())
        return "${DecimalFormat("#,##0.#").format(value)} ${units[group]}"
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
