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
    val saveSuccess: Boolean = false
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

    fun save() {
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
                EditorViewModel(repository = AppContainer.fileRepository)
            }
        }
    }
}
