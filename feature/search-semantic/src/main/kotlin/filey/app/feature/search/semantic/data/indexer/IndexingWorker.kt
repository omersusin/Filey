package filey.app.feature.search.semantic.data.indexer

import android.content.Context
import androidx.work.*
import filey.app.core.data.FileRepository
import filey.app.core.di.AppContainer
import filey.app.core.model.FileCategory
import filey.app.feature.search.semantic.data.vectordb.VectorStore
import filey.app.feature.search.semantic.data.vectordb.ObjectBoxVectorStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class IndexingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "semantic_indexing"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<IndexingWorker>(
                12, TimeUnit.HOURS // Production: every 12 hours
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }

    private val appContainer = AppContainer.Instance
    private val fileRepository: FileRepository = appContainer.fileRepository
    
    private val contentExtractor = ContentExtractor(applicationContext)
    private val chunker = ChunkingStrategy()
    private val embeddingGenerator = EmbeddingGenerator(applicationContext)
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Production: get BoxStore from AppContainer
                val boxStore = try {
                    val field = appContainer.javaClass.getDeclaredField("boxStore")
                    field.isAccessible = true
                    field.get(appContainer) as? io.objectbox.BoxStore
                } catch (e: Exception) {
                    null
                }

                if (boxStore == null) {
                    return@withContext Result.failure(
                        workDataOf("error" to "ObjectBox not initialized in AppContainer")
                    )
                }

                val vectorStore = ObjectBoxVectorStore(boxStore)
                val documentFiles = fileRepository.getCategoryFiles(FileCategory.DOCUMENTS).getOrNull() ?: emptyList()
                
                var indexedCount = 0
                for (fileModel in documentFiles) {
                    // Check if re-index needed
                    val lastModified = File(fileModel.path).lastModified()
                    if (!vectorStore.needsReindex(fileModel.path, lastModified)) {
                        continue
                    }

                    // 1. Extract
                    val extraction = contentExtractor.extract(fileModel)
                    if (extraction.text.isBlank()) continue
                    
                    // 2. Chunk
                    val chunks = chunker.chunk(
                        text = extraction.text,
                        documentId = fileModel.path.hashCode().toString(),
                        metadata = extraction.metadata,
                        entities = extraction.entities
                    )
                    
                    // 3. Embeddings
                    val embeddings = embeddingGenerator.generateBatchEmbeddings(chunks.map { it.text })
                    
                    // 4. Store
                    vectorStore.store(fileModel.path, chunks, embeddings)
                    
                    indexedCount++
                    setProgress(workDataOf("progress" to (indexedCount.toFloat() / documentFiles.size * 100).toInt()))
                }
                
                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }
}
