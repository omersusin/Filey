package filey.app.feature.search.semantic.data.indexer

import filey.app.feature.search.semantic.domain.model.DocumentChunk
import filey.app.feature.search.semantic.domain.model.DocumentMetadata
import javax.inject.Inject

class SemanticChunker @Inject constructor() {
    
    private val maxChunkSize: Int = 512
    private val overlapSize: Int = 128
    private val minChunkSize: Int = 50
    
    fun chunk(
        text: String, 
        documentId: String,
        metadata: DocumentMetadata
    ): List<DocumentChunk> {
        
        if (text.length < minChunkSize) {
            return listOf(
                DocumentChunk(
                    id = "${documentId}_0",
                    text = text,
                    startIndex = 0,
                    endIndex = text.length,
                    pageNumber = 1,
                    chunkIndex = 0,
                    metadata = mapOf("type" to "full")
                )
            )
        }
        
        val paragraphs = text.split(Regex("""\n\s*\n"""))
        val chunks = mutableListOf<DocumentChunk>()
        var currentChunk = StringBuilder()
        var chunkIndex = 0
        var globalOffset = 0
        
        for (paragraph in paragraphs) {
            if (currentChunk.length + paragraph.length > maxChunkSize && currentChunk.isNotEmpty()) {
                val chunkText = currentChunk.toString().trim()
                chunks.add(createChunk(chunkText, documentId, chunkIndex, globalOffset, metadata))
                chunkIndex++
                
                val overlap = chunkText.takeLast(overlapSize)
                currentChunk = StringBuilder(overlap)
            }
            currentChunk.appendLine(paragraph)
            globalOffset += paragraph.length
        }
        
        if (currentChunk.isNotEmpty()) {
            chunks.add(createChunk(currentChunk.toString().trim(), documentId, chunkIndex, globalOffset, metadata))
        }
        
        return chunks
    }
    
    private fun createChunk(
        text: String, 
        docId: String, 
        index: Int, 
        offset: Int, 
        metadata: DocumentMetadata
    ) = DocumentChunk(
        id = "${docId}_$index",
        text = text,
        startIndex = offset - text.length,
        endIndex = offset,
        pageNumber = 1, // Basitlik için 1
        chunkIndex = index,
        metadata = emptyMap()
    )
}
