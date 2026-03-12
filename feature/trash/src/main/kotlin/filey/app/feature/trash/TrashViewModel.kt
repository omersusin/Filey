package filey.app.feature.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import filey.app.core.data.FileRepository
import filey.app.core.di.AppContainer
import filey.app.core.model.FileModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrashUiState(
    val files: List<FileModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TrashViewModel(
    private val repository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        loadTrash()
    }

    fun loadTrash() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.getTrashFiles().fold(
                onSuccess = { files ->
                    _uiState.update { it.copy(isLoading = false, files = files, error = null) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun restore(path: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(path).onSuccess {
                loadTrash()
            }
        }
    }

    fun deletePermanently(path: String) {
        viewModelScope.launch {
            repository.delete(path).onSuccess {
                loadTrash()
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash().onSuccess {
                loadTrash()
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TrashViewModel(repository = AppContainer.Instance.fileRepository)
            }
        }
    }
}
