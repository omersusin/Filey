package filey.app.feature.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import filey.app.core.data.FileRepository
import filey.app.core.di.AppContainer
import filey.app.core.model.FileCategory
import filey.app.core.model.FileModel
import filey.app.core.model.StorageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyzerUiState(
    val storageInfo: StorageInfo? = null,
    val categorySizes: Map<FileCategory, Long> = emptyMap(),
    val largestFiles: List<FileModel> = emptyList(),
    val isLoading: Boolean = false
)

class StorageAnalyzerViewModel(
    private val repository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyzerUiState())
    val uiState: StateFlow<AnalyzerUiState> = _uiState.asStateFlow()

    init {
        analyze()
    }

    fun analyze() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val storage = repository.getStorageInfo("/storage/emulated/0").getOrNull()
            
            val sizes = mutableMapOf<FileCategory, Long>()
            FileCategory.entries.forEach { cat ->
                val files = repository.getCategoryFiles(cat).getOrDefault(emptyList())
                sizes[cat] = files.sumOf { it.size }
            }

            // Also find 10 largest files (this is a bit slow if we scan everything, 
            // but we can take them from the categories for now)
            val allCategorized = FileCategory.entries.flatMap { 
                repository.getCategoryFiles(it).getOrDefault(emptyList())
            }
            val largest = allCategorized.sortedByDescending { it.size }.take(10)

            _uiState.update {
                it.copy(
                    storageInfo = storage,
                    categorySizes = sizes,
                    largestFiles = largest,
                    isLoading = false
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StorageAnalyzerViewModel(repository = AppContainer.Instance.fileRepository)
            }
        }
    }
}
