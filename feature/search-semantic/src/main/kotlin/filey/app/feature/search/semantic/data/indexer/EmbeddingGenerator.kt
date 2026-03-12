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
    // all-MiniLM-L6-v2 — 384 boyutlu vektör, ~80MB, çok hızlı
    // Alternatif: multilingual-e5-small (çok dilli destek, Türkçe dahil)
    
    private var interpreter: Interpreter? = null
    private var tokenizer: BertTokenizer? = null
    
    private val modelLock = Mutex()
    
    companion object {
        private const val MODEL_FILE = "multilingual_e5_small.tflite"
        private const val VOCAB_FILE = "vocab.txt"
        private const val MAX_SEQ_LENGTH = 128
        private const val EMBEDDING_DIM = 384
    }
    
    suspend fun initialize() = modelLock.withLock {
        if (interpreter != null) return@withLock
        
        withContext(Dispatchers.IO) {
            try {
                // TFLite model yükle
                val modelBuffer = context.assets
                    .open(MODEL_FILE)
                    .use { it.readBytes() }
                    .let { 
                        ByteBuffer.allocateDirect(it.size).apply { 
                            order(ByteOrder.nativeOrder())
                            put(it)
                            rewind()
                        } 
                    }
                
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                    // GPU delegate (varsa)
                    try {
                        addDelegate(GpuDelegate())
                    } catch (e: Exception) {
                        // GPU yoksa NNAPI dene
                        try {
                            addDelegate(NnApiDelegate())
                        } catch (_: Exception) { }
                    }
                }
                
                interpreter = Interpreter(modelBuffer, options)
                
                // Tokenizer
                val vocab = context.assets.open(VOCAB_FILE)
                    .bufferedReader().readLines()
                tokenizer = BertTokenizer(vocab, MAX_SEQ_LENGTH)
            } catch (e: Exception) {
                // Handle missing assets or load errors
            }
        }
    }
    
    suspend fun generateEmbedding(text: String): FloatArray {
        initialize()
        val currentInterpreter = interpreter ?: return FloatArray(EMBEDDING_DIM)
        val currentTokenizer = tokenizer ?: return FloatArray(EMBEDDING_DIM)
        
        return withContext(Dispatchers.Default) {
            val tokens = currentTokenizer.tokenize(text)
            
            // Input tensörleri hazırla
            val inputIds = Array(1) { IntArray(MAX_SEQ_LENGTH) }
            val attentionMask = Array(1) { IntArray(MAX_SEQ_LENGTH) }
            val tokenTypeIds = Array(1) { IntArray(MAX_SEQ_LENGTH) }
            
            tokens.forEachIndexed { i, token ->
                inputIds[0][i] = token
                attentionMask[0][i] = 1
            }
            
            // Output
            val output = Array(1) { FloatArray(EMBEDDING_DIM) }
            
            val inputs = mapOf(
                "input_ids" to inputIds,
                "attention_mask" to attentionMask,
                "token_type_ids" to tokenTypeIds
            )
            val outputs = mapOf(0 to output)
            
            currentInterpreter.runForMultipleInputsOutputs(
                inputs.values.toTypedArray(), outputs
            )
            
            // L2 normalize
            normalize(output[0])
        }
    }
    
    // Batch embedding — indexleme için
    suspend fun generateBatchEmbeddings(
        texts: List<String>
    ): List<FloatArray> {
        return texts.map { generateEmbedding(it) }
    }
    
    private fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        return vector
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
