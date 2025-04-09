package com.longkd.simplefilereader.presentation.listfile

import com.longkd.simplefilereader.domain.model.File

sealed interface UiState {
    data object Loading : UiState
    data class Success(val files: List<File>) : UiState
    data class Error(val message: String) : UiState
}