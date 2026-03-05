package filey.app.feature.search.semantic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import filey.app.core.di.AppContainer
import filey.app.feature.search.semantic.domain.usecase.SemanticSearchUseCase
import filey.app.feature.search.semantic.domain.usecase.QueryPreprocessor
import filey.app.feature.search.semantic.data.indexer.EmbeddingGenerator
import filey.app.feature.search.semantic.data.vectordb.ObjectBoxVectorStore
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
        fun createFactory(appContainer: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // For this prototype/ready-version, we'll initialize ObjectBox store manually if needed
                // in the app container.
                
                // Use reflection to try and get the boxStore which might be commented out for build safety
                // In a final production app, this would be a clean Hilt/Dagger injection.
                
                // Attempting manual construction for the user
                val context = appContainer.preferences.context
                
                // This assumes the user has uncommented boxStore in AppContainer
                // and the build has generated MyObjectBox
                val boxStore = try {
                    val field = appContainer.javaClass.getDeclaredField("boxStore")
                    field.isAccessible = true
                    field.get(appContainer) as? io.objectbox.BoxStore
                } catch (e: Exception) {
                    null
                }
                
                if (boxStore == null) {
                    throw IllegalStateException("ObjectBox must be initialized in AppContainer. Please follow the instructions in build.gradle.")
                }

                val vectorStore = ObjectBoxVectorStore(boxStore)
                val embeddingGenerator = EmbeddingGenerator(context)
                val queryPreprocessor = QueryPreprocessor()
                val useCase = SemanticSearchUseCase(embeddingGenerator, vectorStore, queryPreprocessor)
                
                return SearchViewModel(useCase) as T
            }
        }
    }
}

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<filey.app.feature.search.semantic.domain.model.SemanticResult> = emptyList(),
    val error: String? = null,
    val searchTimeMs: Long = 0
)
