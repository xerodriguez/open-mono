package com.fgsoft.klusterui

import androidx.compose.ui.input.pointer.PointerIcon

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun cursorHorizontalResize(): PointerIcon?

expect fun copyToClipboard(text: String)
