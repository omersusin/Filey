package filey.app.core.util

import android.content.Context
import filey.app.core.permission.PermissionOrchestrator
import filey.app.core.permission.StoragePermissionState

/**
 * Legacy helper.
 * Sprint 1'de PermissionOrchestrator'a yönlendiriyoruz.
 */
object PermissionHelper {

    fun hasStoragePermission(context: android.content.Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            val read = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val write = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            read && write
        }
    }


    fun getStoragePermissionState(context: Context): StoragePermissionState {
        return PermissionOrchestrator(context).getStoragePermissionState()
    }
}
