package com.longkd.simplefilereader.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.longkd.simplefilereader.data.model.FileObject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FileDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsManager: SharedPrefsManager
) {
    companion object {
        private val TAG = FileDataSource::class.simpleName
    }

    private val supportedMimeTypes = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel"
    )

    fun getFiles(): List<FileObject> {
        val mediaFiles = getMediaStoreFiles()
        val safFiles = prefsManager.getFolderUri()?.let { getFilesFromSaf(it) } ?: emptyList()
        return mediaFiles + safFiles
    }


    private fun getMediaStoreFiles(): List<FileObject> {
        val fileList = mutableListOf<FileObject>()
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.SIZE
            )

            val selection = supportedMimeTypes.joinToString(" OR ") {
                "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
            }

            val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

            context.contentResolver.query(
                collection,
                projection,
                selection,
                supportedMimeTypes,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val mime = cursor.getString(mimeCol)
                    val date = cursor.getLong(dateCol)
                    val size = cursor.getLong(sizeCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    fileList.add(FileObject(id, name, mime, contentUri, date, size))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore query error: ${e.message}", e)
        }

        return fileList
    }

    private fun getFilesFromSaf(uri: Uri): List<FileObject> {
        val fileList = mutableListOf<FileObject>()
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )

        context.contentResolver.query(childrenUri, arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        ), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                val documentId = cursor.getString(1)
                val mime = cursor.getString(2)
                val size = cursor.getLong(3)
                val date = cursor.getLong(4)

                val fileUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
                fileList.add(FileObject(0, name, mime, fileUri, date, size))
            }
        }

        return fileList
    }
}