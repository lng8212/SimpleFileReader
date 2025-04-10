package com.longkd.simplefilereader.presentation.pdfviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.longkd.simplefilereader.databinding.FragmentPdfViewerBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PdfViewerFragment : Fragment() {
    private var _binding: FragmentPdfViewerBinding? = null
    private val binding get() = _binding!!

    private val args: PdfViewerFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fileUri = args.uri.toUri()
        binding.viewPdf.initWithUri(fileUri)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}