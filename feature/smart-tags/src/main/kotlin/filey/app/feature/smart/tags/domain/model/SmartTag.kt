package filey.app.feature.smart.tags.domain.model

data class SmartTag(
    val value: String,
    val category: TagCategory,
    val confidence: Float,
    val source: TagSource,
    val metadata: Map<String, String> = emptyMap()
)
