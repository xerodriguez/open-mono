package com.fgsoft.klusterui

import com.fgsoft.klusterui.data.Database
import com.fgsoft.klusterui.data.ContextRepository
import com.fgsoft.klusterui.data.JvmDatabase
import com.fgsoft.klusterui.data.PortForwardRepository
import com.fgsoft.klusterui.kubectl.JvmKubectlClient
import com.fgsoft.klusterui.kubectl.JvmProcessManager
import com.fgsoft.klusterui.kubectl.KubectlClient
import com.fgsoft.klusterui.kubectl.ProcessManager
import java.io.File

actual fun createAppDependencies(): AppDependencies {
    val appDir = File(System.getProperty("user.home"), ".klusterui")
    appDir.mkdirs()
    val dbPath = File(appDir, "klusterui.db").absolutePath

    val database = JvmDatabase(dbPath)
    database.connect()

    val contextRepository = ContextRepository(database)
    val portForwardRepository = PortForwardRepository(database)
    val kubectlClient = JvmKubectlClient()
    val processManager = JvmProcessManager()

    return AppDependencies(
        database = database,
        contextRepository = contextRepository,
        portForwardRepository = portForwardRepository,
        kubectlClient = kubectlClient,
        processManager = processManager,
    )
}
