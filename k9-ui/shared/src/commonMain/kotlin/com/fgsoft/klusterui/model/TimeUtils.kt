package com.fgsoft.klusterui.model

expect fun currentTimeMillis(): Long

expect fun formatTimestamp(millis: Long): String
