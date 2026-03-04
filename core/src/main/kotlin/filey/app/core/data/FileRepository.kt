package filey.app.core.data

import filey.app.core.model.FileModel
import filey.app.core.model.StorageInfo

/**
 * Single interface for ALL file operations.
 * Normal / Root / Shizuku implementations all conform to this.
 */
interface FileRepository {
    suspend fun listFiles(path: String): Result<List<FileModel>>
    suspend fun createDirectory(path: String, name: String): Result<Unit>
    suspend fun delete(path: String): Result<Unit>
    suspend fun delete(paths: List<String>): Result<Unit>
    suspend fun rename(path: String, newName: String): Result<String>
    suspend fun copy(
        source: String,
        destination: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Unit>
    suspend fun move(
        source: String,
        destination: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Unit>
    suspend fun getFileInfo(path: String): Result<FileModel>
    suspend fun exists(path: String): Boolean
    suspend fun getStorageInfo(path: String): Result<StorageInfo>
    suspend fun readText(path: String): Result<String>
    suspend fun writeText(path: String, content: String): Result<Unit>
    suspend fun calculateChecksum(path: String, algorithm: String = "SHA-256"): Result<String>
    suspend fun searchFiles(rootPath: String, query: String): Result<List<FileModel>>
    suspend fun getCategoryFiles(category: filey.app.core.model.FileCategory): Result<List<FileModel>>
    
    // ── Trash ──
    suspend fun moveToTrash(path: String): Result<Unit>
    suspend fun restoreFromTrash(path: String): Result<Unit>
    suspend fun getTrashFiles(): Result<List<FileModel>>
    suspend fun emptyTrash(): Result<Unit>
    suspend fun getOwnerApp(path: String): String?
}
