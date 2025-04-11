package com.longkd.simplefilereader.presentation.pdfviewer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.longkd.simplefilereader.R
import com.longkd.simplefilereader.databinding.FragmentPdfViewerBinding
import com.longkd.simplefilereader.domain.model.FileType
import com.longkd.simplefilereader.presentation.listfile.model.File
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PdfViewerFragment : Fragment() {
    private var _binding: FragmentPdfViewerBinding? = null
    private val binding get() = _binding!!

    private val args: PdfViewerFragmentArgs by navArgs()
    private lateinit var file: File

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
        file = args.file
        setupToolbar(file.name)
        binding.viewPdf.initWithUri(file.contentUri)
    }

    private fun setupToolbar(name: String) {
        binding.toolbarLayout.apply {
            toolbarTitle.text = name
            (activity as AppCompatActivity).setSupportActionBar(detailToolbar)
            (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
            (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
            (activity as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)

            detailToolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            val menuHost: MenuHost = requireActivity()
            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.detail_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_more -> {
                            showMoreOptions()
                            true
                        }

                        else -> false
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }
    }


    private fun showMoreOptions() {
        val popupMenu = PopupMenu(
            requireContext(),
            binding.toolbarLayout.detailToolbar.findViewById(R.id.action_more)
        )
        popupMenu.menuInflater.inflate(R.menu.detail_popup_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_view -> {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        type = when (file.fileType) {
                            FileType.PDF -> "application/pdf"
                            FileType.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            FileType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            else -> "*/*"
                        }
                        setDataAndType(file.contentUri, type)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context?.startActivity(Intent.createChooser(viewIntent, "View file via"))
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPdf.closePdfRender()
        _binding = null
    }
}