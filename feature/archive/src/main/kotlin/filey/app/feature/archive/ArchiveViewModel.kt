package filey.app.feature.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

data class ArchiveEntry(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val isDirectory: Boolean
)

data class ArchiveUiState(
    val path: String = "",
    val fileName: String = "",
    val entries: List<ArchiveEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isExtracting: Boolean = false,
    val extractProgress: Float = 0f,
    val error: String? = null,
    val extractSuccess: Boolean = false
)

class ArchiveViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    fun loadArchive(path: String) {
        val fileName = path.substringAfterLast('/')
        _uiState.update { it.copy(path = path, fileName = fileName, isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val entries = withContext(Dispatchers.IO) {
                    val zipFile = ZipFile(path)
                    zipFile.entries().toList().map { entry ->
                        ArchiveEntry(
                            name = entry.name,
                            size = entry.size,
                            compressedSize = entry.compressedSize,
                            isDirectory = entry.isDirectory
                        )
                    }.also { zipFile.close() }
                }
                _uiState.update { it.copy(isLoading = false, entries = entries) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Arşiv okunamadı: ${e.message}")
                }
            }
        }
    }

    fun extractTo(destinationDir: String) {
        val archivePath = _uiState.value.path
        _uiState.update { it.copy(isExtracting = true, extractProgress = 0f, error = null) }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val zipFile = ZipFile(archivePath)
                    val entries = zipFile.entries().toList()
                    val total = entries.size
                    var processed = 0

                    val destDir = File(destinationDir)
                    destDir.mkdirs()

                    for (entry in entries) {
                        val outputFile = File(destDir, entry.name)

                        // Zip-slip protection
                        val canonicalDest = destDir.canonicalPath
                        val canonicalOutput = outputFile.canonicalPath
                        if (!canonicalOutput.startsWith(canonicalDest)) {
                            throw SecurityException("Zip-slip saldırısı tespit edildi: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()
                            zipFile.getInputStream(entry).use { input ->
                                outputFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }

                        processed++
                        _uiState.update {
                            it.copy(extractProgress = processed.toFloat() / total)
                        }
                    }
                    zipFile.close()
                }

                _uiState.update {
                    it.copy(isExtracting = false, extractProgress = 1f, extractSuccess = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExtracting = false, error = "Çıkarma hatası: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ArchiveViewModel() }
        }
    }
}
