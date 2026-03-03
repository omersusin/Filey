package filey.app.feature.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import filey.app.core.data.FileRepository
import filey.app.core.model.FileModel
import filey.app.core.model.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClipboardData(
    val file: FileModel,
    val isCut: Boolean
)

data class BrowserUiState(
    val currentPath: String = "/storage/emulated/0",
    val files: List<FileModel> = emptyList(),
    val isLoading: Boolean = false,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val showHidden: Boolean = false,
    val isGridView: Boolean = false,
    val clipboard: ClipboardData? = null,
    val operationProgress: Float? = null,
    val error: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false
)

class BrowserViewModel : ViewModel() {

    private val repository = FileRepository()
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    // Orijinal listeyi burada tutuyoruz ki arama yaparken veri kaybolmasın
    private var fullFileList: List<FileModel> = emptyList()

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, currentPath = path, error = null)
            try {
                val files = repository.listFiles(path, _uiState.value.showHidden)
                fullFileList = sortFiles(files, _uiState.value.sortOption)
                updateFilteredList()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Dosyalar yüklenemedi",
                    isLoading = false
                )
            }
        }
    }

    private fun updateFilteredList() {
        val query = _uiState.value.searchQuery
        val filtered = if (query.isEmpty()) {
            fullFileList
        } else {
            fullFileList.filter { it.name.contains(query, ignoreCase = true) }
        }
        _uiState.value = _uiState.value.copy(files = filtered, isLoading = false)
    }

    fun setSortOption(option: SortOption) {
        fullFileList = sortFiles(fullFileList, option)
        _uiState.value = _uiState.value.copy(sortOption = option)
        updateFilteredList()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        updateFilteredList()
    }

    fun toggleShowHidden() {
        _uiState.value = _uiState.value.copy(showHidden = !_uiState.value.showHidden)
        loadDirectory(_uiState.value.currentPath)
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(isGridView = !_uiState.value.isGridView)
    }

    fun toggleSearch() {
        val newSearching = !_uiState.value.isSearching
        _uiState.value = _uiState.value.copy(isSearching = newSearching, searchQuery = "")
        if (!newSearching) updateFilteredList()
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                repository.createFolder(_uiState.value.currentPath, name)
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            try {
                repository.createFile(_uiState.value.currentPath, name)
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteFile(file: FileModel) {
        viewModelScope.launch {
            try {
                repository.deleteFile(file.path)
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun renameFile(file: FileModel, newName: String) {
        viewModelScope.launch {
            try {
                repository.renameFile(file.path, newName)
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun copyFile(file: FileModel) {
        _uiState.value = _uiState.value.copy(clipboard = ClipboardData(file, isCut = false))
    }

    fun cutFile(file: FileModel) {
        _uiState.value = _uiState.value.copy(clipboard = ClipboardData(file, isCut = true))
    }

    fun paste() {
        val clip = _uiState.value.clipboard ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operationProgress = 0f)
            try {
                if (clip.isCut) {
                    repository.moveFile(clip.file.path, _uiState.value.currentPath) { progress ->
                        _uiState.value = _uiState.value.copy(operationProgress = progress)
                    }
                } else {
                    repository.copyFile(clip.file.path, _uiState.value.currentPath) { progress ->
                        _uiState.value = _uiState.value.copy(operationProgress = progress)
                    }
                }
                _uiState.value = _uiState.value.copy(clipboard = null, operationProgress = null)
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, operationProgress = null)
            }
        }
    }

    private fun sortFiles(files: List<FileModel>, option: SortOption): List<FileModel> {
        val dirs = files.filter { it.isDirectory }
        val nonDirs = files.filter { !it.isDirectory }

        val sortedDirs = when (option) {
            SortOption.NAME_ASC -> dirs.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> dirs.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_ASC -> dirs.sortedBy { it.lastModified }
            SortOption.DATE_DESC -> dirs.sortedByDescending { it.lastModified }
            SortOption.SIZE_ASC -> dirs.sortedBy { it.size }
            SortOption.SIZE_DESC -> dirs.sortedByDescending { it.size }
            SortOption.TYPE_ASC, SortOption.TYPE_DESC -> dirs.sortedBy { it.name.lowercase() }
        }

        val sortedFiles = when (option) {
            SortOption.NAME_ASC -> nonDirs.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> nonDirs.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_ASC -> nonDirs.sortedBy { it.lastModified }
            SortOption.DATE_DESC -> nonDirs.sortedByDescending { it.lastModified }
            SortOption.SIZE_ASC -> nonDirs.sortedBy { it.size }
            SortOption.SIZE_DESC -> nonDirs.sortedByDescending { it.size }
            SortOption.TYPE_ASC -> nonDirs.sortedBy { it.extension.lowercase() }
            SortOption.TYPE_DESC -> nonDirs.sortedByDescending { it.extension.lowercase() }
        }

        return sortedDirs + sortedFiles
    }
}
