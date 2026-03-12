package filey.app.core.model

import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * Tüm dosya operasyonlarının ortak dönüş tipi.
 * UI katmanı bu sealed hierarchy ile çalışabilir.
 */
sealed interface FileResult<out T> {
    data class Success<T>(val data: T) : FileResult<T>

    sealed interface Error : FileResult<Nothing> {
        val message: String
        val cause: Throwable?

        data class PermissionDenied(
            override val message: String = "Permission denied",
            override val cause: Throwable? = null,
            val requiredPermission: PermissionType = PermissionType.STORAGE
        ) : Error

        data class NotFound(
            val path: String,
            override val message: String = "File not found: $path",
            override val cause: Throwable? = null
        ) : Error

        data class RootRequired(
            override val message: String = "Root access required",
            override val cause: Throwable? = null
        ) : Error

        data class RootDenied(
            override val message: String = "Root access denied by user",
            override val cause: Throwable? = null
        ) : Error

        data class IOFailure(
            override val message: String,
            override val cause: Throwable? = null
        ) : Error

        data class ShellCommandFailed(
            val command: String,
            val exitCode: Int,
            val stderr: String,
            override val message: String = "Shell command failed ($exitCode): $stderr",
            override val cause: Throwable? = null
        ) : Error

        data class StorageFull(
            val requiredBytes: Long,
            val availableBytes: Long,
            override val message: String = "Not enough space",
            override val cause: Throwable? = null
        ) : Error

        data class Unknown(
            override val message: String,
            override val cause: Throwable? = null
        ) : Error
    }

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): Error? = this as? Error
}

enum class PermissionType { STORAGE, MANAGE_ALL, ROOT }

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

inline fun <T, R> FileResult<T>.map(transform: (T) -> R): FileResult<R> = when (this) {
    is FileResult.Success -> FileResult.Success(transform(data))
    is FileResult.Error -> this
}

inline fun <T> FileResult<T>.onSuccess(action: (T) -> Unit): FileResult<T> {
    if (this is FileResult.Success) action(data)
    return this
}

inline fun <T> FileResult<T>.onError(action: (FileResult.Error) -> Unit): FileResult<T> {
    if (this is FileResult.Error) action(this)
    return this
}

inline fun <T> FileResult<T>.getOrElse(fallback: (FileResult.Error) -> T): T = when (this) {
    is FileResult.Success -> data
    is FileResult.Error -> fallback(this)
}

suspend inline fun <T> fileResultOf(crossinline block: suspend () -> T): FileResult<T> {
    return try {
        FileResult.Success(block())
    } catch (e: SecurityException) {
        FileResult.Error.PermissionDenied(cause = e)
    } catch (e: FileNotFoundException) {
        FileResult.Error.NotFound(path = e.message ?: "unknown", cause = e)
    } catch (e: IOException) {
        FileResult.Error.IOFailure(message = e.message ?: "IO error", cause = e)
    } catch (e: CancellationException) {
        throw e // Coroutine cancellation'ı yutma
    } catch (e: Exception) {
        FileResult.Error.Unknown(message = e.message ?: "Unknown error", cause = e)
    }
}
