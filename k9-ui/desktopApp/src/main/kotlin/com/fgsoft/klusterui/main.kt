package com.fgsoft.klusterui

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    val deps = remember { createAppDependencies() }

    Window(
        onCloseRequest = {
            deps.processManager.killAllProcesses()
            deps.database.close()
            exitApplication()
        },
        state = WindowState(width = 1280.dp, height = 800.dp),
        title = "KubeKui",
    ) {
        App(deps)
    }
}