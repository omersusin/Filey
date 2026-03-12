package filey.app.feature.search.semantic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import filey.app.feature.search.semantic.domain.usecase.SemanticSearchUseCase
import filey.app.feature.search.semantic.domain.model.SemanticResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val semanticSearchUseCase: SemanticSearchUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
    }
    
    fun performSearch() {
        val query = _uiState.value.query
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = semanticSearchUseCase(query)
                _uiState.update { 
                    it.copy(
                        results = result.results,
                        searchTimeMs = result.searchTimeMs,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Arama sırasında bir hata oluştu",
                        isLoading = false
                    )
                }
            }
        }
    }
}

data class SearchUiState(
    val query: String = "",
    val results: List<SemanticResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchTimeMs: Long = 0
)
