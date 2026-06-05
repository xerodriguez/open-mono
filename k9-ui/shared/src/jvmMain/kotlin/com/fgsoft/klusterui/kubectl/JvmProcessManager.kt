package com.fgsoft.klusterui.kubectl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmProcessManager : ProcessManager {

    private val activeProcesses = mutableMapOf<Long, Process>()

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
        val args = buildList {
            add("kubectl")
            if (context.isNotEmpty()) {
                add("--context")
                add(context)
            }
            add("port-forward")
            add("$resourceType/$resourceName")
            if (namespace.isNotEmpty()) {
                add("-n")
                add(namespace)
            }
            add("$localPort:$remotePort")
        }

        val process = ProcessBuilder(args)
            .redirectErrorStream(false)
            .start()

        val pid = process.pid()
        activeProcesses[pid] = process

        Thread {
            process.inputStream.bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    onOutput(line)
                    line = reader.readLine()
                }
            }
        }.apply { isDaemon = true; start() }

        Thread {
            process.errorStream.bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    onError(line)
                    line = reader.readLine()
                }
            }
        }.apply { isDaemon = true; start() }

        return PortForwardHandle(
            processId = pid,
            localPort = localPort,
            remotePort = remotePort,
        )
    }

    override fun killProcess(handle: PortForwardHandle) {
        killProcessByPid(handle.processId)
    }

    override fun killProcessByPid(pid: Long) {
        val process = activeProcesses.remove(pid)
        process?.let {
            if (it.isAlive) {
                it.destroy()
                try {
                    it.waitFor()
                } catch (_: InterruptedException) {
                    it.destroyForcibly()
                }
            }
        }
    }

    override fun killAllProcesses() {
        activeProcesses.values.forEach { process ->
            if (process.isAlive) {
                process.destroyForcibly()
            }
        }
        activeProcesses.clear()
    }

    fun isProcessAlive(pid: Long): Boolean {
        return activeProcesses[pid]?.isAlive ?: false
    }

    fun getAllActivePids(): List<Long> {
        return activeProcesses.filter { it.value.isAlive }.keys.toList()
    }
}
