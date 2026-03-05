package filey.app.feature.smart.tags.data.ml

import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import filey.app.core.model.FileModel
import filey.app.feature.smart.tags.domain.model.SmartTag
import filey.app.feature.smart.tags.domain.model.TagCategory
import filey.app.feature.smart.tags.domain.model.TagSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class SmartTextRecognizer {
    
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    }
    
    suspend fun recognizeFromImage(file: FileModel): List<SmartTag> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.path) 
                    ?: return@withContext emptyList()
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                
                val result = Tasks.await(recognizer.process(inputImage), 10, TimeUnit.SECONDS)
                bitmap.recycle()
                
                val fullText = result.text
                if (fullText.isBlank()) return@withContext emptyList()
                
                val tags = mutableListOf<SmartTag>()
                
                tags.add(
                    SmartTag(
                        value = "has_text",
                        category = TagCategory.CONTENT_TYPE,
                        confidence = 0.95f,
                        source = TagSource.ML_OCR
                    )
                )
                
                // Date extraction
                extractDatesFromText(fullText).forEach { date ->
                    tags.add(
                        SmartTag(
                            value = date,
                            category = TagCategory.DATE,
                            confidence = 0.85f,
                            source = TagSource.ML_OCR
                        )
                    )
                }
                
                // Money extraction
                extractMoneyFromText(fullText).forEach { amount ->
                    tags.add(
                        SmartTag(
                            value = amount,
                            category = TagCategory.FINANCIAL,
                            confidence = 0.8f,
                            source = TagSource.ML_OCR
                        )
                    )
                }
                
                // Document type inference
                val docType = inferDocumentType(fullText)
                if (docType != null) {
                    tags.add(
                        SmartTag(
                            value = docType,
                            category = TagCategory.DOCUMENT_TYPE,
                            confidence = 0.75f,
                            source = TagSource.ML_OCR
                        )
                    )
                }
                
                tags
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun recognizeFromDocument(file: FileModel): List<SmartTag> {
        // Implementation for PDF would go here, possibly using PdfRenderer to get bitmaps
        // For now, let's treat it as a placeholder or simple text file read if it's txt
        return if (file.extension == "txt") {
             withContext(Dispatchers.IO) {
                 val text = File(file.path).readText()
                 val tags = mutableListOf<SmartTag>()
                 val docType = inferDocumentType(text)
                 if (docType != null) {
                     tags.add(SmartTag(docType, TagCategory.DOCUMENT_TYPE, 0.7f, TagSource.ML_OCR))
                 }
                 tags
             }
        } else {
            emptyList()
        }
    }
    
    private fun inferDocumentType(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.containsAny("fatura", "invoice", "vergi", "kdv") -> "invoice"
            lower.containsAny("sözleşme", "contract", "taraflar") -> "contract"
            lower.containsAny("cv", "resume", "deneyim", "experience") -> "resume"
            lower.containsAny("reçete", "prescription", "ilaç") -> "prescription"
            lower.containsAny("diploma", "sertifika", "certificate") -> "certificate"
            lower.containsAny("rapor", "report", "sonuç") -> "report"
            else -> null
        }
    }

    private fun extractDatesFromText(text: String): List<String> {
        val dateRegex = Regex("""\d{2}[./-]\d{2}[./-]\d{4}""")
        return dateRegex.findAll(text).map { it.value }.toList()
    }

    private fun extractMoneyFromText(text: String): List<String> {
        val moneyRegex = Regex("""[\$€₺]\s*[\d,.]+|[\d,.]+\s*(TL|USD|EUR|₺)""")
        return moneyRegex.findAll(text).map { it.value }.toList()
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
