package com.longkd.simplefilereader.domain.model

import android.net.Uri

data class FileDTO(
    val name: String,
    val dateModifier: String,
    val size: String,
    val uri: Uri,
    val type: FileType
)