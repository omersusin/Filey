package com.omersusin.filey.core.util

import android.content.Context
import android.os.Build
import android.os.Environment

object PermissionHelper {
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }
}
