package filey.app.core.result

import android.content.Context
import android.util.Log
import filey.app.core.R

object ResultMapper {

    fun mapErrorToUserMessage(error: FileResult.Error, context: Context): String = when (error) {
        is FileResult.Error.IOFailure ->
            context.getString(R.string.error_io, error.cause.localizedMessage ?: "")
        is FileResult.Error.PermissionDenied ->
            context.getString(R.string.error_permission, error.path)
        is FileResult.Error.NotFound ->
            context.getString(R.string.error_not_found, error.path)
        is FileResult.Error.RootRequired ->
            context.getString(R.string.error_root_required)
        is FileResult.Error.ShellCommandFailed ->
            context.getString(R.string.error_shell_failed, error.exitCode.toString())
        is FileResult.Error.Unknown ->
            context.getString(R.string.error_unknown, error.cause.localizedMessage ?: "")
    }

    fun mapErrorToDebugInfo(error: FileResult.Error): String = when (error) {
        is FileResult.Error.ShellCommandFailed -> buildString {
            appendLine("Command  : ${error.command}")
            appendLine("Exit Code: ${error.exitCode}")
            appendLine("STDOUT   : ${error.stdout}")
            appendLine("STDERR   : ${error.stderr}")
        }
        is FileResult.Error.IOFailure -> "IOException: ${error.cause.stackTraceToString()}"
        is FileResult.Error.Unknown   -> "Unknown: ${error.cause.stackTraceToString()}"
        else -> error.toString()
    }

    fun logIfDebug(tag: String, error: FileResult.Error, isDebug: Boolean) {
        if (isDebug) Log.e(tag, mapErrorToDebugInfo(error))
    }
}
