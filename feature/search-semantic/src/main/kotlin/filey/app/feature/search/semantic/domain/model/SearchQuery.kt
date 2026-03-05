package filey.app.feature.search.semantic.domain.model

data class SearchQuery(
    val query: String,
    val topK: Int = 10,
    val scoreThreshold: Float = 0.3f
)

data class SemanticResult(
    val documentPath: String,
    val documentName: String,
    val chunkText: String,
    val pageNumber: Int,
    val relevanceScore: Float,
    val metadata: Map<String, String>
)
