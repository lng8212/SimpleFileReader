package com.longkd.simplefilereader.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object Permission {
    const val READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
}

object PermissionUtil {
    fun checkAndRequestReadExternalStoragePermission(
        context: Context,
        launcher: ActivityResultLauncher<String>,
        onPermissionGranted: () -> Unit
    ) {
        when {
            Build.VERSION.SDK_INT in Build.VERSION_CODES.M until Build.VERSION_CODES.Q -> {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    onPermissionGranted()
                } else {
                    launcher.launch(Permission.READ_EXTERNAL_STORAGE)
                }
            }

            else -> onPermissionGranted()
        }
    }
}