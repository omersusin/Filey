package filey.app.feature.search.semantic.domain.model

data class SearchQuery(
    val originalQuery: String,
    val normalizedQuery: String,
    val intent: SearchIntent,
    val filters: Map<String, String>
)

enum class SearchIntent {
    FIND_DOCUMENT,
    FIND_BY_DATE,
    FIND_BY_CONTENT,
    FIND_BY_TYPE,
    GENERAL
}
