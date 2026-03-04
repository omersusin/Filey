package filey.app.core.data

import filey.app.core.data.root.RootFileRepository
import filey.app.core.data.shizuku.ShizukuFileRepository
import filey.app.core.model.AccessMode
import filey.app.core.model.FileModel
import filey.app.core.model.StorageInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Routes every call to the correct backend based on [modeFlow].
 * The UI sets AccessMode; this class transparently delegates.
 */
class DelegatingFileRepository(
    private val modeFlow: StateFlow<AccessMode>,
    private val normal: FileRepository,
    private val root: FileRepository,
    private val shizuku: FileRepository
) : FileRepository {

    private fun current(): FileRepository = when (modeFlow.value) {
        AccessMode.NORMAL -> normal
        AccessMode.ROOT -> root
        AccessMode.SHIZUKU -> shizuku
    }

    override suspend fun listFiles(path: String) = current().listFiles(path)
    override suspend fun createDirectory(path: String, name: String) = current().createDirectory(path, name)
    override suspend fun delete(path: String) = current().delete(path)
    override suspend fun delete(paths: List<String>) = current().delete(paths)
    override suspend fun rename(path: String, newName: String) = current().rename(path, newName)
    override suspend fun copy(source: String, destination: String, onProgress: ((Float) -> Unit)?) =
        current().copy(source, destination, onProgress)
    override suspend fun move(source: String, destination: String, onProgress: ((Float) -> Unit)?) =
        current().move(source, destination, onProgress)
    override suspend fun getFileInfo(path: String) = current().getFileInfo(path)
    override suspend fun exists(path: String) = current().exists(path)
    override suspend fun getStorageInfo(path: String) = current().getStorageInfo(path)
    override suspend fun readText(path: String) = current().readText(path)
    override suspend fun writeText(path: String, content: String) = current().writeText(path, content)
    override suspend fun calculateChecksum(path: String, algorithm: String) =
        current().calculateChecksum(path, algorithm)
    override suspend fun searchFiles(rootPath: String, query: String) =
        current().searchFiles(rootPath, query)
    override suspend fun getCategoryFiles(category: filey.app.core.model.FileCategory) =
        current().getCategoryFiles(category)

    override suspend fun moveToTrash(path: String) = current().moveToTrash(path)
    override suspend fun restoreFromTrash(path: String) = current().restoreFromTrash(path)
    override suspend fun getTrashFiles() = current().getTrashFiles()
    override suspend fun emptyTrash() = current().emptyTrash()
    override suspend fun getOwnerApp(path: String) = current().getOwnerApp(path)
}
