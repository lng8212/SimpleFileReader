package com.longkd.simplefilereader.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.longkd.simplefilereader.data.model.FileDTO
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FileDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getFiles(): List<FileDTO> {
        val fileList = mutableListOf<FileDTO>()

        val mimeTypes = arrayOf(
            "application/pdf",                                         // PDF
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
            "application/msword",                                      // DOC
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // XLSX
            "application/vnd.ms-excel"                                 // XLS
        )

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE
        )

        val selection = StringBuilder().apply {
            append("${MediaStore.Files.FileColumns.MIME_TYPE} = ?")
            for (i in 1 until mimeTypes.size) {
                append(" OR ${MediaStore.Files.FileColumns.MIME_TYPE} = ?")
            }
        }.toString()

        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            mimeTypes,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateModifiedColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val mime = cursor.getString(mimeColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val size = cursor.getLong(sizeColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Files.getContentUri("external"),
                    id
                )

                fileList.add(
                    FileDTO(
                        id = id,
                        name = name,
                        mime = mime,
                        fileUri = contentUri,
                        dateModified = dateModified,
                        fileSize = size
                    )
                )
            }
        }
        Log.d(TAG, "getFiles: ${fileList.size}")
        return fileList
    }

    companion object {
        private val TAG = FileDataSource::class.simpleName
    }
}