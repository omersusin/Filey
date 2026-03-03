package filey.app.feature.browser.sort

import filey.app.core.model.FileModel
import filey.app.core.model.SortOption as CoreSortOption

fun List<FileModel>.sortedWith(option: CoreSortOption): List<FileModel> {
    val sorted = when (option) {
        CoreSortOption.NAME_ASC  -> sortedBy { it.name.lowercase() }
        CoreSortOption.NAME_DESC -> sortedByDescending { it.name.lowercase() }
        CoreSortOption.DATE_ASC  -> sortedBy { it.lastModified }
        CoreSortOption.DATE_DESC -> sortedByDescending { it.lastModified }
        CoreSortOption.SIZE_ASC  -> sortedBy { it.size }
        CoreSortOption.SIZE_DESC -> sortedByDescending { it.size }
        CoreSortOption.TYPE_ASC  -> sortedBy { it.type.name }
        CoreSortOption.TYPE_DESC -> sortedByDescending { it.type.name }
    }
    // Klasörler her zaman üstte
    return sorted.sortedByDescending { it.isDirectory }
}
