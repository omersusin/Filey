package filey.app.feature.duplicates.engine

import filey.app.core.model.FileUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DuplicateEngine {

    suspend fun findDuplicates(
        rootPath: String,
        onProgress: (String) -> Unit
    ): Map<String, List<File>> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<File>()
        val root = File(rootPath)
        
        // 1. Dosyaları tara
        onProgress("Dosyalar taranıyor...")
        scanRecursive(root, allFiles)

        // 2. Sadece aynı boyuttaki dosyaları grupla (Hız için ilk aşama)
        val sizeGroups = allFiles.filter { it.isFile && it.length() > 0 }
            .groupBy { it.length() }
            .filter { it.value.size > 1 }

        // 3. Aynı boyuttaki dosyaların hash değerlerini karşılaştır
        val duplicates = mutableMapOf<String, MutableList<File>>()
        var processed = 0
        val totalToProcess = sizeGroups.values.flatten().size

        for (group in sizeGroups.values) {
            for (file in group) {
                processed++
                onProgress("Hesaplanıyor: $processed / $totalToProcess")
                
                val hash = FileUtils.calculateHash(file.absolutePath, "MD5") ?: continue
                if (duplicates.containsKey(hash)) {
                    duplicates[hash]?.add(file)
                } else {
                    duplicates[hash] = mutableListOf(file)
                }
            }
        }

        // Sadece birden fazla dosyası olan grupları döndür
        duplicates.filter { it.value.size > 1 }
    }

    private fun scanRecursive(folder: File, result: MutableList<File>) {
        val files = folder.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) {
                if (!f.name.startsWith(".")) scanRecursive(f, result)
            } else {
                result.add(f)
            }
        }
    }
}
