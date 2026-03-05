package filey.app.feature.organizer.engine

import filey.app.core.model.FileUtils
import filey.app.feature.organizer.model.OrganizerRule
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrganizerEngine {

    suspend fun organizeFolder(
        sourceFolderPath: String,
        rules: List<OrganizerRule>,
        onProgress: (String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val sourceFolder = File(sourceFolderPath)
        if (!sourceFolder.exists() || !sourceFolder.isDirectory) return@withContext 0

        var movedCount = 0
        val files = sourceFolder.listFiles() ?: return@withContext 0

        for (file in files) {
            if (file.isDirectory) continue

            val extension = file.extension.lowercase()
            val matchingRule = rules.find { it.isActive && it.extension.lowercase() == extension }

            matchingRule?.let { rule ->
                val targetFolder = File(rule.targetPath)
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs()
                }

                val destination = File(targetFolder, file.name)
                
                onProgress("Taşınıyor: ${file.name} -> ${rule.targetPath}")
                
                if (file.renameTo(destination)) {
                    movedCount++
                }
            }
        }
        movedCount
    }

    /**
     * Varsayılan kurallar oluşturur (Belgeler, Resimler, Videolar vb.)
     */
    fun getDefaultRules(): List<OrganizerRule> {
        val root = android.os.Environment.getExternalStorageDirectory().absolutePath
        return listOf(
            OrganizerRule(extension = "pdf", targetPath = "$root/Documents/PDFs"),
            OrganizerRule(extension = "doc", targetPath = "$root/Documents/Office"),
            OrganizerRule(extension = "docx", targetPath = "$root/Documents/Office"),
            OrganizerRule(extension = "jpg", targetPath = "$root/Pictures/Photos"),
            OrganizerRule(extension = "png", targetPath = "$root/Pictures/Photos"),
            OrganizerRule(extension = "mp4", targetPath = "$root/Movies/Videos"),
            OrganizerRule(extension = "apk", targetPath = "$root/Download/Apps")
        )
    }
}
