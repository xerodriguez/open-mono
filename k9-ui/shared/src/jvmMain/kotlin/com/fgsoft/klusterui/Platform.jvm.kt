package com.fgsoft.klusterui

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

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
