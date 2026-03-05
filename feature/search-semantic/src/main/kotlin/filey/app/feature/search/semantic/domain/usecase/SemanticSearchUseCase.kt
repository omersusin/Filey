package filey.app.feature.search.semantic.domain.usecase

import filey.app.feature.search.semantic.data.indexer.EmbeddingGenerator
import filey.app.feature.search.semantic.data.vectordb.VectorStore
import filey.app.feature.search.semantic.domain.model.SemanticResult

class SemanticSearchUseCase(
    private val embeddingGenerator: EmbeddingGenerator,
    private val vectorStore: VectorStore,
    private val queryPreprocessor: QueryPreprocessor
) {

    data class SmartSearchResult(
        val results: List<SemanticResult>,
        val interpretation: QueryPreprocessor.QueryInterpretation,
        val searchTimeMs: Long
    )

    suspend operator fun invoke(
        query: String,
        topK: Int = 10
    ): SmartSearchResult {
        val startTime = System.currentTimeMillis()

        // 1) Analyze query
        val interpretation = queryPreprocessor.analyze(query)

        // 2) Generate embedding
        val queryEmbedding = embeddingGenerator.generateEmbedding(interpretation.normalizedQuery)

        // 3) Search vector store
        var results = vectorStore.search(
            queryEmbedding = queryEmbedding,
            topK = topK * 2,
            scoreThreshold = 0.3f
        )

        // 4) Rerank and take topK
        results = rerank(results, query).take(topK)

        return SmartSearchResult(
            results = results,
            interpretation = interpretation,
            searchTimeMs = System.currentTimeMillis() - startTime
        )
    }

    private fun rerank(
        results: List<SemanticResult>,
        query: String
    ): List<SemanticResult> {
        val queryTerms = query.lowercase().split(" ").filter { it.length > 2 }
        
        return results.map { result ->
            val keywordScore = if (queryTerms.isEmpty()) 0f else {
                queryTerms.count { term ->
                    result.chunkText.contains(term, ignoreCase = true)
                }.toFloat() / queryTerms.size
            }
            
            // 70% semantic, 30% keyword overlap
            val hybridScore = (result.relevanceScore * 0.7f) + (keywordScore * 0.3f)
            result.copy(relevanceScore = hybridScore)
        }.sortedByDescending { it.relevanceScore }
    }
}
