package com.longkd.simplefilereader.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.core.content.edit
import androidx.core.net.toUri

class SharedPrefsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveFolderUri(uri: Uri) {
        prefs.edit { putString("folder_uri", uri.toString()) }
    }

    fun getFolderUri(): Uri? {
        return prefs.getString("folder_uri", null)?.toUri()
    }
}
