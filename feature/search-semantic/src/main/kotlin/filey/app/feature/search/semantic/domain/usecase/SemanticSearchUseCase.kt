package filey.app.feature.search.semantic.domain.usecase

import filey.app.feature.search.semantic.data.indexer.EmbeddingGenerator
import filey.app.feature.search.semantic.data.vectordb.VectorStore
import filey.app.feature.search.semantic.domain.model.SearchIntent
import filey.app.feature.search.semantic.domain.model.SearchQuery
import filey.app.feature.search.semantic.domain.model.SemanticResult
import javax.inject.Inject

class SemanticSearchUseCase @Inject constructor(
    private val embeddingGenerator: EmbeddingGenerator,
    private val vectorStore: VectorStore,
    private val queryPreprocessor: QueryPreprocessor
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
        val searchQuery = queryPreprocessor.analyze(query)
        
        // Sorgu embedding'ini üret
        val queryEmbedding = embeddingGenerator.generateEmbedding(searchQuery.normalizedQuery)
        
        // Vektör araması
        var results = vectorStore.search(
            queryEmbedding = queryEmbedding, 
            topK = topK * 2, // Re-rank için daha fazla sonuç çekiyoruz
            scoreThreshold = 0.3f
        )
        
        // Re-ranking: Keyword boost
        results = rerank(results, query).take(topK)
        
        return SmartSearchResult(
            results = results,
            query = searchQuery,
            searchTimeMs = System.currentTimeMillis() - startTime
        )
    }
    
    private fun rerank(
        results: List<SemanticResult>, 
        query: String
    ): List<SemanticResult> {
        val queryTerms = query.lowercase()
            .split(" ")
            .filter { it.length > 2 }
        
        return results.map { result ->
            val keywordScore = queryTerms.count { term ->
                result.chunkText.contains(term, ignoreCase = true)
            }.toFloat() / queryTerms.size.coerceAtLeast(1)
            
            // %70 semantic, %30 keyword
            val hybridScore = (result.relevanceScore * 0.7f) + (keywordScore * 0.3f)
            
            result.copy(relevanceScore = hybridScore)
        }.sortedByDescending { it.relevanceScore }
    }
}
