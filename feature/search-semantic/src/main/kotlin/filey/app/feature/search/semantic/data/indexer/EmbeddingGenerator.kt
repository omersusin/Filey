package filey.app.feature.search.semantic.data.indexer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class EmbeddingGenerator(
    private val context: Context
) {

    private var interpreter: Interpreter? = null
    private var tokenizer: BertTokenizer? = null
    private val modelLock = Mutex()

    companion object {
        private const val MODEL_FILE = "multilingual_e5_small.tflite"
        private const val VOCAB_FILE = "vocab.txt"
        private const val MAX_SEQ_LENGTH = 128
        private const val EMBEDDING_DIM = 384
    }

    private suspend fun initialize() = modelLock.withLock {
        if (interpreter != null) return@withLock

        withContext(Dispatchers.IO) {
            try {
                // Load TFLite model from assets
                val modelBuffer = context.assets.open(MODEL_FILE).use {
                    val bytes = it.readBytes()
                    ByteBuffer.allocateDirect(bytes.size).apply {
                        order(ByteOrder.nativeOrder())
                        put(bytes)
                    }
                }
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = Interpreter(modelBuffer, options)

                // Load Vocabulary for tokenizer
                val vocab = context.assets.open(VOCAB_FILE).bufferedReader().readLines()
                tokenizer = BertTokenizer(vocab, MAX_SEQ_LENGTH)
            } catch (e: Exception) {
                // In production, log this and notify the user or try an alternative
                e.printStackTrace()
            }
        }
    }

    suspend fun generateEmbedding(text: String): FloatArray {
        initialize()

        return withContext(Dispatchers.Default) {
            val currentInterpreter = interpreter ?: return@withContext FloatArray(EMBEDDING_DIM)
            val currentTokenizer = tokenizer ?: return@withContext FloatArray(EMBEDDING_DIM)

            val tokenIds = currentTokenizer.tokenize(text)
            
            // TFLite input/output preparation
            // Input: [1, MAX_SEQ_LENGTH] int32
            // For E5 model, usually input_ids, attention_mask, token_type_ids
            val inputIds = Array(1) { IntArray(MAX_SEQ_LENGTH) }
            val attentionMask = Array(1) { IntArray(MAX_SEQ_LENGTH) }
            val tokenTypeIds = Array(1) { IntArray(MAX_SEQ_LENGTH) }

            tokenIds.forEachIndexed { i, id ->
                inputIds[0][i] = id
                attentionMask[0][i] = if (id != 0) 1 else 0 // Assuming 0 is [PAD]
            }

            // Output: [1, EMBEDDING_DIM] float32
            val output = Array(1) { FloatArray(EMBEDDING_DIM) }

            // Depending on the exact E5 TFLite conversion, you might pass multiple inputs
            // Here we assume a standard conversion that takes these 3 inputs
            val inputs = arrayOf(inputIds, attentionMask, tokenTypeIds)
            val outputs = mutableMapOf<Int, Any>(0 to output)
            
            try {
                currentInterpreter.runForMultipleInputsOutputs(inputs, outputs)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext FloatArray(EMBEDDING_DIM)
            }

            normalize(output[0])
        }
    }

    suspend fun generateBatchEmbeddings(texts: List<String>): List<FloatArray> {
        return texts.map { generateEmbedding(it) }
    }

    private fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 1e-9f) {
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
