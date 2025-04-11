package com.longkd.simplefilereader.presentation.docviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.longkd.simplefilereader.databinding.FragmentDocViewerBinding
import com.longkd.simplefilereader.presentation.ApachePoiDocumentViewer
import com.longkd.simplefilereader.presentation.listfile.model.File
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DocViewerFragment : Fragment() {
    private var _binding: FragmentDocViewerBinding? = null
    private val binding get() = _binding!!

    private val args: DocViewerFragmentArgs by navArgs()
    private lateinit var file: File

    private val apachePoiDocumentViewer by lazy {
        ApachePoiDocumentViewer(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        file = args.file
        binding.webView.settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        binding.pbLoading.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            apachePoiDocumentViewer.processDocument(
                file.contentUri,
                binding.webView
            ) { success, message ->
                lifecycleScope.launch {
                    binding.pbLoading.visibility = View.GONE
                    if (!success) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}