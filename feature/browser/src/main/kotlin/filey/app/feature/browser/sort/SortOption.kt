package filey.app.feature.browser.sort

import filey.app.core.model.FileModel

enum class SortField     { NAME, SIZE, DATE, EXTENSION }
enum class SortDirection { ASC, DESC }

data class SortOption(
    val field: SortField = SortField.NAME,
    val direction: SortDirection = SortDirection.ASC,
    val foldersFirst: Boolean = true
)

fun List<FileModel>.sorted(option: SortOption): List<FileModel> {
    val base: Comparator<FileModel> = when (option.field) {
        SortField.NAME      -> compareBy { it.name.lowercase() }
        SortField.SIZE      -> compareBy { it.size }
        SortField.DATE      -> compareBy { it.lastModified }
        SortField.EXTENSION -> compareBy { it.name.substringAfterLast('.', "").lowercase() }
    }
    val directed = if (option.direction == SortDirection.DESC) base.reversed() else base
    val final    = if (option.foldersFirst)
        compareByDescending<FileModel> { it.isDirectory }.then(directed)
    else directed
    return sortedWith(final)
}
