package filey.app.feature.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import filey.app.core.data.ArchiveHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ArchiveUiState(
    val fileName: String = "",
    val filePath: String = "",
    val entries: List<ArchiveHandler.ArchiveEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isExtracting: Boolean = false,
    val extractProgress: String = "",
    val error: String? = null
)

class ArchiveViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    fun loadArchive(path: String) {
        viewModelScope.launch {
            _uiState.value = ArchiveUiState(
                fileName = File(path).name,
                filePath = path,
                isLoading = true
            )
            try {
                val entries = ArchiveHandler.listContents(path)
                _uiState.value = _uiState.value.copy(entries = entries, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun extractToSameDir() {
        val path = _uiState.value.filePath
        val destDir = File(path).parent ?: return
        val folderName = File(path).nameWithoutExtension
        val dest = File(destDir, folderName).absolutePath

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExtracting = true, extractProgress = "Çıkarılıyor...")
            try {
                ArchiveHandler.extract(path, dest)
                _uiState.value = _uiState.value.copy(
                    isExtracting = false,
                    extractProgress = "Tamamlandı: $dest"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExtracting = false,
                    error = e.message
                )
            }
        }
    }
}
