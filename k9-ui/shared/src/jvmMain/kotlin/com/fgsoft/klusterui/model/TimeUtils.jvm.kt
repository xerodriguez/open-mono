package com.fgsoft.klusterui.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatTimestamp(millis: Long): String {
    if (millis == 0L) return ""
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}
