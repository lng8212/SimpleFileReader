package com.longkd.simplefilereader.presentation.listfile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.longkd.simplefilereader.databinding.FragmentListFileBinding
import com.longkd.simplefilereader.util.PermissionUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListFileFragment : Fragment() {
    private var _binding: FragmentListFileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ListFileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PermissionUtil.checkAndRequestReadExternalStoragePermission(
            requireContext(),
            requestPermissionLauncher
        ) {
            viewModel.onPermissionResult(true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            Log.d(TAG, "UiState.Loading")
                        }

                        is UiState.Success -> {
                            Log.d(TAG, "UiState.Success: ${state.files.joinToString()}")
                        }

                        is UiState.Error -> {
                            Log.d(TAG, "UiState.Error: ${state.message}")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.onPermissionResult(isGranted)
        }

    companion object {
        private val TAG = ListFileFragment::class.simpleName
    }
}