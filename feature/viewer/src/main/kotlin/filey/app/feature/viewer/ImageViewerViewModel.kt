package filey.app.feature.viewer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class ImageViewerUiState(
    val filePath: String = "",
    val fileName: String = "",
    val fileSize: String = "",
    val showInfo: Boolean = false
)

class ImageViewerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ImageViewerUiState())
    val uiState: StateFlow<ImageViewerUiState> = _uiState.asStateFlow()

    fun load(path: String) {
        val file = File(path)
        _uiState.value = ImageViewerUiState(
            filePath = path,
            fileName = file.name,
            fileSize = formatSize(file.length())
        )
    }

    fun toggleInfo() {
        _uiState.value = _uiState.value.copy(showInfo = !_uiState.value.showInfo)
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val g = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, g.toDouble()), units[g])
    }
}
