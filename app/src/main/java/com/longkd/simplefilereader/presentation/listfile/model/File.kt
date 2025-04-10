package com.longkd.simplefilereader.presentation.listfile.model

import android.net.Uri
import com.longkd.simplefilereader.domain.model.FileType

data class File(
    val name: String,
    val desc: String,
    val fileType: FileType,
    val contentUri: Uri
)


