package filey.app.feature.search.semantic.domain.model

data class DocumentChunk(
    val id: String,
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
    val pageNumber: Int?,
    val chunkIndex: Int,
    val metadata: Map<String, String>
)
