package filey.app.feature.search.semantic.domain.model

import android.graphics.Rect

data class DocumentChunk(
    val id: String,
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
    val pageNumber: Int?,
    val chunkIndex: Int,
    val metadata: Map<String, String>
)

data class NamedEntity(
    val type: EntityType,
    val value: String,
    val position: IntRange
)

enum class EntityType {
    DATE, INVOICE_NUMBER, MONEY, PERSON, ORGANIZATION, LOCATION
}

data class DocumentMetadata(
    val title: String?,
    val author: String?,
    val creationDate: Long?,
    val modifiedDate: Long?,
    val pageCount: Int?,
    val language: String?,
    val detectedEntities: List<NamedEntity>
) {
    companion object {
        val EMPTY = DocumentMetadata(null, null, null, null, null, null, emptyList())
    }
}
