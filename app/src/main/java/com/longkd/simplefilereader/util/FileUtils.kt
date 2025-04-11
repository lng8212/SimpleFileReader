package com.longkd.simplefilereader.util

import android.annotation.SuppressLint
import com.longkd.simplefilereader.domain.model.FileType
import kotlin.math.log10
import kotlin.math.pow

object FileUtils {
    @SuppressLint("DefaultLocale")
    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(sizeInBytes.toDouble()) / log10(1024.0)).toInt()
        return String.format("%.1f %s", sizeInBytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }

    fun mapMimeTypeToFileType(mimeType: String?): FileType {
        return when (mimeType) {
            "application/pdf" -> FileType.PDF

            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword" -> FileType.DOCX

            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel" -> FileType.XLSX

            else -> FileType.UNKNOWN
        }
    }

    fun cachedFileNameWithFormat(name: Any, format: String = ".jpg") = "$name$format"
}