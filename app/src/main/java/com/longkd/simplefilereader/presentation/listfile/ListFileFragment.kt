package com.longkd.simplefilereader.presentation.listfile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.longkd.simplefilereader.databinding.FragmentListFileBinding
import com.longkd.simplefilereader.domain.model.FileType
import com.longkd.simplefilereader.util.PermissionUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListFileFragment : Fragment() {
    private var _binding: FragmentListFileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ListFileViewModel by viewModels()

    private val adapter by lazy {
        ListFileAdapter { file ->
            when (file.fileType) {
                FileType.PDF -> {
                    val action =
                        ListFileFragmentDirections.actionListFileFragmentToPdfViewerFragment(
                            file.contentUri.toString()
                        )
                    findNavController().navigate(action)
                }

                FileType.DOCX -> TODO()
                FileType.XLSX -> TODO()
                FileType.UNKNOWN -> TODO()
            }
        }
    }

    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            viewModel.onFolderSelected(uri)
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.onPermissionResult(isGranted)
        }

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
        if (viewModel.shouldRequestSafFolder) {
            folderPickerLauncher.launch(null)
        } else {
            PermissionUtil.checkAndRequestReadExternalStoragePermission(
                requireContext(),
                requestPermissionLauncher
            ) {
                viewModel.onPermissionResult(true)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        startObserving()

    }

    private fun initRecyclerView() {
        binding.rvMain.apply {
            adapter = this@ListFileFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
            setHasFixedSize(true)
        }
    }

    private fun startObserving() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            Log.d(TAG, "UiState.Loading")
                            binding.pbLoading.visibility = View.VISIBLE
                        }

                        is UiState.Success -> {
                            Log.d(TAG, "UiState.Success : ${state.files.size}")
                            binding.pbLoading.visibility = View.GONE
                            adapter.submitList(state.files)
                        }

                        is UiState.Error -> {
                            binding.pbLoading.visibility = View.GONE
                            Log.d(TAG, "UiState.Error: ${state.message}")
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG)
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

    companion object {
        private val TAG = ListFileFragment::class.simpleName
    }
}