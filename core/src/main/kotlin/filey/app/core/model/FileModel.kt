package filey.app.core.model

data class FileModel(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val extension: String,
    val type: FileType,
    val sizeFormatted: String,
    val dateFormatted: String,
    val childCount: Int = 0
)
