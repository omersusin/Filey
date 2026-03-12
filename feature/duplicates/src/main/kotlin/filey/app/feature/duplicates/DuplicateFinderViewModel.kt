package filey.app.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import filey.app.core.data.FileRepository
import filey.app.core.di.AppContainer
import filey.app.core.model.FileModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DuplicateGroup(
    val hash: String,
    val files: List<FileModel>
)

data class DuplicateUiState(
    val groups: List<DuplicateGroup> = emptyList(),
    val isLoading: Boolean = false,
    val progress: String = "",
    val error: String? = null
)

class DuplicateFinderViewModel(
    private val repository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicateUiState())
    val uiState: StateFlow<DuplicateUiState> = _uiState.asStateFlow()

    fun scan() {
        _uiState.update { it.copy(isLoading = true, groups = emptyList(), error = null) }
        viewModelScope.launch {
            try {
                val allFiles = withContext(Dispatchers.IO) {
                    // Recursive walk of main storage
                    val root = File("/storage/emulated/0")
                    root.walkTopDown()
                        .filter { it.isFile && it.length() > 1024 } // Skip small files < 1KB
                        .toList()
                }

                _uiState.update { it.copy(progress = "${allFiles.size} dosya bulundu, analiz ediliyor…") }

                // Group by size first (cheap)
                val sizeGroups = allFiles.groupBy { it.length() }.filter { it.value.size > 1 }
                
                val duplicates = mutableMapOf<String, MutableList<FileModel>>()
                var processed = 0
                
                sizeGroups.values.forEach { group ->
                    group.forEach { file ->
                        processed++
                        _uiState.update { it.copy(progress = "Hash hesaplanıyor: $processed / ${sizeGroups.values.sumOf { it.size }}") }
                        
                        val hash = repository.calculateChecksum(file.absolutePath, "MD5").getOrNull()
                        if (hash != null) {
                            duplicates.getOrPut(hash) { mutableListOf() }.add(file.toFileModel())
                        }
                    }
                }

                val finalGroups = duplicates.filter { it.value.size > 1 }.map { 
                    DuplicateGroup(it.key, it.value)
                }.sortedByDescending { it.files.first().size * it.files.size }

                _uiState.update { it.copy(isLoading = false, groups = finalGroups, progress = "") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            repository.delete(path).onSuccess {
                // Refresh local state
                val updatedGroups = _uiState.value.groups.map { group ->
                    group.copy(files = group.files.filter { it.path != path })
                }.filter { it.files.size > 1 }
                _uiState.update { it.copy(groups = updatedGroups) }
            }
        }
    }

    private fun File.toFileModel() = FileModel(
        name = name,
        path = absolutePath,
        isDirectory = false,
        size = length(),
        lastModified = lastModified()
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DuplicateFinderViewModel(repository = AppContainer.Instance.fileRepository)
            }
        }
    }
}
