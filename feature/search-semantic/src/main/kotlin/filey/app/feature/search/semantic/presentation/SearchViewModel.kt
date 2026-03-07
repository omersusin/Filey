package filey.app.feature.search.semantic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import filey.app.feature.search.semantic.domain.usecase.SemanticSearchUseCase
import filey.app.feature.search.semantic.domain.model.SemanticResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val semanticSearchUseCase: SemanticSearchUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState
    
    fun search(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val result = semanticSearchUseCase(query)
                _uiState.value = SearchUiState.Success(result.results)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Arama sırasında bir hata oluştu")
            }
        }
    }
}

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<SemanticResult>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
