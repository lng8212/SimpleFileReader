package com.longkd.simplefilereader.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(date)
    }

}