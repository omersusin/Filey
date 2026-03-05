package filey.app.feature.search.semantic.data.vectordb

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id

@Entity
data class VectorEntry(
    @Id var id: Long = 0,
    var documentPath: String = "",
    var documentName: String = "",
    var chunkId: String = "",
    var chunkText: String = "",
    var chunkIndex: Int = 0,
    var pageNumber: Int = 0,
    @HnswIndex(dimensions = 384)
    var embedding: FloatArray = FloatArray(384),
    var metadataJson: String = "",
    var indexedAt: Long = System.currentTimeMillis(),
    var fileLastModified: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VectorEntry

        if (id != other.id) return false
        if (documentPath != other.documentPath) return false
        if (documentName != other.documentName) return false
        if (chunkId != other.chunkId) return false
        if (chunkText != other.chunkText) return false
        if (chunkIndex != other.chunkIndex) return false
        if (pageNumber != other.pageNumber) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (metadataJson != other.metadataJson) return false
        if (indexedAt != other.indexedAt) return false
        if (fileLastModified != other.fileLastModified) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentPath.hashCode()
        result = 31 * result + documentName.hashCode()
        result = 31 * result + chunkId.hashCode()
        result = 31 * result + chunkText.hashCode()
        result = 31 * result + chunkIndex
        result = 31 * result + pageNumber
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + metadataJson.hashCode()
        result = 31 * result + indexedAt.hashCode()
        result = 31 * result + fileLastModified.hashCode()
        return result
    }
}
