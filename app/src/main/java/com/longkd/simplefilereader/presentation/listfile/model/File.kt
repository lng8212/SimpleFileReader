package com.longkd.simplefilereader.presentation.listfile.model

import android.net.Uri
import android.os.Parcelable
import com.longkd.simplefilereader.domain.model.FileType
import kotlinx.parcelize.Parcelize

@Parcelize
data class File(
    val name: String,
    val desc: String,
    val fileType: FileType,
    val contentUri: Uri
) : Parcelable


