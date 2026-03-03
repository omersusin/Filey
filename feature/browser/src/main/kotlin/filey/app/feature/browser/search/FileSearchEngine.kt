package filey.app.feature.browser.search

import filey.app.core.model.FileModel
import filey.app.core.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SearchFilter(
    val query: String = "",
    val extensions: Set<String> = emptySet(),
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val modifiedAfter: Long? = null,
    val modifiedBefore: Long? = null,
)

class FileSearchEngine {

    fun search(rootPath: String, filter: SearchFilter): Flow<FileModel> = flow {
        val rootDir = File(rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return@flow

        val queue = ArrayDeque<File>()
        queue.add(rootDir)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val children = current.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory) queue.add(child)
                if (matchesFilter(child, filter)) emit(child.toFileModel())
            }
            yield()
        }
    }.flowOn(Dispatchers.IO)

    private fun matchesFilter(file: File, f: SearchFilter): Boolean {
        if (f.query.isNotBlank() && !file.name.contains(f.query, ignoreCase = true)) return false
        if (f.extensions.isNotEmpty() && file.extension.lowercase() !in f.extensions) return false
        f.minSize?.let { if (file.length() < it) return false }
        f.maxSize?.let { if (file.length() > it) return false }
        f.modifiedAfter?.let { if (file.lastModified() < it) return false }
        f.modifiedBefore?.let { if (file.lastModified() > it) return false }
        return true
    }

    private fun File.toFileModel(): FileModel {
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return FileModel(
            name          = name,
            path          = absolutePath,
            size          = length(),
            lastModified  = lastModified(),
            isDirectory   = isDirectory,
            isHidden      = isHidden,
            extension     = extension.lowercase(),
            type          = FileType.fromFileName(name, isDirectory),
            sizeFormatted = formatSize(length()),
            dateFormatted = fmt.format(Date(lastModified())),
        )
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1_024L               -> "$bytes B"
        bytes < 1_048_576L           -> "${"%.1f".format(bytes / 1_024.0)} KB"
        bytes < 1_073_741_824L       -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
        else                         -> "${"%.1f".format(bytes / 1_073_741_824.0)} GB"
    }
}
