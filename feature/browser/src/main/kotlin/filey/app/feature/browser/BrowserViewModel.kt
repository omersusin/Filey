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

    // ── Navigation history ──
    private val pathHistory = mutableListOf(BrowserUiState.DEFAULT_PATH)
    private var historyIndex = 0

    // ── Snackbar events ──
    private val _snackbarEvent = MutableStateFlow<String?>(null)
    val snackbarEvent: StateFlow<String?> = _snackbarEvent.asStateFlow()

    init {
        // Observe preference changes
        viewModelScope.launch {
            preferences.viewModeFlow.collect { mode ->
                _uiState.update { it.copy(viewMode = mode) }
            }
        }
        viewModelScope.launch {
            preferences.sortOptionFlow.collect { sort ->
                _uiState.update { it.copy(sortOption = sort) }
            }
        }
        viewModelScope.launch {
            preferences.showHiddenFlow.collect { show ->
                _uiState.update { it.copy(showHiddenFiles = show) }
            }
        }
        viewModelScope.launch {
            preferences.accessModeFlow.collect { mode ->
                _uiState.update { it.copy(accessMode = mode) }
            }
        }

        // Initial load
        loadDirectory(BrowserUiState.DEFAULT_PATH)
    }

    // ══════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════

    fun navigateTo(path: String) {
        // Trim forward history if we're not at the end
        if (historyIndex < pathHistory.lastIndex) {
            while (pathHistory.size > historyIndex + 1) {
                pathHistory.removeAt(pathHistory.lastIndex)
            }
        }
        pathHistory.add(path)
        historyIndex = pathHistory.lastIndex
        loadDirectory(path)
    }

    fun navigateUp() {
        val parent = _uiState.value.currentPath.substringBeforeLast('/', "")
        if (parent.isNotEmpty() && parent != _uiState.value.currentPath) {
            navigateTo(parent)
        }
    }

    fun goBack(): Boolean {
        if (historyIndex > 0) {
            historyIndex--
            loadDirectory(pathHistory[historyIndex], addToHistory = false)
            return true
        }
        return false
    }

    fun goForward() {
        if (historyIndex < pathHistory.lastIndex) {
            historyIndex++
            loadDirectory(pathHistory[historyIndex], addToHistory = false)
        }
    }

    fun loadDirectory(path: String, addToHistory: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentPath = path,
                    pathSegments = pathToSegments(path),
                    canGoBack = historyIndex > 0,
                    canGoForward = historyIndex < pathHistory.lastIndex,
                    // Clear selection and search when changing directory
                    selectedFiles = emptySet(),
                    isMultiSelectActive = false,
                    isSearchActive = false,
                    searchQuery = ""
                )
            }

            val result = repository.listFiles(path)
            result.fold(
                onSuccess = { files ->
                    _uiState.update {
                        it.copy(isLoading = false, files = files, error = null)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Bilinmeyen hata",
                            files = emptyList()
                        )
                    }
                }
            )

            if (addToHistory) {
                pathHistory.add(path)
                historyIndex = pathHistory.lastIndex
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // SEARCH
    // ══════════════════════════════════════════════════════════

    fun toggleSearch() {
        _uiState.update {
            if (it.isSearchActive) {
                it.copy(isSearchActive = false, searchQuery = "")
            } else {
                it.copy(isSearchActive = true)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // ══════════════════════════════════════════════════════════
    // DISPLAY OPTIONS
    // ══════════════════════════════════════════════════════════

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch { preferences.setViewMode(mode) }
    }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch { preferences.setSortOption(option) }
    }

    fun toggleHiddenFiles() {
        viewModelScope.launch {
            preferences.setShowHidden(!_uiState.value.showHiddenFiles)
        }
    }

    // ══════════════════════════════════════════════════════════
    // FILE OPERATIONS
    // ══════════════════════════════════════════════════════════

    fun createFolder(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.createDirectory(_uiState.value.currentPath, name)
            result.fold(
                onSuccess = {
                    showSnackbar("Klasör oluşturuldu: $name")
                    refreshCurrentDirectory()
                },
                onFailure = { e ->
                    showSnackbar("Klasör oluşturulamadı: ${e.message}")
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.delete(path)
            result.fold(
                onSuccess = {
                    showSnackbar("Silindi")
                    refreshCurrentDirectory()
                },
                onFailure = { e ->
                    showSnackbar("Silinemedi: ${e.message}")
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }

    fun deleteSelected() {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.delete(selected)
            result.fold(
                onSuccess = {
                    showSnackbar("${selected.size} öğe silindi")
                    _uiState.update { it.copy(selectedFiles = emptySet(), isMultiSelectActive = false) }
                    refreshCurrentDirectory()
                },
                onFailure = { e ->
                    showSnackbar("Silme hatası: ${e.message}")
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }

    fun renameFile(path: String, newName: String) {
        viewModelScope.launch {
            val result = repository.rename(path, newName)
            result.fold(
                onSuccess = {
                    showSnackbar("Yeniden adlandırıldı: $newName")
                    refreshCurrentDirectory()
                },
                onFailure = { e ->
                    showSnackbar("Yeniden adlandırılamadı: ${e.message}")
                }
            )
        }
    }

    // ── Clipboard ──

    fun setClipboard(paths: List<String>, isCut: Boolean) {
        _uiState.update { it.copy(clipboard = ClipboardData(paths, isCut)) }
    }

    fun paste() {
        val clipboard = _uiState.value.clipboard ?: return
        val destPath = _uiState.value.currentPath

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, operationMessage = "İşleniyor…") }

            var hasError = false
            for (sourcePath in clipboard.paths) {
                val result = if (clipboard.isCut) {
                    repository.move(sourcePath, destPath) { progress ->
                        _uiState.update { it.copy(operationMessage = "Taşınıyor… %${(progress * 100).toInt()}") }
                    }
                } else {
                    repository.copy(sourcePath, destPath) { progress ->
                        _uiState.update { it.copy(operationMessage = "Kopyalanıyor… %${(progress * 100).toInt()}") }
                    }
                }
                if (result.isFailure) {
                    showSnackbar("Hata: ${result.exceptionOrNull()?.message}")
                    hasError = true
                }
            }

            if (!hasError) {
                val action = if (clipboard.isCut) "Taşındı" else "Kopyalandı"
                showSnackbar("$action: ${clipboard.paths.size} öğe")
                if (clipboard.isCut) {
                    _uiState.update { it.copy(clipboard = null) }
                }
            }

            _uiState.update { it.copy(isLoading = false, operationMessage = null) }
            refreshCurrentDirectory()
        }
    }

    // ── Selection ──

    fun toggleFileSelection(path: String) {
        _uiState.update { state ->
            val newSelection = state.selectedFiles.toMutableSet()
            if (newSelection.contains(path)) {
                newSelection.remove(path)
            } else {
                newSelection.add(path)
            }
            state.copy(
                selectedFiles = newSelection,
                isMultiSelectActive = newSelection.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(
                selectedFiles = state.displayFiles.map { it.path }.toSet(),
                isMultiSelectActive = true
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedFiles = emptySet(), isMultiSelectActive = false) }
    }

    // ── Access mode ──

    fun setAccessMode(mode: AccessMode) {
        viewModelScope.launch {
            preferences.setAccessMode(mode)
            // Reload current directory with the new mode
            refreshCurrentDirectory()
        }
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════

    fun refreshCurrentDirectory() {
        loadDirectory(_uiState.value.currentPath)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun showSnackbar(message: String) {
        _snackbarEvent.value = message
    }

    fun clearSnackbar() {
        _snackbarEvent.value = null
    }

    fun copyTextToSystemClipboard(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Filey", text))
    }

    /** Creates a [FileActionCallback] bound to this ViewModel. */
    fun createActionCallback(context: Context): FileActionCallback = object : FileActionCallback {
        override fun onSuccess(message: String) { showSnackbar(message) }
        override fun onError(message: String) { showSnackbar("⚠️ $message") }
        override fun showSnackbar(message: String) { this@BrowserViewModel.showSnackbar(message) }
        override fun refreshDirectory() { refreshCurrentDirectory() }
        override fun navigateTo(path: String) { this@BrowserViewModel.navigateTo(path) }
        override fun copyToClipboard(text: String) { copyTextToSystemClipboard(context, text) }
        override fun setClipboard(paths: List<String>, isCut: Boolean) {
            this@BrowserViewModel.setClipboard(paths, isCut)
        }
    }

    // ══════════════════════════════════════════════════════════
    // FACTORY
    // ══════════════════════════════════════════════════════════

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BrowserViewModel(
                    repository = AppContainer.fileRepository,
                    preferences = AppContainer.preferences
                )
            }
        }
    }
}
