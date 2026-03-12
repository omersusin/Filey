package filey.app.feature.search.semantic.data.indexer

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OCR stub — search-semantic modülünde ML Kit bağımlılığı yok.
 * Gerçek OCR feature:smart-tags modülündeki SmartTextRecognizer ile yapılıyor.
 * PDF metin çıkarma için PdfRenderer bitmap'leri buraya gelir; şimdilik boş string döner.
 * İleride ML Kit bağımlılığı eklenirse bu sınıf doldurulabilir.
 */
class OcrContentExtractor {

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        // ML Kit bu modülde yok — ContentExtractor PDF sayfalarını burada işler.
        // Gelecekte: ML Kit eklenirse TextRecognition.getClient() burada başlatılır.
        ""
    }

    fun close() { /* no-op */ }
}
