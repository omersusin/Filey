package filey.app.core.result

sealed class FileResult<out T> {
    data class Success<T>(val data: T) : FileResult<T>()

    sealed class Error : FileResult<Nothing>() {
        data class IOFailure(val cause: Throwable) : Error()
        data class PermissionDenied(val path: String) : Error()
        data class NotFound(val path: String) : Error()
        object RootRequired : Error()
        data class ShellCommandFailed(
            val command: String,
            val exitCode: Int,
            val stdout: String,
            val stderr: String
        ) : Error()
        data class Unknown(val cause: Throwable) : Error()
    }
}
