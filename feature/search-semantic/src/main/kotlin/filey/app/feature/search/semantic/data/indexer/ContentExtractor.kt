package filey.app.feature.search.semantic.data.indexer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import filey.app.core.model.FileModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ContentExtractor(
    private val context: Context,
    private val ocrExtractor: OcrContentExtractor = OcrContentExtractor(),
    private val metadataExtractor: MetadataExtractor = MetadataExtractor()
) {

    data class ExtractionResult(
        val text: String,
        val metadata: DocumentMetadata,
        val entities: List<MetadataExtractor.NamedEntity> = emptyList()
    )

    data class DocumentMetadata(
        val title: String? = null,
        val pageCount: Int? = null,
        val detectedEntities: List<String> = emptyList()
    )

    suspend fun extract(file: FileModel): ExtractionResult {
        return when {
            file.extension.equals("pdf", ignoreCase = true) -> extractPdf(file)
            file.extension.equals("txt", ignoreCase = true) -> extractText(file)
            file.extension.equals("md", ignoreCase = true) -> extractText(file)
            else -> ExtractionResult("", DocumentMetadata())
        }
    }

    private suspend fun extractText(file: FileModel): ExtractionResult {
        return withContext(Dispatchers.IO) {
            try {
                val text = File(file.path).readText()
                val entities = metadataExtractor.extractEntities(text)
                ExtractionResult(
                    text = text,
                    metadata = DocumentMetadata(title = file.name),
                    entities = entities
                )
            } catch (e: Exception) {
                ExtractionResult("", DocumentMetadata())
            }
        }
    }

    private suspend fun extractPdf(file: FileModel): ExtractionResult {
        return withContext(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(File(file.path), ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val fullText = StringBuilder()
                val pageCount = renderer.pageCount

                for (i in 0 until pageCount) {
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2, page.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        
                        val text = ocrExtractor.recognizeText(bitmap)
                        fullText.appendLine(text)
                        bitmap.recycle()
                    }
                }

                renderer.close()
                pfd.close()

                val text = fullText.toString()
                val entities = metadataExtractor.extractEntities(text)

                ExtractionResult(
                    text = text,
                    metadata = DocumentMetadata(
                        title = file.name,
                        pageCount = pageCount
                    ),
                    entities = entities
                )
            } catch (e: Exception) {
                ExtractionResult("", DocumentMetadata())
            }
        }
    }
}
