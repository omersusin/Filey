package filey.app.core.permission

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

/**
 * Tüm permission logic'ini merkezi yönetir.
 * Sprint 1 hedefi: storage permission state'i doğru hesaplamak.
 */
class PermissionOrchestrator(
    private val context: Context
) {

    fun getStoragePermissionState(): StoragePermissionState {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    StoragePermissionState.Granted
                } else {
                    StoragePermissionState.NeedsManageAllFiles
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val readGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                // Android 10 ve altı için WRITE_EXTERNAL_STORAGE kontrolü mantıklı
                val writeGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                if (readGranted && writeGranted) {
                    StoragePermissionState.Granted
                } else {
                    val perms = buildList {
                        if (!readGranted) add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        if (!writeGranted) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    StoragePermissionState.NeedsRuntimePermission(perms)
                }
            }

            else -> StoragePermissionState.Granted
        }
    }
}

sealed interface StoragePermissionState {
    data object Granted : StoragePermissionState
    data class NeedsRuntimePermission(val permissions: List<String>) : StoragePermissionState
    data object NeedsManageAllFiles : StoragePermissionState
}
