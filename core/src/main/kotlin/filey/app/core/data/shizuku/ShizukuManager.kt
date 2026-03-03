package filey.app.core.data.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Manages Shizuku lifecycle: install check, service status, permission.
 */
object ShizukuManager {

    /** Is Shizuku app installed on this device? */
    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Is Shizuku service currently running? */
    fun isServiceRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    /** Does our app have Shizuku permission? */
    fun hasPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    /** Request Shizuku permission (call from Activity with listener). */
    fun requestPermission(requestCode: Int) {
        if (!Shizuku.isPreV11()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    /**
     * Execute a shell command with Shizuku (ADB-level) privileges.
     * Returns (stdout lines, stderr lines, exit code).
     */
    fun exec(vararg command: String): Triple<List<String>, List<String>, Int> {
        return try {
            val fullCommand = command.joinToString(" && ")
            val process = Shizuku.newProcess(arrayOf("sh", "-c", fullCommand), null, null)
            val stdout = process.inputStream.bufferedReader().readLines()
            val stderr = process.errorStream.bufferedReader().readLines()
            val exitCode = process.waitFor()
            Triple(stdout, stderr, exitCode)
        } catch (e: Exception) {
            Triple(emptyList(), listOf(e.message ?: "Unknown error"), -1)
        }
    }
}
