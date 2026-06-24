package com.fgsoft.klusterui.ui.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fgsoft.klusterui.AppDependencies
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess
import com.fgsoft.klusterui.model.currentTimeMillis
import kotlinx.coroutines.CoroutineScope

class PortForwardStore(
    private val deps: AppDependencies,
    private val scope: CoroutineScope,
) {
    var configs: List<PortForwardConfig> by mutableStateOf(emptyList())
        private set
    var activeProcesses: List<PortForwardProcess> by mutableStateOf(emptyList())
        private set

    fun loadConfigs(contextId: Long) {
        configs = deps.portForwardRepository.getConfigsForContext(contextId)
    }

    fun addConfig(config: PortForwardConfig): Long {
        val id = deps.portForwardRepository.createConfig(config)
        loadConfigs(config.contextId)
        return id
    }

    fun updateConfig(config: PortForwardConfig) {
        deps.portForwardRepository.updateConfig(config)
        loadConfigs(config.contextId)
    }

    fun deleteConfig(id: Long) {
        stopByConfigId(id)
        deps.portForwardRepository.deleteConfig(id)
        activeProcesses = deps.portForwardRepository.getActiveProcesses()
    }

    fun start(
        config: PortForwardConfig,
        podName: String,
        kubectlContext: String,
    ) {
        val handle =
            deps.processManager.startPortForward(
                context = kubectlContext,
                namespace = config.namespace,
                resourceType = config.resourceType,
                resourceName = config.resourceName,
                localPort = config.localPort,
                remotePort = config.remotePort,
                onOutput = { },
                onError = { },
            )
        val process =
            PortForwardProcess(
                configId = config.id,
                localPort = config.localPort,
                remotePort = config.remotePort,
                podName = podName,
                namespace = config.namespace,
                pid = handle.processId,
                isRunning = true,
                startedAt = currentTimeMillis(),
            )
        deps.portForwardRepository.createProcess(process)
        refreshProcesses()
    }

    fun stop(processId: Long) {
        val process = activeProcesses.find { it.id == processId } ?: return
        deps.processManager.killProcessByPid(process.pid)
        deps.portForwardRepository.updateProcess(process.copy(isRunning = false))
        refreshProcesses()
    }

    fun killAll() {
        activeProcesses.forEach { process ->
            deps.processManager.killProcessByPid(process.pid)
            deps.portForwardRepository.updateProcess(process.copy(isRunning = false))
        }
        activeProcesses = emptyList()
    }

    fun refreshProcesses() {
        activeProcesses = deps.portForwardRepository.getActiveProcesses()
    }

    private fun stopByConfigId(configId: Long) {
        val processes = deps.portForwardRepository.getProcessesForConfig(configId)
        processes.forEach { process ->
            if (process.isRunning) {
                deps.processManager.killProcessByPid(process.pid)
            }
            deps.portForwardRepository.updateProcess(process.copy(isRunning = false))
        }
    }
}
