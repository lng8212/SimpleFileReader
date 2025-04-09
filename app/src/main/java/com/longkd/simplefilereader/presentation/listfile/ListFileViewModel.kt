package com.longkd.simplefilereader.presentation.listfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.longkd.simplefilereader.domain.FileRepository
import com.longkd.simplefilereader.domain.model.File
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ListFileViewModel @Inject constructor(private val fileRepository: FileRepository) :
    ViewModel() {

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> = _permissionState
        .flatMapLatest { permissionState ->
            when (permissionState) {
                PermissionState.NotRequested -> flow { emit(UiState.Loading) }
                PermissionState.Denied -> flow { emit(UiState.Error("Storage permission is required")) }
                PermissionState.Granted -> fileRepository.getFiles()
                    .map<List<File>, UiState> { UiState.Success(it) }
                    .onStart { emit(UiState.Loading) }
                    .catch { emit(UiState.Error("Failed to load files: ${it.message}")) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun onPermissionResult(isGranted: Boolean) {
        _permissionState.value = if (isGranted) PermissionState.Granted else PermissionState.Denied
    }

    sealed class PermissionState {
        data object NotRequested : PermissionState()
        data object Granted : PermissionState()
        data object Denied : PermissionState()
    }
}