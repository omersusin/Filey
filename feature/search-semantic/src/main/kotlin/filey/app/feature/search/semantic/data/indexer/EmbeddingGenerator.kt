package filey.app.feature.search.semantic.data.indexer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.math.sqrt

class EmbeddingGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private val modelLock = Mutex()
    
    companion object {
        private const val MODEL_FILE = "multilingual_e5_small.tflite"
        private const val MAX_SEQ_LENGTH = 128
        private const val EMBEDDING_DIM = 384
    }
    
    suspend fun initialize() = modelLock.withLock {
        if (interpreter != null) return@withLock
        
        withContext(Dispatchers.IO) {
            try {
                val modelBuffer = context.assets.open(MODEL_FILE).use { it.readBytes() }
                    .let { ByteBuffer.allocateDirect(it.size).apply { order(ByteOrder.nativeOrder()); put(it) } }
                
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = Interpreter(modelBuffer, options)
            } catch (e: Exception) {
                // Model dosyası yoksa veya yüklenemezse hata yönetimi
            }
        }
    }
    
    suspend fun generateEmbedding(text: String): FloatArray {
        initialize()
        if (interpreter == null) return FloatArray(EMBEDDING_DIM)
        
        return withContext(Dispatchers.Default) {
            val output = Array(1) { FloatArray(EMBEDDING_DIM) }
            // Gerçek implementasyonda tokenizer ile input tensörleri hazırlanmalı
            // Şimdilik dummy bir çıktı veriyoruz veya interpreter'ı basitçe çalıştırıyoruz
            normalize(output[0])
        }
    }
    
    suspend fun generateBatchEmbeddings(texts: List<String>): List<FloatArray> {
        return texts.map { generateEmbedding(it) }
    }
    
    private fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0f) {
            for (i in vector.indices) vector[i] /= norm
        }
        return vector
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
