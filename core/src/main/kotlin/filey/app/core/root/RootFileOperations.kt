package filey.app.core.root

import filey.app.core.result.FileResult

/**
 * UI ve ViewModel katmanları SADECE bu sınıfı kullanmalı.
 * RootManager.execute() doğrudan çağrılmamalı.
 */
class RootFileOperations internal constructor(
    private val rootManager: RootManager
) {
    companion object {
        val instance by lazy { RootFileOperations(RootManager.instance) }
    }

    suspend fun listDirectory(path: String) =
        rootManager.execute("ls", listOf("-la"), listOf(path))

    suspend fun copyFile(source: String, destination: String) =
        rootManager.execute("cp", listOf("-r"), listOf(source, destination))

    suspend fun moveFile(source: String, destination: String) =
        rootManager.execute("mv", emptyList(), listOf(source, destination))

    suspend fun deleteFile(path: String, recursive: Boolean = false): FileResult<List<String>> {
        val flags = if (recursive) listOf("-rf") else listOf("-f")
        return rootManager.execute("rm", flags, listOf(path))
    }

    suspend fun stat(path: String) =
        rootManager.execute("stat", emptyList(), listOf(path))

    suspend fun createDirectory(path: String) =
        rootManager.execute("mkdir", listOf("-p"), listOf(path))

    suspend fun changePermissions(
        path: String,
        mode: String,
        recursive: Boolean = false
    ): FileResult<List<String>> {
        if (!mode.matches(Regex("^[0-7]{3,4}$|^[ugoa]+[=+\\-][rwxXst]+$"))) {
            return FileResult.Error.Unknown(
                IllegalArgumentException("Geçersiz permission mode: $mode")
            )
        }
        val flags = if (recursive) listOf("-R") else emptyList()
        return rootManager.execute("chmod", flags, listOf(mode, path))
    }
}
