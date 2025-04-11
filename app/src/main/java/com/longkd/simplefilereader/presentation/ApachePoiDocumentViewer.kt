package com.longkd.simplefilereader.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

class ApachePoiDocumentViewer(private val context: Context) {

    private val cacheDir = context.cacheDir
    private val tempHtmlFile by lazy { File(cacheDir, "temp_document.html") }
    private val tag = "ApachePoiViewer"

    /**
     * Main method to process a document from content:URI
     */
    fun processDocument(uri: Uri, webView: WebView, callback: (Boolean, String) -> Unit) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open input stream for URI: $uri")

            val mimeType = context.contentResolver.getType(uri) ?: ""
            val fileName = getFileNameFromUri(uri) ?: "document"

            when {
                mimeType.contains("word") || fileName.endsWith(".docx", ignoreCase = true) -> {
                    processDocxWithApachePoi(inputStream, webView, callback)
                }

                mimeType.contains("spreadsheet") || fileName.endsWith(
                    ".xlsx",
                    ignoreCase = true
                ) -> {
                    processXlsxWithApachePoi(inputStream, webView, callback)
                }

                else -> {
                    callback(false, "Unsupported file type: $mimeType")
                }
            }

            inputStream.close()
        } catch (e: Exception) {
            Log.e(tag, "Error processing document", e)
            callback(false, "Error processing document: ${e.message}")
        }
    }

    /**
     * Process DOCX file using Apache POI
     */
    private fun processDocxWithApachePoi(
        inputStream: InputStream,
        webView: WebView,
        callback: (Boolean, String) -> Unit
    ) {
        try {
            // Load DOCX document
            val document = XWPFDocument(inputStream)

            // Create HTML content
            val htmlContent = StringBuilder()
            htmlContent.append(
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            margin: 20px;
                            line-height: 1.5;
                        }
                        table {
                            border-collapse: collapse;
                            margin: 15px 0;
                            width: 100%;
                        }
                        td, th {
                            border: 1px solid #ddd;
                            padding: 8px;
                        }
                        .heading1 { font-size: 24px; font-weight: bold; margin-top: 20px; margin-bottom: 10px; }
                        .heading2 { font-size: 20px; font-weight: bold; margin-top: 15px; margin-bottom: 10px; }
                        .heading3 { font-size: 16px; font-weight: bold; margin-top: 10px; margin-bottom: 5px; }
                        .paragraph { margin: 10px 0; }
                        .bold { font-weight: bold; }
                        .italic { font-style: italic; }
                        .underline { text-decoration: underline; }
                        .list-item { margin-left: 20px; }
                    </style>
                </head>
                <body>
            """
            )

            // Process paragraphs
            document.paragraphs.forEach { paragraph ->
                val text = paragraph.text

                // Determine paragraph style/heading level
                when {
                    paragraph.style == "Heading1" || paragraph.style?.contains(
                        "heading 1",
                        ignoreCase = true
                    ) == true -> {
                        htmlContent.append("<div class='heading1'>$text</div>")
                    }

                    paragraph.style == "Heading2" || paragraph.style?.contains(
                        "heading 2",
                        ignoreCase = true
                    ) == true -> {
                        htmlContent.append("<div class='heading2'>$text</div>")
                    }

                    paragraph.style == "Heading3" || paragraph.style?.contains(
                        "heading 3",
                        ignoreCase = true
                    ) == true -> {
                        htmlContent.append("<div class='heading3'>$text</div>")
                    }

                    text.isNotEmpty() -> {
//                        if (paragraph.is) {
//                            htmlContent.append("<div class='list-item'>$text</div>")
//                        } else {else
                        htmlContent.append("<div class='paragraph'>$text</div>")
//                        }
                    }
                }
            }

            // Process tables
            document.tables.forEach { table ->
                htmlContent.append("<table>")

                table.rows.forEach { row ->
                    htmlContent.append("<tr>")

                    row.tableCells.forEach { cell ->
                        // Determine if it's a header row
                        val cellTag = if (table.rows.indexOf(row) == 0) "th" else "td"

                        htmlContent.append("<$cellTag>")

                        // Process paragraphs within the cell
                        cell.paragraphs.forEach { cellParagraph ->
                            htmlContent.append("${cellParagraph.text}<br>")
                        }

                        htmlContent.append("</$cellTag>")
                    }

                    htmlContent.append("</tr>")
                }

                htmlContent.append("</table>")
            }

            htmlContent.append("</body></html>")

            // Save HTML to temp file
            FileWriter(tempHtmlFile).use { it.write(htmlContent.toString()) }

            // Load HTML into WebView
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempHtmlFile
            )

            webView.webViewClient = WebViewClient()
            webView.settings.apply {
                javaScriptEnabled = true
                builtInZoomControls = true
                displayZoomControls = false
            }
            webView.loadUrl(fileUri.toString())

            callback(true, "Document loaded successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error processing DOCX", e)
            callback(false, "Error processing DOCX: ${e.message}")
        }
    }

    /**
     * Process XLSX file using Apache POI
     */
    private fun processXlsxWithApachePoi(
        inputStream: InputStream,
        webView: WebView,
        callback: (Boolean, String) -> Unit
    ) {
        try {
            // Load XLSX workbook
            val workbook = XSSFWorkbook(inputStream)

            // Create HTML content
            val htmlContent = StringBuilder()
            htmlContent.append(
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            margin: 15px;
                        }
                        .tabs {
                            display: flex;
                            border-bottom: 1px solid #ccc;
                            margin-bottom: 15px;
                        }
                        .tab {
                            padding: 8px 16px;
                            cursor: pointer;
                            border: 1px solid #ccc;
                            background-color: #f1f1f1;
                            border-radius: 4px 4px 0 0;
                            margin-right: 2px;
                        }
                        .tab.active {
                            background-color: #fff;
                            border-bottom: 1px solid white;
                            margin-bottom: -1px;
                        }
                        .sheet {
                            display: none;
                        }
                        .sheet.active {
                            display: block;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%;
                            margin-bottom: 20px;
                            overflow-x: auto;
                        }
                        th, td {
                            border: 1px solid #ddd;
                            padding: 8px;
                            text-align: left;
                        }
                        th {
                            background-color: #f2f2f2;
                        }
                        .numeric {
                            text-align: right;
                        }
                        .date {
                            text-align: center;
                        }
                    </style>
                    <script>
                        function showSheet(sheetNumber) {
                            // Hide all sheets
                            const sheets = document.getElementsByClassName('sheet');
                            for (let i = 0; i < sheets.length; i++) {
                                sheets[i].classList.remove('active');
                            }
                            
                            // Deactivate all tabs
                            const tabs = document.getElementsByClassName('tab');
                            for (let i = 0; i < tabs.length; i++) {
                                tabs[i].classList.remove('active');
                            }
                            
                            // Show selected sheet and activate tab
                            document.getElementById('sheet-' + sheetNumber).classList.add('active');
                            document.getElementById('tab-' + sheetNumber).classList.add('active');
                        }
                    </script>
                </head>
                <body>
            """
            )

            // Create tabs for each sheet
            htmlContent.append("<div class='tabs'>")
            for (i in 0 until workbook.numberOfSheets) {
                val sheetName = workbook.getSheetName(i)
                htmlContent.append(
                    """
                    <div id="tab-$i" class="tab${if (i == 0) " active" else ""}" onclick="showSheet($i)">
                        $sheetName
                    </div>
                """
                )
            }
            htmlContent.append("</div>")

            // Process each sheet
            for (i in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(i)

                htmlContent.append(
                    """
                    <div id="sheet-$i" class="sheet${if (i == 0) " active" else ""}">
                        <table>
                """
                )

                // Process rows and cells
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                // Determine the used range
                var maxRow = 0
                var maxCol = 0

                for (rowIndex in sheet.firstRowNum..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    maxRow = maxRow.coerceAtLeast(rowIndex)
                    maxCol = maxCol.coerceAtLeast(row.lastCellNum.toInt() - 1)
                }

                // Create header row with column letters
                htmlContent.append("<tr><th></th>") // Corner cell
                for (colIndex in 0..maxCol) {
                    val colLetter = getColumnLetter(colIndex)
                    htmlContent.append("<th>$colLetter</th>")
                }
                htmlContent.append("</tr>")

                // Create data rows
                for (rowIndex in 0..maxRow) {
                    val row = sheet.getRow(rowIndex)

                    htmlContent.append("<tr>")

                    // Add row number cell
                    htmlContent.append("<th>${rowIndex + 1}</th>")

                    // Add data cells
                    for (colIndex in 0..maxCol) {
                        val cell = row?.getCell(colIndex)
                        var cellValue = ""
                        var cellClass = ""

                        // Extract cell value based on its type
                        if (cell != null) {
                            when (cell.cellType) {
                                CellType.NUMERIC -> {
                                    cellClass = if (DateUtil.isCellDateFormatted(cell)) {
                                        cellValue = formatter.format(cell.dateCellValue)
                                        "date"
                                    } else {
                                        // Format number appropriately
                                        if (cell.numericCellValue == cell.numericCellValue.toLong()
                                                .toDouble()
                                        ) {
                                            cellValue = cell.numericCellValue.toLong().toString()
                                        } else {
                                            cellValue = cell.numericCellValue.toString()
                                        }
                                        "numeric"
                                    }
                                }

                                CellType.STRING -> cellValue = cell.stringCellValue
                                CellType.BOOLEAN -> cellValue = cell.booleanCellValue.toString()
                                CellType.FORMULA -> {
                                    try {
                                        cellValue = when (cell.cachedFormulaResultType) {
                                            CellType.NUMERIC -> {
                                                if (cell.numericCellValue == cell.numericCellValue.toLong()
                                                        .toDouble()
                                                ) {
                                                    cell.numericCellValue.toLong().toString()
                                                } else {
                                                    cell.numericCellValue.toString()
                                                }
                                            }

                                            CellType.STRING -> cell.stringCellValue
                                            else -> cell.cellFormula
                                        }
                                    } catch (e: Exception) {
                                        cellValue = "=[Formula]"
                                    }
                                }

                                else -> cellValue = ""
                            }
                        }

                        htmlContent.append("<td class='$cellClass'>$cellValue</td>")
                    }

                    htmlContent.append("</tr>")
                }

                htmlContent.append("</table></div>")
            }

            htmlContent.append("</body></html>")

            // Save HTML to temp file
            FileWriter(tempHtmlFile).use { it.write(htmlContent.toString()) }

            // Load HTML into WebView
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempHtmlFile
            )

            webView.webViewClient = WebViewClient()
            webView.settings.apply {
                javaScriptEnabled = true
                builtInZoomControls = true
                displayZoomControls = false
            }
            webView.loadUrl(fileUri.toString())

            callback(true, "Spreadsheet loaded successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error processing XLSX", e)
            callback(false, "Error processing XLSX: ${e.message}")
        }
    }

    /**
     * Helper function to convert column index to letter (0 = A, 25 = Z, 26 = AA)
     */
    private fun getColumnLetter(columnIndex: Int): String {
        var dividend = columnIndex + 1
        var columnName = ""

        while (dividend > 0) {
            val modulo = (dividend - 1) % 26
            columnName = (65 + modulo).toChar() + columnName
            dividend = (dividend - modulo) / 26
        }

        return columnName
    }

    /**
     * Extract filename from URI
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex("_display_name")
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}