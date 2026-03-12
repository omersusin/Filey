package filey.app.feature.organizer.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object OrganizerWorkManager {
    private const val WORK_NAME = "OrganizerPeriodicWork"

    fun schedulePeriodicWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<OrganizerWorker>(
            12, TimeUnit.HOURS // Her 12 saatte bir çalış
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
