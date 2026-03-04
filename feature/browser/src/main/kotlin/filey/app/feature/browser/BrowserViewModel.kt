package filey.app.feature.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.FileObserver
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
import java.io.File
import java.util.ArrayDeque

class BrowserViewModel(
    private val repository: FileRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val pathHistory = mutableListOf(BrowserUiState.DEFAULT_PATH)
    private var historyIndex = 0
    private var searchJob: Job? = null
    private var folderObserver: FileObserver? = null

    data class SnackbarData(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null)
    private val _snackbarEvent = MutableStateFlow<SnackbarData?>(null)
    val snackbarEvent: StateFlow<SnackbarData?> = _snackbarEvent.asStateFlow()

    private val undoStack = ArrayDeque<FileOperation>()

    init {
        viewModelScope.launch { preferences.viewModeFlow.collect { mode -> _uiState.update { it.copy(viewMode = mode) } } }
        viewModelScope.launch { preferences.sortOptionFlow.collect { sort -> _uiState.update { it.copy(sortOption = sort) }; refreshDisplayFiles() } }
        viewModelScope.launch { preferences.showHiddenFlow.collect { show -> _uiState.update { it.copy(showHiddenFiles = show) }; refreshDisplayFiles() } }
        viewModelScope.launch { preferences.accessModeFlow.collect { mode -> _uiState.update { it.copy(accessMode = mode) } } }
        viewModelScope.launch { preferences.favoritesFlow.collect { favs -> _uiState.update { it.copy(favorites = favs) } } }
        viewModelScope.launch { preferences.recentsFlow.collect { recs -> _uiState.update { it.copy(recents = recs) } } }
        viewModelScope.launch { preferences.fileTagsFlow.collect { tags -> refreshDisplayFiles() } } // Refresh when tags change

        val startPath = preferences.getLastPath() ?: BrowserUiState.DEFAULT_PATH
        loadDirectory(startPath)
    }

    private fun refreshDisplayFiles() {
        val state = _uiState.value
        val filters = state.searchFilters
        val allTags = preferences.fileTagsFlow.value
        
        var result = if (state.isDeepSearch && state.searchQuery.isNotBlank()) {
            state.searchResults
        } else {
            state.files
        }

        // Attach tags to each file model
        result = result.map { it.copy(tags = allTags[it.path]?.toList() ?: emptyList()) }

        if (!state.showHiddenFiles) {
            result = result.filter { !it.isHidden }
        }

        // Apply filters
        if (filters.type != null) {
            result = result.filter { FileUtils.getFileType(it.path, it.isDirectory) == filters.type }
        }
        if (filters.minSize > 0) {
            result = result.filter { it.size >= filters.minSize }
        }
        if (filters.dateAfter > 0) {
            result = result.filter { it.lastModified >= filters.dateAfter }
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
        if (state.currentPath != "/" && state.searchQuery.isBlank() && !isCategory && !state.isDeepSearch) {
            val parentPath = state.currentPath.substringBeforeLast('/', "")
            val target = if (parentPath.isEmpty()) "/" else parentPath
            val parentDir = FileModel(name = "^", path = target, isDirectory = true)
            result = listOf(parentDir) + result
        }

        _uiState.update { it.copy(displayFiles = result) }
    }

    fun toggleTag(path: String, tag: String) = viewModelScope.launch { preferences.toggleTag(path, tag) }

    fun toggleFolderWatcher() {
        val currentPath = _uiState.value.currentPath
        if (folderObserver != null) {
            folderObserver?.stopWatching(); folderObserver = null; showSnackbar("İzleme durduruldu")
        } else {
            if (currentPath.startsWith("/storage")) {
                folderObserver = object : FileObserver(currentPath, CREATE or DELETE or MODIFY or MOVED_TO) {
                    override fun onEvent(event: Int, path: String?) {
                        viewModelScope.launch { refreshCurrentDirectory(); path?.let { showSnackbar("Klasör değişti: $it") } }
                    }
                }
                folderObserver?.startWatching(); showSnackbar("Klasör izleniyor…")
            } else showSnackbar("Bu konum izlenemiyor")
        }
    }

    fun toggleShelfItem(path: String) = _uiState.update { state ->
        val newShelf = state.shelf.toMutableSet()
        if (newShelf.contains(path)) newShelf.remove(path) else newShelf.add(path)
        state.copy(shelf = newShelf)
    }

    fun clearShelf() = _uiState.update { it.copy(shelf = emptySet()) }

    fun pasteFromShelf() {
        val shelf = _uiState.value.shelf
        if (shelf.isEmpty()) return
        val dest = _uiState.value.currentPath
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, operationMessage = "Raftakiler işleniyor…") }
            shelf.forEach { source ->
                repository.copy(source, dest) { p -> _uiState.update { it.copy(operationMessage = "Kopyalanıyor %${(p*100).toInt()}") } }
            }
            showSnackbar("${shelf.size} öğe raftan kopyalandı"); _uiState.update { it.copy(isLoading = false, operationMessage = null) }
            refreshCurrentDirectory()
        }
    }

    fun toggleFavorite(path: String) = viewModelScope.launch { preferences.toggleFavorite(path) }
    fun addToRecents(path: String) = viewModelScope.launch { preferences.addRecent(path) }

    fun navigateTo(path: String) {
        if (historyIndex < pathHistory.lastIndex) {
            while (pathHistory.size > historyIndex + 1) pathHistory.removeAt(pathHistory.lastIndex)
        }
        pathHistory.add(path); historyIndex = pathHistory.lastIndex; loadDirectory(path)
    }

    fun navigateUp() {
        val current = _uiState.value.currentPath
        if (current == "/" || FileCategory.entries.any { it.label == current }) return
        val parent = current.substringBeforeLast('/', "")
        navigateTo(if (parent.isEmpty()) "/" else parent)
    }

    fun goBack(): Boolean {
        if (historyIndex > 0) {
            historyIndex--; loadDirectory(pathHistory[historyIndex], addToHistory = false); return true
        }
        return false
    }

    fun loadDirectory(path: String, addToHistory: Boolean = false) {
        folderObserver?.stopWatching(); folderObserver = null
        viewModelScope.launch {
            preferences.setLastPath(path)
            _uiState.update {
                it.copy(
                    isLoading = true, error = null, currentPath = path, pathSegments = pathToSegments(path),
                    canGoBack = historyIndex > 0, canGoForward = historyIndex < pathHistory.lastIndex,
                    selectedFiles = emptySet(), isMultiSelectActive = false, isSearchActive = false, searchQuery = ""
                )
            }
            repository.listFiles(path).fold(
                onSuccess = { files ->
                    val storage = repository.getStorageInfo(path).getOrNull()
                    _uiState.update { it.copy(isLoading = false, files = files, storageInfo = storage) }
                    refreshDisplayFiles()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Hata", files = emptyList()) }
                    refreshDisplayFiles()
                }
            )
            if (addToHistory) { pathHistory.add(path); historyIndex = pathHistory.lastIndex }
        }
    }

    fun loadCategory(category: FileCategory) = viewModelScope.launch {
        folderObserver?.stopWatching(); folderObserver = null
        _uiState.update {
            it.copy(
                isLoading = true, error = null, currentPath = category.label, pathSegments = listOf(PathSegment(category.label, "")),
                canGoBack = true, selectedFiles = emptySet(), isMultiSelectActive = false, isSearchActive = false, searchQuery = ""
            )
        }
        repository.getCategoryFiles(category).fold(
            onSuccess = { files -> _uiState.update { it.copy(isLoading = false, files = files) }; refreshDisplayFiles() },
            onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message, files = emptyList()) }; refreshDisplayFiles() }
        )
    }

    fun undoLastOperation() {
        val op = undoStack.pollFirst() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, operationMessage = "Geri alınıyor…") }
            when (op) {
                is FileOperation.Rename -> { repository.rename(op.newPath, File(op.oldPath).name) }
                is FileOperation.Move -> {
                    op.sourcePaths.forEach { path ->
                        val fileName = File(path).name
                        val currentPath = File(op.destinationDir, fileName).absolutePath
                        repository.move(currentPath, File(path).parent ?: "") { }
                    }
                }
                is FileOperation.Trash -> { op.trashPaths.forEach { path -> repository.restoreFromTrash(path) } }
            }
            _uiState.update { it.copy(isLoading = false, operationMessage = null) }; refreshCurrentDirectory(); showSnackbar("İşlem geri alındı")
        }
    }

    fun toggleSearch() {
        _uiState.update {
            if (it.isSearchActive) {
                searchJob?.cancel()
                it.copy(isSearchActive = false, searchQuery = "", isDeepSearch = false, searchResults = emptyList(), searchFilters = SearchFilters())
            } else it.copy(isSearchActive = true)
        }; refreshDisplayFiles()
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

    fun updateSearchFilters(filters: SearchFilters) {
        _uiState.update { it.copy(searchFilters = filters) }
        val q = _uiState.value.searchQuery
        if (_uiState.value.isDeepSearch && q.isNotBlank()) performDeepSearch(q) else refreshDisplayFiles()
    }

    private fun performDeepSearch(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList()) }; refreshDisplayFiles(); return
        }
        searchJob = viewModelScope.launch {
            delay(300); _uiState.update { it.copy(isLoading = true) }
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
            onSuccess = {
                repository.getTrashFiles().onSuccess { trashFiles ->
                    val lastTrash = trashFiles.firstOrNull()
                    if (lastTrash != null) undoStack.push(FileOperation.Trash(listOf(path), listOf(lastTrash.path)))
                }
                showSnackbar("Çöpe taşındı", "Geri Al") { undoLastOperation() }; refreshCurrentDirectory()
            },
            onFailure = { e -> showSnackbar("Hata: ${e.message}"); _uiState.update { it.copy(isLoading = false) } }
        )
    }

    fun deleteSelected(permanently: Boolean = false) {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (permanently) {
                repository.delete(selected).onSuccess { showSnackbar("${selected.size} silindi"); clearSelection(); refreshCurrentDirectory() }
            } else {
                val trashPaths = mutableListOf<String>()
                selected.forEach { path ->
                    repository.moveToTrash(path).onSuccess {
                        repository.getTrashFiles().onSuccess { it.firstOrNull()?.let { f -> trashPaths.add(f.path) } }
                    }
                }
                undoStack.push(FileOperation.Trash(selected, trashPaths))
                showSnackbar("${selected.size} çöpe taşındı", "Geri Al") { undoLastOperation() }; clearSelection(); refreshCurrentDirectory()
            }
        }
    }

    fun renameFile(path: String, newName: String) = viewModelScope.launch {
        repository.rename(path, newName).fold(
            onSuccess = {
                undoStack.push(FileOperation.Rename(path, File(File(path).parent, newName).absolutePath))
                showSnackbar("Yeniden adlandırıldı", "Geri Al") { undoLastOperation() }; refreshCurrentDirectory()
            },
            onFailure = { e -> showSnackbar("Hata: ${e.message}") }
        )
    }

    fun batchRename(base: String, prefix: String, suffix: String, startNumber: Int) {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, operationMessage = "İşleniyor…") }
            var counter = startNumber
            selected.forEach { path ->
                val f = File(path)
                val newName = buildString {
                    append(prefix)
                    if (base.isNotBlank()) { append(base); append("_"); append(counter.toString().padStart(3, '0')) }
                    else append(f.nameWithoutExtension)
                    append(suffix)
                    if (f.extension.isNotBlank()) { append("."); append(f.extension) }
                }
                repository.rename(path, newName).onSuccess { counter++ }
            }
            showSnackbar("Toplu adlandırma tamamlandı"); clearSelection(); refreshCurrentDirectory()
        }
    }

    fun setClipboard(paths: List<String>, isCut: Boolean) = _uiState.update { it.copy(clipboard = ClipboardData(paths, isCut)) }

    fun paste() {
        val clipboard = _uiState.value.clipboard ?: return
        val dest = _uiState.value.currentPath
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, operationMessage = "İşleniyor…") }
            if (clipboard.isCut) undoStack.push(FileOperation.Move(clipboard.paths, dest))
            clipboard.paths.forEach { source ->
                if (clipboard.isCut) repository.move(source, dest) { p -> _uiState.update { it.copy(operationMessage = "Taşınıyor %${(p*100).toInt()}") } }
                else repository.copy(source, dest) { p -> _uiState.update { it.copy(operationMessage = "Kopyalanıyor %${(p*100).toInt()}") } }
            }
            val msg = if (clipboard.isCut) "Taşındı" else "Kopyalandı"
            showSnackbar("$msg: ${clipboard.paths.size} öğe", if(clipboard.isCut) "Geri Al" else null) { if (clipboard.isCut) undoLastOperation() }
            if (clipboard.isCut) _uiState.update { it.copy(clipboard = null) }
            _uiState.update { it.copy(isLoading = false, operationMessage = null) }; refreshCurrentDirectory()
        }
    }

    fun toggleFileSelection(path: String) = _uiState.update { state ->
        val new = state.selectedFiles.toMutableSet()
        if (new.contains(path)) new.remove(path) else new.add(path)
        state.copy(selectedFiles = new, isMultiSelectActive = new.isNotEmpty())
    }

    fun selectAll() = _uiState.update { it.copy(selectedFiles = it.displayFiles.map { f -> f.path }.toSet(), isMultiSelectActive = true) }
    fun clearSelection() = _uiState.update { it.copy(selectedFiles = emptySet(), isMultiSelectActive = false) }

    fun setAccessMode(mode: AccessMode) = viewModelScope.launch { preferences.setAccessMode(mode); refreshCurrentDirectory() }
    fun refreshCurrentDirectory() = loadDirectory(_uiState.value.currentPath)
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun showSnackbar(message: String, action: String? = null, onAction: (() -> Unit)? = null) {
        _snackbarEvent.value = SnackbarData(message, action, onAction)
    }
    fun clearSnackbar() { _snackbarEvent.value = null }

    override fun onCleared() { super.onCleared(); folderObserver?.stopWatching() }

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
