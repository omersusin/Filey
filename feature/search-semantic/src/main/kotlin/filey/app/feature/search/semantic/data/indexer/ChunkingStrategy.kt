package filey.app.feature.search.semantic.data.indexer

import filey.app.feature.search.semantic.domain.model.DocumentChunk

class ChunkingStrategy(
    private val maxChunkSize: Int = 512,
    private val overlapSize: Int = 128,
    private val minChunkSize: Int = 50
) {

    fun chunk(
        text: String,
        documentId: String,
        metadata: ContentExtractor.DocumentMetadata,
        entities: List<MetadataExtractor.NamedEntity> = emptyList()
    ): List<DocumentChunk> {
        if (text.length < minChunkSize) {
            return listOf(
                DocumentChunk(
                    id = "${documentId}_0",
                    text = text,
                    startIndex = 0,
                    endIndex = text.length,
                    pageNumber = null,
                    chunkIndex = 0,
                    metadata = buildChunkMetadata(text, metadata, entities, 0, text.length)
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
                val chunkStart = globalOffset - currentChunk.length
                val chunkEnd = globalOffset
                
                if (chunkText.length >= minChunkSize) {
                    chunks.add(
                        DocumentChunk(
                            id = "${documentId}_$chunkIndex",
                            text = chunkText,
                            startIndex = chunkStart,
                            endIndex = chunkEnd,
                            pageNumber = estimatePage(globalOffset, text.length, metadata.pageCount),
                            chunkIndex = chunkIndex,
                            metadata = buildChunkMetadata(chunkText, metadata, entities, chunkStart, chunkEnd)
                        )
                    )
                    chunkIndex++
                }

                val overlap = chunkText.takeLast(overlapSize)
                currentChunk = StringBuilder(overlap)
            }

            currentChunk.appendLine(paragraph)
            globalOffset += paragraph.length + 2
        }

        val remaining = currentChunk.toString().trim()
        if (remaining.length >= minChunkSize) {
            val chunkStart = globalOffset - remaining.length
            val chunkEnd = globalOffset
            chunks.add(
                DocumentChunk(
                    id = "${documentId}_$chunkIndex",
                    text = remaining,
                    startIndex = chunkStart,
                    endIndex = chunkEnd,
                    pageNumber = estimatePage(globalOffset, text.length, metadata.pageCount),
                    chunkIndex = chunkIndex,
                    metadata = buildChunkMetadata(remaining, metadata, entities, chunkStart, chunkEnd)
                )
            )
        }

        return chunks
    }

    private fun estimatePage(offset: Int, totalLength: Int, totalPages: Int?): Int? {
        if (totalPages == null || totalLength == 0) return null
        return (offset.toFloat() / totalLength * totalPages).toInt().coerceIn(0, totalPages - 1) + 1
    }

    private fun buildChunkMetadata(
        text: String,
        docMeta: ContentExtractor.DocumentMetadata,
        entities: List<MetadataExtractor.NamedEntity>,
        chunkStart: Int,
        chunkEnd: Int
    ): Map<String, String> {
        val meta = mutableMapOf<String, String>()
        docMeta.title?.let { meta["title"] = it }
        
        // Find entities that fall within this chunk
        val chunkEntities = entities.filter { entity ->
            entity.range.first >= chunkStart && entity.range.last <= chunkEnd
        }
        
        // Group by type and join values
        chunkEntities.groupBy { it.type }.forEach { (type, typeEntities) ->
            meta[type.name.lowercase()] = typeEntities.joinToString(",") { it.value }
        }
        
        return meta
    }
}
