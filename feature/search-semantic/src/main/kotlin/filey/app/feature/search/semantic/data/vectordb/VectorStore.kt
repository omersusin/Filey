package filey.app.feature.search.semantic.data.vectordb

import filey.app.feature.search.semantic.domain.model.DocumentChunk
import filey.app.feature.search.semantic.domain.model.SemanticResult

interface VectorStore {
    suspend fun store(documentPath: String, chunks: List<DocumentChunk>, embeddings: List<FloatArray>)
    suspend fun search(queryEmbedding: FloatArray, topK: Int, scoreThreshold: Float): List<SemanticResult>
    suspend fun isIndexed(path: String): Boolean
    suspend fun needsReindex(path: String, lastModified: Long): Boolean
}
