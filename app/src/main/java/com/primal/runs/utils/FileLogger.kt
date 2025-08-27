package com.primal.runs.utils

import android.content.Context
import java.io.File

object FileLogger {

        fun log(context: Context, message: String) {
            val logFile = File(context.getExternalFilesDir(null), "my_app_logs.txt")
            try {
                logFile.appendText("${getTimestamp()} $message\n")
            } catch (e: Exception) {
                // Handle error
            }
        }

        private fun getTimestamp(): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            return sdf.format(java.util.Date())
        }
}