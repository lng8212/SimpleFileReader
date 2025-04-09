package com.longkd.simplefilereader.data.model

import android.net.Uri

data class FileDTO(
    val id: Long,
    val name: String,
    val mime: String,
    val fileUri: Uri,
    val dateModified: Long,
    val fileSize: Long
)