package com.fgsoft.klusterui

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun cursorHorizontalResize(): PointerIcon? =
    try {
        PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))
    } catch (_: Exception) {
        null
    }

actual fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}
