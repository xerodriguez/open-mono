package com.fgsoft.klusterui.data

import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess

interface Database {
    fun connect()

    fun close()

    fun getAllContexts(): List<KubeContext>

    fun getContext(id: Long): KubeContext?

    fun insertContext(context: KubeContext): Long

    fun updateContext(context: KubeContext)

    fun deleteContext(id: Long)

    fun setActiveContext(id: Long)

    fun deactivateContext(id: Long)

    fun getActiveContexts(): List<KubeContext>

    fun getAllPortForwardConfigs(): List<PortForwardConfig>

    fun getPortForwardConfigsForContext(contextId: Long): List<PortForwardConfig>

    fun insertPortForwardConfig(config: PortForwardConfig): Long

    fun updatePortForwardConfig(config: PortForwardConfig)

    fun deletePortForwardConfig(id: Long)

    fun insertPortForwardProcess(process: PortForwardProcess): Long

    fun updatePortForwardProcess(process: PortForwardProcess)

    fun getAllActiveProcesses(): List<PortForwardProcess>

    fun getProcessesForConfig(configId: Long): List<PortForwardProcess>

    fun deletePortForwardProcess(id: Long)
}
