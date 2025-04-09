package com.longkd.simplefilereader.data

import com.longkd.simplefilereader.data.model.FileDTO
import com.longkd.simplefilereader.domain.FileRepository
import com.longkd.simplefilereader.domain.model.File
import com.longkd.simplefilereader.util.DateUtils
import com.longkd.simplefilereader.util.FileUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FileRepositoryImpl @Inject constructor(private val dataSource: FileDataSource) :
    FileRepository {
    override fun getFiles(): Flow<List<File>> = flow {
        emit(dataSource.getFiles().map { it.toFile() })
    }

    private fun FileDTO.toFile(): File {
        return File(
            name = name,
            dateModifier = DateUtils.formatDate(dateModified),
            size = FileUtils.formatFileSize(fileSize),
            path = fileUri.path ?: "",
            type = FileUtils.mapMimeTypeToFileType(mime)
        )
    }
}