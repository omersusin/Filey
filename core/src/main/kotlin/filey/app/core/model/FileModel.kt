package filey.app.core.model

data class FileModel(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val isHidden: Boolean = false,
    val extension: String = "",
    val mimeType: String = "",
    val permissions: String = "",
    val owner: String = "",
    val childCount: Int = 0,
    val tags: List<String> = emptyList(), // Added tags support
    val uri: String = "", // Added for content extraction
    val category: FileCategory = FileCategory.OTHER
)
