package filey.app.feature.organizer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import filey.app.feature.organizer.engine.OrganizerEngine
import java.io.File

class OrganizerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val engine = OrganizerEngine()
        val rules = engine.getDefaultRules() // Daha sonra DataStore'dan çekilebilir
        val downloadPath = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        ).absolutePath

        return try {
            val movedCount = engine.organizeFolder(downloadPath, rules) { _ -> }
            
            if (movedCount > 0) {
                // Bildirim gönder (Opsiyonel: NotificationManager ile)
                println("Otomatik Düzenleme: $movedCount dosya düzenlendi.")
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
