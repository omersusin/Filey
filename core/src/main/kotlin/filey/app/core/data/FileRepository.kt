package filey.app.core.data

import filey.app.core.model.FileResult
import filey.app.core.model.FileResult.Error
import filey.app.core.model.PermissionType
import kotlinx.coroutines.flow.Flow

interface FileRepository {

    suspend fun listFiles(path: String): FileResult<List<FileModel>>

    suspend fun copy(
        source: String,
        destination: String,
        onProgress: (FileProgress) -> Unit = {}
    ): FileResult<Unit>

    suspend fun move(source: String, destination: String): FileResult<Unit>

    suspend fun delete(path: String): FileResult<Unit>

    suspend fun rename(oldPath: String, newName: String): FileResult<String>

    suspend fun createFile(parentPath: String, name: String): FileResult<String>

    suspend fun createDirectory(parentPath: String, name: String): FileResult<String>

    suspend fun getFileInfo(path: String): FileResult<FileModel>

    fun search(rootPath: String, query: String): Flow<FileModel>

    suspend fun calculateSize(path: String): FileResult<Long>
}

data class FileProgress(
    val currentFile: String,
    val currentFileIndex: Int,
    val totalFiles: Int,
    val bytesProcessed: Long,
    val totalBytes: Long
) {
    val percentage: Float
        get() = if (totalBytes > 0) bytesProcessed.toFloat() / totalBytes else 0f
}

fun Error.toRequiredPermissionOrNull(): PermissionType? = when (this) {
    is Error.PermissionDenied -> requiredPermission
    is Error.RootRequired, is Error.RootDenied -> PermissionType.ROOT
    else -> null
}
