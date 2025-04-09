package com.longkd.simplefilereader.data

import com.longkd.simplefilereader.data.model.FileObject
import com.longkd.simplefilereader.domain.FileRepository
import com.longkd.simplefilereader.domain.model.FileDTO
import com.longkd.simplefilereader.util.DateUtils
import com.longkd.simplefilereader.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class FileRepositoryImpl @Inject constructor(private val dataSource: FileDataSource) :
    FileRepository {
    override fun getFiles(): Flow<List<FileDTO>> = flow {
        emit(dataSource.getFiles().map { it.toFile() })
    }.flowOn(Dispatchers.IO)

    private fun FileObject.toFile(): FileDTO {
        return FileDTO(
            name = name,
            dateModifier = DateUtils.formatDate(dateModified),
            size = FileUtils.formatFileSize(fileSize),
            uri = fileUri,
            type = FileUtils.mapMimeTypeToFileType(mime)
        )
    }
}