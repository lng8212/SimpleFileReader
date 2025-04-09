package com.longkd.simplefilereader.domain

import com.longkd.simplefilereader.domain.model.FileDTO
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getFiles(): Flow<List<FileDTO>>
}