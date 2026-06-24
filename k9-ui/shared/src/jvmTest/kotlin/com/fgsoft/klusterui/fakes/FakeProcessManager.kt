package com.fgsoft.klusterui.fakes

import com.fgsoft.klusterui.kubectl.PortForwardHandle
import com.fgsoft.klusterui.kubectl.ProcessManager

class FakeProcessManager : ProcessManager {
    val startedForwards = mutableListOf<StartCall>()
    val killedPids = mutableListOf<Long>()
    val killedHandles = mutableListOf<PortForwardHandle>()
    var killedAll = false
        private set

    private var nextPid: Long = 1000

    data class StartCall(
        val context: String,
        val namespace: String,
        val resourceType: String,
        val resourceName: String,
        val localPort: Int,
        val remotePort: Int,
    )

    override fun startPortForward(
        context: String,
        namespace: String,
        resourceType: String,
        resourceName: String,
        localPort: Int,
        remotePort: Int,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit,
    ): PortForwardHandle {
        val pid = nextPid++
        startedForwards.add(
            StartCall(context, namespace, resourceType, resourceName, localPort, remotePort),
        )
        return PortForwardHandle(processId = pid, localPort = localPort, remotePort = remotePort)
    }

    override fun killProcess(handle: PortForwardHandle) {
        killedHandles.add(handle)
        killedPids.add(handle.processId)
    }

    override fun killProcessByPid(pid: Long) {
        killedPids.add(pid)
    }

    override fun killAllProcesses() {
        killedAll = true
    }
}
