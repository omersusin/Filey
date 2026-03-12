#!/bin/bash
set -e
CYAN='\033[0;36m'; GREEN='\033[0;32m'; NC='\033[0m'
log() { echo -e "${CYAN}[Filey]${NC} $1"; }
ok()  { echo -e "${GREEN}[OK]${NC} $1"; }

FEAT="feature/browser/src/main/kotlin/filey/app/feature/browser"
mkdir -p "$FEAT/navigation" "$FEAT/search" "$FEAT/sort"

# ── FileRouter.kt ──
cat > "$FEAT/navigation/FileRouter.kt" << 'KOTLIN'
package filey.app.feature.browser.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import filey.app.core.model.FileModel
import filey.app.core.model.FileType
import filey.app.core.navigation.Routes
import java.io.File

class FileRouter(
    private val navController: NavController,
    private val context: Context
) {
    fun openFile(file: FileModel) {
        when (file.type) {
            FileType.IMAGE         -> navController.navigate(Routes.viewer(file.path))
            FileType.TEXT          -> navController.navigate(Routes.editor(file.path))
            FileType.VIDEO,
            FileType.AUDIO         -> navController.navigate(Routes.player(file.path))
            FileType.ARCHIVE       -> navController.navigate(Routes.archive(file.path))
            FileType.DIRECTORY,
            FileType.PDF,
            FileType.APK,
            FileType.OTHER -> openWithSystemChooser(file.path)
        }
    }

    private fun openWithSystemChooser(filePath: String) {
        val uri = Uri.fromFile(File(filePath))
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Şununla aç…"))
    }
}
KOTLIN
ok "FileRouter.kt"

# ── FileSearchEngine.kt ──
cat > "$FEAT/search/FileSearchEngine.kt" << 'KOTLIN'
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
KOTLIN
ok "FileSearchEngine.kt"

# ── SortOption.kt ──
cat > "$FEAT/sort/SortOption.kt" << 'KOTLIN'
package filey.app.feature.browser.sort

import filey.app.core.model.FileModel
import filey.app.core.model.SortOption as CoreSortOption

fun List<FileModel>.sortedWith(option: CoreSortOption): List<FileModel> {
    val sorted = when (option) {
        CoreSortOption.NAME_ASC  -> sortedBy { it.name.lowercase() }
        CoreSortOption.NAME_DESC -> sortedByDescending { it.name.lowercase() }
        CoreSortOption.DATE_ASC  -> sortedBy { it.lastModified }
        CoreSortOption.DATE_DESC -> sortedByDescending { it.lastModified }
        CoreSortOption.SIZE_ASC  -> sortedBy { it.size }
        CoreSortOption.SIZE_DESC -> sortedByDescending { it.size }
        CoreSortOption.TYPE_ASC  -> sortedBy { it.type.name }
        CoreSortOption.TYPE_DESC -> sortedByDescending { it.type.name }
    }
    // Klasörler her zaman üstte
    return sorted.sortedByDescending { it.isDirectory }
}
KOTLIN
ok "SortOption.kt"

# ── Commit & Push ──
git add "$FEAT/"
git commit -m "fix: use FileModel, fix FileRouter imports and exhaustive when"
git push origin sprint1-manual-impl:feat/sprint1-hardening

echo ""
echo -e "${GREEN}✓ Push tamamlandı — GitHub Actions tetiklendi!${NC}"
