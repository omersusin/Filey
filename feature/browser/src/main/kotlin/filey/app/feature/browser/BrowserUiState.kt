package filey.app.feature.browser

import filey.app.core.model.*

data class SearchFilters(
    val type: FileType? = null,
    val minSize: Long = 0L,
    val dateAfter: Long = 0L
)

data class BrowserUiState(
    val currentPath: String = DEFAULT_PATH,
    val files: List<FileModel> = emptyList(),
    val displayFiles: List<FileModel> = emptyList(), // Pre-calculated for performance
    val isLoading: Boolean = false,
    val error: String? = null,

    // Search
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isDeepSearch: Boolean = false,
    val searchResults: List<FileModel> = emptyList(),
    val searchFilters: SearchFilters = SearchFilters(),

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

    // Favorites
    val favorites: Set<String> = emptySet(),

    // Recents
    val recents: List<String> = emptyList(),

    // Storage info
    val storageInfo: StorageInfo? = null,

    // Operation progress
    val operationMessage: String? = null
) {
    companion object {
        const val DEFAULT_PATH = "/storage/emulated/0"
    }
}

// Sort helpers — directories always first
fun <T : Comparable<T>> dirFirstThen(
    selector: (FileModel) -> T
): Comparator<FileModel> =
    compareByDescending<FileModel> { it.isDirectory }.thenBy(selector)

fun <T : Comparable<T>> dirFirstThenDesc(
    selector: (FileModel) -> T
): Comparator<FileModel> =
    compareByDescending<FileModel> { it.isDirectory }.thenByDescending(selector)
