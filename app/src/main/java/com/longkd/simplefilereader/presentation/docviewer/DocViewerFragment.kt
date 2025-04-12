package com.longkd.simplefilereader.presentation.docviewer

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.longkd.simplefilereader.databinding.FragmentDocViewerBinding
import com.longkd.simplefilereader.presentation.listfile.model.File
import dagger.hilt.android.AndroidEntryPoint
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream

@AndroidEntryPoint
class DocViewerFragment : Fragment() {
    private var _binding: FragmentDocViewerBinding? = null
    private val binding get() = _binding!!

    private val args: DocViewerFragmentArgs by navArgs()
    private lateinit var file: File

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
        showInWebView(binding.webView, convertXlsxToHtml(requireContext(), file.contentUri, 0))
    }

    fun showInWebView(webView: WebView, html: String) {
        webView.settings.javaScriptEnabled = true
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun convertXlsxToHtml(context: Context, uri: Uri, sheetIndex: Int): String {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)!!
        val workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(sheetIndex)

        val htmlContent = buildString {
            append("<html><head>")

            // Reference the external CSS file from the assets folder
            append("<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/css/style.css\">")

            append("</head><body>")

            // Add table
            append("<table>")

            // Add table headers
            append("<tr>")
            val headerRow = sheet.getRow(0)
            headerRow.forEach { cell ->
                append("<th>${getCellHtml(cell)}</th>")
            }
            append("</tr>")

            // Render table rows and cells
            for (row in sheet) {
                append("<tr>")
                for (cell in row) {
                    append("<td>${getCellHtml(cell)}</td>")
                }
                append("</tr>")
            }

            append("</table>")

            // Reference the external JavaScript file from the assets folder
            append("<script src=\"file:///android_asset/js/script.js\"></script>")

            append("</body></html>")
        }

        inputStream.close()
        return htmlContent
    }

    // Extract the HTML representation of a cell, including its style
    private fun getCellHtml(cell: Cell): String {
        val style = cell.cellStyle
        val font = cell.sheet.workbook.getFontAt(style.fontIndex)

        // Extract text color (if available)
        val textColor = font.color.let {
            "#${Integer.toHexString(it.toInt())}"
        }  // Default black color

        // Extract font size
        val fontSize = font.fontHeightInPoints

        // Extract font family
        val fontFamily = font.fontName

        // Extract text alignment
        val alignment = when (style.alignment) {
            HorizontalAlignment.CENTER -> "center"
            HorizontalAlignment.LEFT -> "left"
            HorizontalAlignment.RIGHT -> "right"
            else -> "left"
        }

        // Generate HTML with style
        return buildString {
            append("<span style=\"")
            append("font-family: $fontFamily; ")
            append("font-size: ${fontSize}pt; ")
            append("color: $textColor; ")
            append("text-align: $alignment; ")
            append("\">")
            append(cell.toString())
            append("</span>")
        }
    }


    fun convertDocxToHtml(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val document = XWPFDocument(inputStream)
        val paragraphs = document.paragraphs

        val html = buildString {
            append("<html><body>")
            for (p in paragraphs) {
                append("<p>${p.text}</p>")
            }
            append("</body></html>")
        }

        inputStream?.close()
        return html
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}