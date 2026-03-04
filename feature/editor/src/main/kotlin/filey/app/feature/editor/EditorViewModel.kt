package filey.app.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import filey.app.core.data.FileRepository
import filey.app.core.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val path: String = "",
    val fileName: String = "",
    val originalContent: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,

    // Search & Replace
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val searchResults: List<Int> = emptyList(), // indices of matches
    val currentMatchIndex: Int = -1
) {
    val hasChanges: Boolean get() = content != originalContent
}

class EditorViewModel(
    private val repository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun loadFile(path: String) {
        val fileName = path.substringAfterLast('/')
        _uiState.update { it.copy(path = path, fileName = fileName, isLoading = true, error = null) }

        viewModelScope.launch {
            val result = repository.readText(path)
            result.fold(
                onSuccess = { text ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            originalContent = text,
                            content = text
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Okuma hatası: ${e.message}")
                    }
                }
            )
        }
    }

    fun updateContent(newContent: String) {
        _uiState.update { it.copy(content = newContent, saveSuccess = false) }
    }

    // ── Search & Replace ────────────────────────────────────

    fun toggleSearch() {
        _uiState.update { 
            it.copy(
                isSearchOpen = !it.isSearchOpen,
                searchQuery = if (!it.isSearchOpen) "" else it.searchQuery,
                searchResults = if (!it.isSearchOpen) emptyList() else it.searchResults,
                currentMatchIndex = -1
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val results = if (query.isNotBlank()) {
                findAllOccurrences(state.content, query)
            } else {
                emptyList()
            }
            state.copy(
                searchQuery = query,
                searchResults = results,
                currentMatchIndex = if (results.isNotEmpty()) 0 else -1
            )
        }
    }

    fun updateReplaceQuery(query: String) {
        _uiState.update { it.copy(replaceQuery = query) }
    }

    fun nextMatch() {
        _uiState.update { state ->
            if (state.searchResults.isEmpty()) return@update state
            val next = (state.currentMatchIndex + 1) % state.searchResults.size
            state.copy(currentMatchIndex = next)
        }
    }

    fun previousMatch() {
        _uiState.update { state ->
            if (state.searchResults.isEmpty()) return@update state
            val prev = if (state.currentMatchIndex <= 0) 
                state.searchResults.lastIndex else state.currentMatchIndex - 1
            state.copy(currentMatchIndex = prev)
        }
    }

    fun replaceCurrent() {
        val state = _uiState.value
        if (state.currentMatchIndex == -1 || state.searchQuery.isBlank()) return

        val matchStart = state.searchResults[state.currentMatchIndex]
        val newContent = state.content.substring(0, matchStart) + 
                         state.replaceQuery + 
                         state.content.substring(matchStart + state.searchQuery.length)
        
        updateContent(newContent)
        updateSearchQuery(state.searchQuery) // refresh matches
    }

    fun replaceAll() {
        val state = _uiState.value
        if (state.searchQuery.isBlank()) return
        val newContent = state.content.replace(state.searchQuery, state.replaceQuery)
        updateContent(newContent)
        updateSearchQuery(state.searchQuery)
    }

    private fun findAllOccurrences(text: String, query: String): List<Int> {
        val indices = mutableListOf<Int>()
        var index = text.indexOf(query, 0, ignoreCase = true)
        while (index != -1) {
            indices.add(index)
            index = text.indexOf(query, index + query.length, ignoreCase = true)
        }
        return indices
    }

    // ── Saving ──────────────────────────────────────────────
        val state = _uiState.value
        if (!state.hasChanges) return

        _uiState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }

        viewModelScope.launch {
            val result = repository.writeText(state.path, state.content)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            originalContent = state.content,
                            saveSuccess = true
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isSaving = false, error = "Kaydetme hatası: ${e.message}")
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EditorViewModel(repository = AppContainer.Instance.fileRepository)
            }
        }
    }
}
