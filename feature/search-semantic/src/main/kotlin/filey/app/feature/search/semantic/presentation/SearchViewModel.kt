package filey.app.feature.search.semantic.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import filey.app.core.di.AppContainer
import filey.app.feature.search.semantic.domain.model.SemanticResult
import filey.app.feature.search.semantic.domain.usecase.QueryPreprocessor
import filey.app.feature.search.semantic.domain.usecase.SemanticSearchUseCase
import filey.app.feature.search.semantic.data.indexer.EmbeddingGenerator
import filey.app.feature.search.semantic.data.vectordb.ObjectBoxVectorStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<SemanticResult> = emptyList(),
    val searchTimeMs: Long = 0,
    val error: String? = null
)

class SearchViewModel(
    private val semanticSearchUseCase: SemanticSearchUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
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
                        isLoading = false,
                        results = result.results,
                        searchTimeMs = result.searchTimeMs
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Arama hatası") }
            }
        }
    }

    companion object {
        fun createFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val embeddingGenerator = EmbeddingGenerator(context)
                    val queryPreprocessor = QueryPreprocessor()

                    // ObjectBox store'u AppContainer'dan reflection ile al
                    val boxStore = try {
                        val container = AppContainer.Instance
                        val field = container.javaClass.getDeclaredField("boxStore")
                        field.isAccessible = true
                        field.get(container) as? io.objectbox.BoxStore
                    } catch (e: Exception) { null }

                    val vectorStore = if (boxStore != null) {
                        ObjectBoxVectorStore(boxStore)
                    } else {
                        throw IllegalStateException("ObjectBox BoxStore not initialized in AppContainer")
                    }

                    val useCase = SemanticSearchUseCase(embeddingGenerator, vectorStore, queryPreprocessor)
                    return SearchViewModel(useCase) as T
                }
            }
    }
}
