package filey.app.feature.smart.tags.data.worker

import android.content.Context
import androidx.work.*
import filey.app.core.data.FileRepository
import filey.app.core.di.AppContainer
import filey.app.core.model.FileCategory
import filey.app.feature.smart.tags.data.ml.*
import filey.app.feature.smart.tags.data.repository.SmartTagRepository
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class SmartTaggingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "smart_tagging"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<SmartTaggingWorker>(
                        12, TimeUnit.HOURS
                    ).setConstraints(constraints).build()
                )
        }
    }

    // In a real app, these would be injected via Hilt. 
    // For this prototype, we'll get them from AppContainer.
    private val appContainer = AppContainer.Instance
    private val fileRepository: FileRepository = appContainer.fileRepository
    
    // We'll need a way to get these components. 
    // For now, we'll initialize them here or assume they are in AppContainer.
    private val imageLabeler = SmartImageLabeler(applicationContext)
    private val textRecognizer = SmartTextRecognizer()
    private val documentClassifier = DocumentClassifier()
    private val exifExtractor = ExifTagExtractor()
    
    private val mlRouter = MLPipelineRouter(
        imageLabeler, textRecognizer, documentClassifier, exifExtractor
    )
    
    private val tagRepository: SmartTagRepository by lazy {
        filey.app.feature.smart.tags.di.SmartTagContainer.getInstance(applicationContext).repository
    }
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val supportedCategories = setOf(
                    FileCategory.IMAGE, FileCategory.DOCUMENTS, 
                    FileCategory.VIDEOS, FileCategory.AUDIO
                )
                
                // This is expensive, in a real app we'd query for untagged files directly
                val allFiles = fileRepository.getCategoryFiles(FileCategory.ALL).getOrNull() ?: emptyList()
                
                val untaggedFiles = allFiles.filter { file ->
                    file.category in supportedCategories && 
                    (!tagRepository.isTagged(file.path) || tagRepository.needsRetagging(file.path, file.lastModified))
                }
                
                if (untaggedFiles.isEmpty()) return@withContext Result.success()
                
                var processed = 0
                untaggedFiles.chunked(5).forEach { batch ->
                    coroutineScope {
                        batch.map { file ->
                            async(Dispatchers.Default) {
                                try {
                                    val result = mlRouter.process(file)
                                    tagRepository.saveTags(file.path, result.tags)
                                    processed++
                                } catch (e: Exception) {
                                    // Log failure
                                }
                            }
                        }.awaitAll()
                    }
                    setProgress(workDataOf("processed" to processed, "total" to untaggedFiles.size))
                }
                
                Result.success(workDataOf("total_tagged" to processed))
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }
}
