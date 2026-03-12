package filey.app.feature.smart.tags.data.ml

import android.media.MediaMetadataRetriever
import filey.app.core.model.FileCategory
import filey.app.core.model.FileModel
import filey.app.feature.smart.tags.domain.model.SmartTag
import filey.app.feature.smart.tags.domain.model.TagCategory
import filey.app.feature.smart.tags.domain.model.TagSource

class MLPipelineRouter(
    private val imageLabeler: SmartImageLabeler,
    private val textRecognizer: SmartTextRecognizer,
    private val documentClassifier: DocumentClassifier,
    private val exifExtractor: ExifTagExtractor
) {
    
    data class TaggingResult(
        val filePath: String,
        val tags: List<SmartTag>,
        val confidence: Float,
        val processingTimeMs: Long
    )
    
    suspend fun process(file: FileModel): TaggingResult {
        val startTime = System.currentTimeMillis()
        val tags = mutableListOf<SmartTag>()
        
        when (file.category) {
            FileCategory.IMAGES -> {
                tags += imageLabeler.label(file)
                tags += textRecognizer.recognizeFromImage(file)
                tags += exifExtractor.extract(file)
            }
            
            FileCategory.DOCUMENTS -> {
                tags += textRecognizer.recognizeFromDocument(file)
                tags += documentClassifier.classify(file)
            }
            
            FileCategory.VIDEOS -> {
                tags += imageLabeler.labelVideoThumbnail(file)
            }
            
            FileCategory.AUDIO -> {
                tags += extractAudioMetadata(file)
            }
            
            else -> {
                tags += inferTagsFromFilename(file)
            }
        }
        
        val filteredTags = tags
            .filter { it.confidence >= 0.5f }
            .distinctBy { it.value.lowercase() }
            .sortedByDescending { it.confidence }
            .take(15)
        
        return TaggingResult(
            filePath = file.path,
            tags = filteredTags,
            confidence = if (filteredTags.isEmpty()) 0f else filteredTags.map { it.confidence }.average().toFloat(),
            processingTimeMs = System.currentTimeMillis() - startTime
        )
    }

    private fun extractAudioMetadata(file: FileModel): List<SmartTag> {
        val retriever = MediaMetadataRetriever()
        val tags = mutableListOf<SmartTag>()
        try {
            retriever.setDataSource(file.path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                tags.add(SmartTag(it, TagCategory.AUDIO_METADATA, 1.0f, TagSource.AUDIO_METADATA))
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                tags.add(SmartTag(it, TagCategory.AUDIO_METADATA, 1.0f, TagSource.AUDIO_METADATA))
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            retriever.release()
        }
        return tags
    }

    private fun inferTagsFromFilename(file: FileModel): List<SmartTag> {
        val tags = mutableListOf<SmartTag>()
        val name = file.name.substringBeforeLast(".")
        if (name.length > 3) {
            tags.add(SmartTag(file.extension, TagCategory.CONTENT_TYPE, 1.0f, TagSource.FILENAME_ANALYSIS))
        }
        return tags
    }
}
