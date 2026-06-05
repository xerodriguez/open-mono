package com.fgsoft.klusterui.data

import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess

class PortForwardRepository(private val database: Database) {

    fun getAllConfigs(): List<PortForwardConfig> = database.getAllPortForwardConfigs()

    fun getConfigsForContext(contextId: Long): List<PortForwardConfig> =
        database.getPortForwardConfigsForContext(contextId)

    fun createConfig(config: PortForwardConfig): Long = database.insertPortForwardConfig(config)

    fun updateConfig(config: PortForwardConfig) = database.updatePortForwardConfig(config)

    fun deleteConfig(id: Long) = database.deletePortForwardConfig(id)

    fun createProcess(process: PortForwardProcess): Long = database.insertPortForwardProcess(process)

    fun updateProcess(process: PortForwardProcess) = database.updatePortForwardProcess(process)

    fun getActiveProcesses(): List<PortForwardProcess> = database.getAllActiveProcesses()

    fun getProcessesForConfig(configId: Long): List<PortForwardProcess> =
        database.getProcessesForConfig(configId)

    fun deleteProcess(id: Long) = database.deletePortForwardProcess(id)
}
