package filey.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import filey.app.core.data.FileRepository
import filey.app.core.di.AppContainer
import filey.app.core.model.FileCategory
import filey.app.core.model.StorageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val storageInfo: StorageInfo? = null,
    val categoryStats: Map<FileCategory, Int> = emptyMap(),
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val repository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val storage = repository.getStorageInfo("/storage/emulated/0").getOrNull()
            
            val stats = mutableMapOf<FileCategory, Int>()
            FileCategory.entries.forEach { cat ->
                val files = repository.getCategoryFiles(cat).getOrDefault(emptyList())
                stats[cat] = files.size
            }

            _uiState.update {
                it.copy(
                    storageInfo = storage,
                    categoryStats = stats,
                    isLoading = false
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(repository = AppContainer.Instance.fileRepository)
            }
        }
    }
}
