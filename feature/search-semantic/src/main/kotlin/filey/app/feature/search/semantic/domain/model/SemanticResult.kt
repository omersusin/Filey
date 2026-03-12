package filey.app.feature.search.semantic.domain.model

data class SemanticResult(
    val documentPath: String,
    val documentName: String,
    val chunkText: String,
    val pageNumber: Int,
    val relevanceScore: Float,
    val metadata: Map<String, String>
)
