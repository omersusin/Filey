package filey.app.feature.search.semantic.data.vectordb

import filey.app.feature.search.semantic.domain.model.DocumentChunk
import filey.app.feature.search.semantic.domain.model.SemanticResult
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ObjectBoxVectorStore @Inject constructor(
    private val boxStore: BoxStore
) : VectorStore {
    
    private val box: Box<VectorEntry> by lazy { boxStore.boxFor(VectorEntry::class.java) }
    
    override suspend fun store(
        documentPath: String,
        chunks: List<DocumentChunk>,
        embeddings: List<FloatArray>
    ) = withContext(Dispatchers.IO) {
        // Eski entry'leri sil
        val existing = box.query(VectorEntry_.documentPath.equal(documentPath)).build().find()
        box.remove(existing)
        
        val entries = chunks.zip(embeddings).map { (chunk, embedding) ->
            VectorEntry(
                documentPath = documentPath,
                documentName = documentPath.substringAfterLast("/"),
                chunkId = chunk.id,
                chunkText = chunk.text,
                chunkIndex = chunk.chunkIndex,
                pageNumber = chunk.pageNumber ?: 1,
                embedding = embedding,
                fileLastModified = File(documentPath).lastModified()
            )
        }
        box.put(entries)
    }
    
    override suspend fun search(
        queryEmbedding: FloatArray,
        topK: Int,
        scoreThreshold: Float
    ): List<SemanticResult> = withContext(Dispatchers.IO) {
        val query = box.query(VectorEntry_.embedding.nearestNeighbors(queryEmbedding, topK)).build()
        val results = query.findWithScores()
        query.close()
        
        results
            .filter { it.score >= scoreThreshold }
            .map { scored ->
                val entry = scored.get()
                SemanticResult(
                    documentPath = entry.documentPath,
                    documentName = entry.documentName,
                    chunkText = entry.chunkText,
                    pageNumber = entry.pageNumber,
                    relevanceScore = scored.score.toFloat(),
                    metadata = emptyMap()
                )
            }
    }
    
    override suspend fun isIndexed(path: String): Boolean = box.query(VectorEntry_.documentPath.equal(path)).build().count() > 0
    
    override suspend fun needsReindex(path: String, lastModified: Long): Boolean {
        val entry = box.query(VectorEntry_.documentPath.equal(path)).build().findFirst() ?: return true
        return entry.fileLastModified < lastModified
    }
}
