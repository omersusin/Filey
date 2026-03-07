package filey.app.feature.search.semantic.domain.usecase

import filey.app.feature.search.semantic.data.indexer.EmbeddingGenerator
import filey.app.feature.search.semantic.data.vectordb.VectorStore
import filey.app.feature.search.semantic.domain.model.SearchIntent
import filey.app.feature.search.semantic.domain.model.SearchQuery
import filey.app.feature.search.semantic.domain.model.SemanticResult
import javax.inject.Inject

class SemanticSearchUseCase @Inject constructor(
    private val embeddingGenerator: EmbeddingGenerator,
    private val vectorStore: VectorStore
) {
    
    data class SmartSearchResult(
        val results: List<SemanticResult>,
        val query: SearchQuery,
        val searchTimeMs: Long
    )
    
    suspend operator fun invoke(
        query: String, 
        topK: Int = 10
    ): SmartSearchResult {
        val startTime = System.currentTimeMillis()
        
        // Sorguyu analiz et
        val searchQuery = analyzeQuery(query)
        
        // Sorgu embedding'ini üret
        val queryEmbedding = embeddingGenerator.generateEmbedding(searchQuery.normalizedQuery)
        
        // Vektör araması
        val results = vectorStore.search(
            queryEmbedding = queryEmbedding, 
            topK = topK,
            scoreThreshold = 0.3f
        )
        
        return SmartSearchResult(
            results = results,
            query = searchQuery,
            searchTimeMs = System.currentTimeMillis() - startTime
        )
    }
    
    private fun analyzeQuery(query: String): SearchQuery {
        val normalized = query.lowercase().trim()
        val intent = when {
            normalized.contains("nerede") || normalized.contains("bul") -> SearchIntent.FIND_DOCUMENT
            normalized.contains("tarih") || normalized.contains("ay") -> SearchIntent.FIND_BY_DATE
            else -> SearchIntent.GENERAL
        }
        
        return SearchQuery(
            originalQuery = query,
            normalizedQuery = normalized,
            intent = intent,
            filters = emptyMap()
        )
    }
}
