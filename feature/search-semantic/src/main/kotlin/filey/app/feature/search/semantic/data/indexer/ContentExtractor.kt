package filey.app.feature.search.semantic.data.indexer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import filey.app.feature.search.semantic.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ContentExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    data class ExtractionResult(
        val text: String,
        val metadata: DocumentMetadata,
        val extractionMethod: ExtractionMethod,
        val confidence: Float
    )
    
    enum class ExtractionMethod {
        NONE, PDF_RENDER_OCR, TEXT, OFFICE
    }

    suspend fun extract(filePath: String): ExtractionResult {
        val file = File(filePath)
        return when {
            file.extension.equals("pdf", ignoreCase = true) -> extractPdf(file)
            file.extension.equals("txt", ignoreCase = true) || 
            file.extension.equals("md", ignoreCase = true) -> extractText(file)
            else -> ExtractionResult(
                text = "",
                metadata = DocumentMetadata.EMPTY,
                extractionMethod = ExtractionMethod.NONE,
                confidence = 0f
            )
        }
    }
    
    private suspend fun extractPdf(file: File): ExtractionResult {
        return withContext(Dispatchers.IO) {
            try {
                val fd = context.contentResolver.openFileDescriptor(Uri.fromFile(file), "r") ?: return@withContext emptyResult()
                val renderer = PdfRenderer(fd)
                val fullText = StringBuilder()
                val pageCount = renderer.pageCount
                
                // Basit bir implementasyon: Henüz OCR yok, sadece metin çıkarılabiliyorsa (gelecekte ML Kit eklenecek)
                // Şimdilik sadece sayfa sayısını ve adını alıyoruz
                
                renderer.close()
                fd.close()
                
                ExtractionResult(
                    text = "PDF Content of ${file.name}", // Placeholder
                    metadata = DocumentMetadata(
                        title = file.name,
                        pageCount = pageCount,
                        detectedEntities = emptyList(),
                        author = null, creationDate = null, modifiedDate = file.lastModified(), language = null
                    ),
                    extractionMethod = ExtractionMethod.PDF_RENDER_OCR,
                    confidence = 0.5f
                )
            } catch (e: Exception) {
                emptyResult()
            }
        }
    }
    
    private suspend fun extractText(file: File): ExtractionResult {
        return withContext(Dispatchers.IO) {
            val text = file.readText()
            ExtractionResult(
                text = text,
                metadata = DocumentMetadata(
                    title = file.name,
                    pageCount = 1,
                    detectedEntities = extractEntities(text),
                    author = null, creationDate = null, modifiedDate = file.lastModified(), language = null
                ),
                extractionMethod = ExtractionMethod.TEXT,
                confidence = 1.0f
            )
        }
    }
    
    private fun extractEntities(text: String): List<NamedEntity> {
        val entities = mutableListOf<NamedEntity>()
        // Basit Regex bazlı entity extraction
        val datePattern = """\d{2}[./]\d{2}[./]\d{4}""".toRegex()
        datePattern.findAll(text).forEach { match ->
            entities.add(NamedEntity(EntityType.DATE, match.value, match.range))
        }
        return entities
    }

    private fun emptyResult() = ExtractionResult("", DocumentMetadata.EMPTY, ExtractionMethod.NONE, 0f)
}
