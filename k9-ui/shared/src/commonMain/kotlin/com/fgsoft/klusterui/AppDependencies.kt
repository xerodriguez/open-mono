package com.fgsoft.klusterui

import com.fgsoft.klusterui.data.Database
import com.fgsoft.klusterui.data.ContextRepository
import com.fgsoft.klusterui.data.PortForwardRepository
import com.fgsoft.klusterui.kubectl.KubectlClient
import com.fgsoft.klusterui.kubectl.ProcessManager

data class AppDependencies(
    val database: Database,
    val contextRepository: ContextRepository,
    val portForwardRepository: PortForwardRepository,
    val kubectlClient: KubectlClient,
    val processManager: ProcessManager,
)

expect fun createAppDependencies(): AppDependencies
