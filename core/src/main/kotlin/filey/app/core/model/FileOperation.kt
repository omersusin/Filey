package filey.app.core.model

sealed class FileOperation {
    data class Rename(val oldPath: String, val newPath: String) : FileOperation()
    data class Move(val sourcePaths: List<String>, val destinationDir: String) : FileOperation()
    data class Trash(val originalPaths: List<String>, val trashPaths: List<String>) : FileOperation()
}
