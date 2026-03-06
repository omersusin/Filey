package filey.app.feature.smart.tags.data.worker

import android.content.Context
import androidx.work.*
import filey.app.core.di.AppContainer
import filey.app.core.model.FileCategory
import filey.app.feature.smart.tags.data.ml.*
import filey.app.feature.smart.tags.data.repository.SmartTagRepository
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class SmartTaggingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "smart_tagging"
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true).setRequiresBatteryNotLow(true).setRequiresDeviceIdle(true).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SmartTaggingWorker>(12, TimeUnit.HOURS).setConstraints(constraints).build()
            )
        }
        fun runOnce(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_once", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SmartTaggingWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()
            )
        }
    }

    private val fileRepository = AppContainer.Instance.fileRepository
    private val imageLabeler    by lazy { SmartImageLabeler(applicationContext) }
    private val textRecognizer  by lazy { SmartTextRecognizer() }
    private val mlRouter        by lazy { MLPipelineRouter(imageLabeler, textRecognizer, DocumentClassifier(), ExifTagExtractor()) }
    private val tagRepository: SmartTagRepository by lazy {
        filey.app.feature.smart.tags.di.SmartTagContainer.getInstance(applicationContext).repository
    }

    private val supportedCategories = setOf(FileCategory.IMAGES, FileCategory.DOCUMENTS, FileCategory.VIDEOS, FileCategory.AUDIO)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val allFiles = fileRepository.getCategoryFiles(FileCategory.ALL).getOrNull() ?: emptyList()
            val toProcess = allFiles.filter { it.category in supportedCategories &&
                (!tagRepository.isTagged(it.path) || tagRepository.needsRetagging(it.path, it.lastModified)) }
            if (toProcess.isEmpty()) return@withContext Result.success()
            var processed = 0
            toProcess.chunked(5).forEach { batch ->
                coroutineScope {
                    batch.map { file -> async(Dispatchers.Default) {
                        try { val r = mlRouter.process(file); if (r.tags.isNotEmpty()) tagRepository.saveTags(file.path, r.tags); processed++ } catch (_: Exception) {}
                    }}.awaitAll()
                }
                setProgress(workDataOf("processed" to processed, "total" to toProcess.size))
            }
            Result.success(workDataOf("total_tagged" to processed))
        } catch (e: Exception) { Result.failure() }
    }
}
