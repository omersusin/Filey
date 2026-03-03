package com.omersusin.filey.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class EditorUiState(
    val fileName: String = "",
    val filePath: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = true,
    val isModified: Boolean = false,
    val lineCount: Int = 0,
    val charCount: Int = 0,
    val error: String? = null
)

class EditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun loadFile(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val file = File(path)
                val text = withContext(Dispatchers.IO) { file.readText() }
                _uiState.value = EditorUiState(
                    fileName = file.name,
                    filePath = path,
                    content = text,
                    isLoading = false,
                    isSaved = true,
                    lineCount = text.lines().size,
                    charCount = text.length
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateContent(text: String) {
        _uiState.value = _uiState.value.copy(
            content = text,
            isModified = true,
            isSaved = false,
            lineCount = text.lines().size,
            charCount = text.length
        )
    }

    fun saveFile() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    File(_uiState.value.filePath).writeText(_uiState.value.content)
                }
                _uiState.value = _uiState.value.copy(isSaved = true, isModified = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
