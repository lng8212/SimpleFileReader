package com.longkd.simplefilereader.presentation.listfile

import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.longkd.simplefilereader.data.SharedPrefsManager
import com.longkd.simplefilereader.domain.FileRepository
import com.longkd.simplefilereader.domain.model.FileDTO
import com.longkd.simplefilereader.presentation.listfile.model.FileMapper.toFile
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
class ListFileViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val sharedPrefsManager: SharedPrefsManager
) : ViewModel() {

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)

    val shouldRequestSafFolder: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && sharedPrefsManager.getFolderUri() == null

    fun onFolderSelected(uri: Uri?) {
        if (uri != null) {
            sharedPrefsManager.saveFolderUri(uri)
            _permissionState.value = PermissionState.Granted
        } else {
            _permissionState.value = PermissionState.Denied
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> = _permissionState
        .flatMapLatest { state ->
            when (state) {
                PermissionState.NotRequested -> flow { emit(UiState.Loading) }
                PermissionState.Denied -> flow { emit(UiState.Error("Storage permission is required")) }
                PermissionState.Granted -> fileRepository.getFiles()
                    .map<List<FileDTO>, UiState> {
                        UiState.Success(it.map { fileDTO -> fileDTO.toFile() })
                    }
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