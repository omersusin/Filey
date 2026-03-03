package filey.app.core.util

import android.content.Context
import filey.app.core.permission.PermissionOrchestrator
import filey.app.core.permission.StoragePermissionState

/**
 * Legacy helper.
 * Sprint 1'de PermissionOrchestrator'a yönlendiriyoruz.
 */
object PermissionHelper {

    fun getStoragePermissionState(context: Context): StoragePermissionState {
        return PermissionOrchestrator(context).getStoragePermissionState()
    }
}
