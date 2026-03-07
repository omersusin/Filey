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
    var metadataJson: String = "{}",
    var indexedAt: Long = System.currentTimeMillis(),
    var fileLastModified: Long = 0
)
