package com.longkd.simplefilereader.domain

import com.longkd.simplefilereader.domain.model.File
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getFiles(): Flow<List<File>>
}