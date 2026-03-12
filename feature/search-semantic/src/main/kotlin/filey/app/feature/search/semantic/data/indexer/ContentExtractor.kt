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

class ContentExtractor(
    @ApplicationContext private val context: Context,
    private val ocrExtractor: OcrContentExtractor,
    private val metadataExtractor: MetadataExtractor
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
        val file = java.io.File(filePath)
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
    
    private suspend fun extractPdf(file: java.io.File): ExtractionResult {
        return withContext(Dispatchers.IO) {
            try {
                val fd = context.contentResolver.openFileDescriptor(android.net.Uri.fromFile(file), "r") ?: return@withContext emptyResult()
                val renderer = PdfRenderer(fd)
                val fullText = StringBuilder()
                val pageCount = renderer.pageCount
                
                for (i in 0 until pageCount.coerceAtMost(10)) { // Limit to first 10 pages for performance
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2, page.height * 2, 
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(
                            bitmap, null, null, 
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                        
                        val text = ocrExtractor.recognizeText(bitmap)
                        fullText.appendLine(text)
                        bitmap.recycle()
                    }
                }
                renderer.close()
                fd.close()
                
                val text = fullText.toString()
                val entities = extractEntities(text)
                
                ExtractionResult(
                    text = text,
                    metadata = DocumentMetadata(
                        title = file.name,
                        pageCount = pageCount,
                        detectedEntities = entities,
                        author = null, 
                        creationDate = null, 
                        modifiedDate = file.lastModified(), 
                        language = null
                    ),
                    extractionMethod = ExtractionMethod.PDF_RENDER_OCR,
                    confidence = 0.9f
                )
            } catch (e: Exception) {
                emptyResult()
            }
        }
    }
    
    private suspend fun extractText(file: java.io.File): ExtractionResult {
        return withContext(Dispatchers.IO) {
            val text = file.readText()
            ExtractionResult(
                text = text,
                metadata = DocumentMetadata(
                    title = file.name,
                    pageCount = 1,
                    detectedEntities = extractEntities(text),
                    author = null, 
                    creationDate = null, 
                    modifiedDate = file.lastModified(), 
                    language = null
                ),
                extractionMethod = ExtractionMethod.TEXT,
                confidence = 1.0f
            )
        }
    }
    
    private fun extractEntities(text: String): List<NamedEntity> {
        val entities = mutableListOf<NamedEntity>()
        
        // Tarih pattern'leri
        val datePatterns = listOf(
            """\d{2}[./]\d{2}[./]\d{4}""",
            """\d{4}-\d{2}-\d{2}""",
            """(?i)(ocak|şubat|mart|nisan|mayıs|haziran|temmuz|ağustos|eylül|ekim|kasım|aralık)\s+\d{4}"""
        )
        
        datePatterns.forEach { pattern ->
            Regex(pattern).findAll(text).forEach { match ->
                entities.add(
                    NamedEntity(
                        type = EntityType.DATE,
                        value = match.value,
                        position = match.range
                    )
                )
            }
        }
        
        // Fatura / Invoice pattern
        val invoicePattern = """(?i)(fatura|invoice|fiş)\s*(no|numarası|number)?[:\s#]*([A-Z0-9\-]+)"""
        Regex(invoicePattern).findAll(text).forEach { match ->
            entities.add(
                NamedEntity(
                    type = EntityType.INVOICE_NUMBER,
                    value = match.groupValues[3],
                    position = match.range
                )
            )
        }
        
        // Para miktarları
        val moneyPattern = """[\$€₺]\s*[\d,.]+|[\d,.]+\s*(TL|USD|EUR|₺)"""
        Regex(moneyPattern).findAll(text).forEach { match ->
            entities.add(
                NamedEntity(
                    type = EntityType.MONEY,
                    value = match.value,
                    position = match.range
                )
            )
        }
        
        return entities
    }

    private fun emptyResult() = ExtractionResult("", DocumentMetadata.EMPTY, ExtractionMethod.NONE, 0f)
}
