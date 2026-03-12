package filey.app.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

/**
 * Storage izin durumunu merkezi yönetir.
 * Not: Android 11+ için File Manager semantiği gerektiğinden MANAGE_EXTERNAL_STORAGE kullanılır.
 */
class PermissionOrchestrator(
    private val appContext: Context
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
                    appContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                val writeGranted = ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                if (readGranted && writeGranted) {
                    StoragePermissionState.Granted
                } else {
                    StoragePermissionState.NeedsRuntimePermission(
                        permissions = buildList {
                            if (!readGranted) add(Manifest.permission.READ_EXTERNAL_STORAGE)
                            if (!writeGranted) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    )
                }
            }

            else -> StoragePermissionState.Granted
        }
    }
}

sealed interface StoragePermissionState {
    data object Granted : StoragePermissionState

    data class NeedsRuntimePermission(
        val permissions: List<String>
    ) : StoragePermissionState

    data object NeedsManageAllFiles : StoragePermissionState
}
