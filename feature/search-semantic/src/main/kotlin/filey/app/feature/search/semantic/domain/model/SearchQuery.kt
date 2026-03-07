package filey.app.feature.search.semantic.domain.model

data class SearchQuery(
    val query: String,
    val topK: Int = 10,
    val scoreThreshold: Float = 0.3f
)
// SemanticResult burada değil — SemanticResult.kt dosyasında tanımlı
