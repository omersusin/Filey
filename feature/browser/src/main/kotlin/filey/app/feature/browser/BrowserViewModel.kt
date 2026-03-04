package filey.app.feature.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import filey.app.core.data.FileRepository
import filey.app.core.data.preferences.AppPreferences
import filey.app.core.model.*
import filey.app.core.di.AppContainer
import filey.app.feature.browser.actions.FileActionCallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowserViewModel(
    private val repository: FileRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val pathHistory = mutableListOf(BrowserUiState.DEFAULT_PATH)
    private var historyIndex = 0
    private var searchJob: Job? = null

    private val _snackbarEvent = MutableStateFlow<String?>(null)
    val snackbarEvent: StateFlow<String?> = _snackbarEvent.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.viewModeFlow.collect { mode ->
                _uiState.update { it.copy(viewMode = mode) }
            }
        }
        viewModelScope.launch {
            preferences.sortOptionFlow.collect { sort ->
                _uiState.update { it.copy(sortOption = sort) }
                refreshDisplayFiles()
            }
        }
        viewModelScope.launch {
            preferences.showHiddenFlow.collect { show ->
                _uiState.update { it.copy(showHiddenFiles = show) }
                refreshDisplayFiles()
            }
        }
        viewModelScope.launch {
            preferences.accessModeFlow.collect { mode ->
                _uiState.update { it.copy(accessMode = mode) }
            }
        }
        viewModelScope.launch {
            preferences.favoritesFlow.collect { favs ->
                _uiState.update { it.copy(favorites = favs) }
            }
        }
        viewModelScope.launch {
            preferences.recentsFlow.collect { recs ->
                _uiState.update { it.copy(recents = recs) }
            }
        }

        val startPath = preferences.getLastPath() ?: BrowserUiState.DEFAULT_PATH
        loadDirectory(startPath)
    }

    private fun refreshDisplayFiles() {
        val state = _uiState.value
        if (state.isDeepSearch && state.searchQuery.isNotBlank()) {
            _uiState.update { it.copy(displayFiles = it.searchResults) }
            return
        }

        var result = state.files
        if (!state.showHiddenFiles) {
            result = result.filter { !it.isHidden }
        }

        if (state.searchQuery.isNotBlank() && !state.isDeepSearch) {
            val q = state.searchQuery.lowercase()
            result = result.filter { it.name.lowercase().contains(q) }
        }

        val sortOption = state.sortOption
        result = when (sortOption) {
            SortOption.NAME_ASC -> result.sortedWith(dirFirstThen { it.name.lowercase() })
            SortOption.NAME_DESC -> result.sortedWith(dirFirstThenDesc { it.name.lowercase() })
            SortOption.SIZE_ASC -> result.sortedWith(dirFirstThen { it.size })
            SortOption.SIZE_DESC -> result.sortedWith(dirFirstThenDesc { it.size })
            SortOption.DATE_ASC -> result.sortedWith(dirFirstThen { it.lastModified })
            SortOption.DATE_DESC -> result.sortedWith(dirFirstThenDesc { it.lastModified })
            SortOption.TYPE_ASC -> result.sortedWith(dirFirstThen { it.extension })
        }

        val isCategory = state.currentPath.let { p -> FileCategory.entries.any { it.label == p } }
        if (state.currentPath != "/" && state.searchQuery.isBlank() && !isCategory) {
            val parentPath = state.currentPath.substringBeforeLast('/', "")
            val target = if (parentPath.isEmpty()) "/" else parentPath
            val parentDir = FileModel(name = "^", path = target, isDirectory = true)
            result = listOf(parentDir) + result
        }

        _uiState.update { it.copy(displayFiles = result) }
    }

    fun toggleFavorite(path: String) = viewModelScope.launch { preferences.toggleFavorite(path) }
    fun addToRecents(path: String) = viewModelScope.launch { preferences.addRecent(path) }

    fun navigateTo(path: String) {
        if (historyIndex < pathHistory.lastIndex) {
            while (pathHistory.size > historyIndex + 1) pathHistory.removeAt(pathHistory.lastIndex)
        }
        pathHistory.add(path)
        historyIndex = pathHistory.lastIndex
        loadDirectory(path)
    }

    fun navigateUp() {
        val current = _uiState.value.currentPath
        if (current == "/" || FileCategory.entries.any { it.label == current }) return
        val parent = current.substringBeforeLast('/', "")
        navigateTo(if (parent.isEmpty()) "/" else parent)
    }

    fun goBack(): Boolean {
        if (historyIndex > 0) {
            historyIndex--
            loadDirectory(pathHistory[historyIndex], addToHistory = false)
            return true
        }
        return false
    }

    fun loadDirectory(path: String, addToHistory: Boolean = false) {
        viewModelScope.launch {
            preferences.setLastPath(path)
            _uiState.update {
                it.copy(
                    isLoading = true, error = null, currentPath = path,
                    pathSegments = pathToSegments(path),
                    canGoBack = historyIndex > 0,
                    canGoForward = historyIndex < pathHistory.lastIndex,
                    selectedFiles = emptySet(), isMultiSelectActive = false,
                    isSearchActive = false, searchQuery = ""
                )
            }
            repository.listFiles(path).fold(
                onSuccess = { files ->
                    val storage = repository.getStorageInfo(path).getOrNull()
                    _uiState.update { it.copy(isLoading = false, files = files, storageInfo = storage) }
                    refreshDisplayFiles()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message, files = emptyList()) }
                    refreshDisplayFiles()
                }
            )
            if (addToHistory) { pathHistory.add(path); historyIndex = pathHistory.lastIndex }
        }
    }

    fun loadCategory(category: FileCategory) = viewModelScope.launch {
        _uiState.update {
            it.copy(
                isLoading = true, error = null, currentPath = category.label,
                pathSegments = listOf(PathSegment(category.label, "")),
                canGoBack = true, selectedFiles = emptySet(), isMultiSelectActive = false,
                isSearchActive = false, searchQuery = ""
            )
        }
        repository.getCategoryFiles(category).fold(
            onSuccess = { files ->
                _uiState.update { it.copy(isLoading = false, files = files) }
                refreshDisplayFiles()
            },
            onFailure = { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message, files = emptyList()) }
                refreshDisplayFiles()
            }
        )
    }

    fun toggleSearch() {
        _uiState.update {
            if (it.isSearchActive) {
                searchJob?.cancel()
                it.copy(isSearchActive = false, searchQuery = "", isDeepSearch = false, searchResults = emptyList())
            } else it.copy(isSearchActive = true)
        }
        refreshDisplayFiles()
    }

    fun toggleDeepSearch() {
        _uiState.update { it.copy(isDeepSearch = !it.isDeepSearch) }
        val q = _uiState.value.searchQuery
        if (q.isNotBlank()) performDeepSearch(q) else refreshDisplayFiles()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (_uiState.value.isDeepSearch) performDeepSearch(query) else refreshDisplayFiles()
    }

    private fun performDeepSearch(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            refreshDisplayFiles()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isLoading = true) }
            repository.searchFiles(_uiState.value.currentPath, query).fold(
                onSuccess = { r -> _uiState.update { it.copy(isLoading = false, searchResults = r) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
            refreshDisplayFiles()
        }
    }

    fun setViewMode(mode: ViewMode) = viewModelScope.launch { preferences.setViewMode(mode) }
    fun setSortOption(option: SortOption) = viewModelScope.launch { preferences.setSortOption(option) }
    fun toggleHiddenFiles() = viewModelScope.launch { preferences.setShowHidden(!_uiState.value.showHiddenFiles) }

    fun createFolder(name: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        repository.createDirectory(_uiState.value.currentPath, name).fold(
            onSuccess = { showSnackbar("Klasör oluşturuldu: $name"); refreshCurrentDirectory() },
            onFailure = { e -> showSnackbar("Klasör oluşturulamadı: ${e.message}"); _uiState.update { it.copy(isLoading = false) } }
        )
    }

    fun moveToTrash(path: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        repository.moveToTrash(path).fold(
            onSuccess = { showSnackbar("Çöp kutusuna taşındı"); refreshCurrentDirectory() },
            onFailure = { e -> showSnackbar("Hata: ${e.message}"); _uiState.update { it.copy(isLoading = false) } }
        )
    }

    fun deletePermanently(path: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        repository.delete(path).fold(
            onSuccess = { showSnackbar("Kalıcı olarak silindi"); refreshCurrentDirectory() },
            onFailure = { e -> showSnackbar("Silinemedi: ${e.message}"); _uiState.update { it.copy(isLoading = false) } }
        )
    }

    fun deleteSelected(permanently: Boolean = false) {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (permanently) repository.delete(selected) 
                         else runCatching { selected.forEach { repository.moveToTrash(it) } }.map { Unit }
            
            result.fold(
                onSuccess = {
                    showSnackbar("${selected.size} öğe ${if(permanently) "silindi" else "çöpe taşındı"}")
                    _uiState.update { it.copy(selectedFiles = emptySet(), isMultiSelectActive = false) }
                    refreshCurrentDirectory()
                },
                onFailure = { e -> showSnackbar("Hata: ${e.message}"); _uiState.update { it.copy(isLoading = false) } }
            )
        }
    }

    fun renameFile(path: String, newName: String) = viewModelScope.launch {
        repository.rename(path, newName).fold(
            onSuccess = { showSnackbar("Yeniden adlandırıldı"); refreshCurrentDirectory() },
            onFailure = { e -> showSnackbar("Hata: ${e.message}") }
        )
    }

    fun setClipboard(paths: List<String>, isCut: Boolean) {
        _uiState.update { it.copy(clipboard = ClipboardData(paths, isCut)) }
    }

    fun paste() {
        val clipboard = _uiState.value.clipboard ?: return
        val destPath = _uiState.value.currentPath
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, operationMessage = "İşleniyor…") }
            clipboard.paths.forEach { source ->
                if (clipboard.isCut) repository.move(source, destPath) { p -> _uiState.update { it.copy(operationMessage = "Taşınıyor… %${(p*100).toInt()}") } }
                else repository.copy(source, destPath) { p -> _uiState.update { it.copy(operationMessage = "Kopyalanıyor… %${(p*100).toInt()}") } }
            }
            if (clipboard.isCut) _uiState.update { it.copy(clipboard = null) }
            _uiState.update { it.copy(isLoading = false, operationMessage = null) }
            refreshCurrentDirectory()
        }
    }

    fun toggleFileSelection(path: String) = _uiState.update { state ->
        val new = state.selectedFiles.toMutableSet()
        if (new.contains(path)) new.remove(path) else new.add(path)
        state.copy(selectedFiles = new, isMultiSelectActive = new.isNotEmpty())
    }

    fun selectAll() = _uiState.update { it.copy(selectedFiles = it.displayFiles.map { f -> f.path }.toSet(), isMultiSelectActive = true) }
    fun clearSelection() = _uiState.update { it.copy(selectedFiles = emptySet(), isMultiSelectActive = false) }

    fun setAccessMode(mode: AccessMode) = viewModelScope.launch {
        preferences.setAccessMode(mode)
        refreshCurrentDirectory()
    }

    fun refreshCurrentDirectory() = loadDirectory(_uiState.value.currentPath)
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun showSnackbar(message: String) { _snackbarEvent.value = message }
    fun clearSnackbar() { _snackbarEvent.value = null }

    fun createActionCallback(context: Context): FileActionCallback = object : FileActionCallback {
        override fun onSuccess(message: String) = showSnackbar(message)
        override fun onError(message: String) = showSnackbar("⚠️ $message")
        override fun showSnackbar(message: String) = this@BrowserViewModel.showSnackbar(message)
        override fun refreshDirectory() = refreshCurrentDirectory()
        override fun navigateTo(path: String) = this@BrowserViewModel.navigateTo(path)
        override fun copyToClipboard(text: String) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Filey", text))
        }
        override fun setClipboard(paths: List<String>, isCut: Boolean) = this@BrowserViewModel.setClipboard(paths, isCut)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BrowserViewModel(repository = AppContainer.Instance.fileRepository, preferences = AppContainer.Instance.preferences)
            }
        }
    }
}
