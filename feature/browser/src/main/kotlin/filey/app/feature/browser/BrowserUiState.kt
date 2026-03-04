package filey.app.feature.browser

import filey.app.core.model.*

data class BrowserUiState(
    val currentPath: String = DEFAULT_PATH,
    val files: List<FileModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,

    // Search
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,

    // Display
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val showHiddenFiles: Boolean = false,

    // Selection
    val selectedFiles: Set<String> = emptySet(),
    val isMultiSelectActive: Boolean = false,

    // Clipboard
    val clipboard: ClipboardData? = null,

    // Navigation
    val pathSegments: List<PathSegment> = emptyList(),
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,

    // Access mode
    val accessMode: AccessMode = AccessMode.NORMAL,

    // Operation progress
    val operationMessage: String? = null
) {
    /** Files after filtering (search + hidden) and sorting */
    val displayFiles: List<FileModel>
        get() {
            var result = files

            // Hidden filter
            if (!showHiddenFiles) {
                result = result.filter { !it.isHidden }
            }

            // Search filter
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase()
                result = result.filter { it.name.lowercase().contains(q) }
            }

            // Sort
            result = when (sortOption) {
                SortOption.NAME_ASC -> result.sortedWith(dirFirstThen { it.name.lowercase() })
                SortOption.NAME_DESC -> result.sortedWith(dirFirstThenDesc { it.name.lowercase() })
                SortOption.SIZE_ASC -> result.sortedWith(dirFirstThen { it.size })
                SortOption.SIZE_DESC -> result.sortedWith(dirFirstThenDesc { it.size })
                SortOption.DATE_ASC -> result.sortedWith(dirFirstThen { it.lastModified })
                SortOption.DATE_DESC -> result.sortedWith(dirFirstThenDesc { it.lastModified })
                SortOption.TYPE_ASC -> result.sortedWith(dirFirstThen { it.extension })
            }

            // Prepend parent folder "^" if not at root and not searching
            if (currentPath != "/" && searchQuery.isBlank()) {
                val parentPath = currentPath.substringBeforeLast('/', "")
                val target = if (parentPath.isEmpty()) "/" else parentPath
                val parentDir = FileModel(
                    name = "^",
                    path = target,
                    isDirectory = true,
                    childCount = 0
                )
                result = listOf(parentDir) + result
            }

            return result
        }

    companion object {
        const val DEFAULT_PATH = "/storage/emulated/0"
    }
}

// Sort helpers — directories always first
private fun <T : Comparable<T>> dirFirstThen(
    selector: (FileModel) -> T
): Comparator<FileModel> =
    compareByDescending<FileModel> { it.isDirectory }.thenBy(selector)

private fun <T : Comparable<T>> dirFirstThenDesc(
    selector: (FileModel) -> T
): Comparator<FileModel> =
    compareByDescending<FileModel> { it.isDirectory }.thenByDescending(selector)

