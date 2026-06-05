package com.fgsoft.klusterui.kubectl

interface ProcessManager {
    fun startPortForward(
        context: String,
        namespace: String,
        resourceType: String,
        resourceName: String,
        localPort: Int,
        remotePort: Int,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit,
    ): PortForwardHandle

    fun killProcess(handle: PortForwardHandle)

    fun killProcessByPid(pid: Long)

    fun killAllProcesses()

    companion object {
        const val PORT_FORWARD_TIMEOUT_MS = 5000L
    }
}

data class PortForwardHandle(
    val processId: Long,
    val localPort: Int,
    val remotePort: Int,
)
