package filey.app.core.data.shizuku

import android.content.Context
import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.shizuku.ShizukuShell
import rikka.shizuku.Shizuku

/**
 * Manages Shizuku lifecycle: install check, service status, permission.
 * Uses libsu for robust shell execution over Shizuku.
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

    private var shizukuShell: Shell? = null

    @Synchronized
    private fun getShell(): Shell {
        val current = shizukuShell
        if (current != null && current.isAlive) return current
        
        val newShell = Shell.Builder.create()
            .setFlags(Shell.FLAG_REDIRECT_STDERR)
            .build(ShizukuShell.Builder().build())
        shizukuShell = newShell
        return newShell
    }

    /**
     * Execute a shell command with Shizuku (ADB-level) privileges.
     * Returns (stdout lines, stderr lines, exit code).
     */
    fun exec(vararg command: String): Triple<List<String>, List<String>, Int> {
        return try {
            val result = getShell().newJob().add(*command).exec()
            Triple(result.out, result.err, result.code)
        } catch (e: Exception) {
            Triple(emptyList(), listOf(e.message ?: "Unknown error"), -1)
        }
    }
}
