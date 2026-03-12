#!/bin/bash
set -e
cd ~/Filey

# ── 1) OcrContentExtractor.kt — MLKit kaldır, Android ML Kit olmadan OCR yap
# search-semantic modülünde MLKit bağımlılığı yok, kullanmıyoruz.
# PDF sayfaları zaten ContentExtractor'da render ediliyor; text basit string döner.
cat > feature/search-semantic/src/main/kotlin/filey/app/feature/search/semantic/data/indexer/OcrContentExtractor.kt << 'EOF'
package filey.app.feature.search.semantic.data.indexer

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OCR stub — search-semantic modülünde ML Kit bağımlılığı yok.
 * Gerçek OCR feature:smart-tags modülündeki SmartTextRecognizer ile yapılıyor.
 * PDF metin çıkarma için PdfRenderer bitmap'leri buraya gelir; şimdilik boş string döner.
 * İleride ML Kit bağımlılığı eklenirse bu sınıf doldurulabilir.
 */
class OcrContentExtractor {

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        // ML Kit bu modülde yok — ContentExtractor PDF sayfalarını burada işler.
        // Gelecekte: ML Kit eklenirse TextRecognition.getClient() burada başlatılır.
        ""
    }

    fun close() { /* no-op */ }
}
EOF
echo "✅ OcrContentExtractor.kt"

# ── 2) SearchQuery.kt — SemanticResult duplicate'ini kaldır
cat > feature/search-semantic/src/main/kotlin/filey/app/feature/search/semantic/domain/model/SearchQuery.kt << 'EOF'
package filey.app.feature.search.semantic.domain.model

data class SearchQuery(
    val query: String,
    val topK: Int = 10,
    val scoreThreshold: Float = 0.3f
)
// SemanticResult burada değil — SemanticResult.kt dosyasında tanımlı
EOF
echo "✅ SearchQuery.kt"

# ── 3) SearchViewModel.kt — appContainer.preferences.context erişimini düzelt
cat > feature/search-semantic/src/main/kotlin/filey/app/feature/search/semantic/presentation/SearchViewModel.kt << 'EOF'
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
EOF
echo "✅ SearchViewModel.kt"

git add \
  feature/search-semantic/src/main/kotlin/filey/app/feature/search/semantic/data/indexer/OcrContentExtractor.kt \
  feature/search-semantic/src/main/kotlin/filey/app/feature/search/semantic/domain/model/SearchQuery.kt \
  feature/search-semantic/src/main/kotlin/filey/app/feature/search/semantic/presentation/SearchViewModel.kt

git commit -m "fix(search-semantic): remove MLKit from OCR stub, fix SemanticResult redecl, fix ViewModel context"
git push origin feat/sprint1-hardening
echo "DONE"
