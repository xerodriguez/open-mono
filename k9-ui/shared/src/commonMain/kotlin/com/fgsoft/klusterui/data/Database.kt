package com.fgsoft.klusterui.data

import com.fgsoft.klusterui.model.FavoriteNamespace
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess
import com.fgsoft.klusterui.model.SubContext

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

    fun getAllSubContexts(): List<SubContext>

    fun getSubContexts(contextId: Long): List<SubContext>

    fun insertSubContext(subContext: SubContext): Long

    fun updateSubContext(subContext: SubContext)

    fun deleteSubContext(id: Long)

    fun deleteSubContextsForContext(contextId: Long)

    fun getAllFavoriteNamespaces(): List<FavoriteNamespace>

    fun getFavoriteNamespaces(contextId: Long): List<FavoriteNamespace>

    fun insertFavoriteNamespace(fav: FavoriteNamespace): Long

    fun deleteFavoriteNamespace(
        contextId: Long,
        namespace: String,
    )

    fun isFavoriteNamespace(
        contextId: Long,
        namespace: String,
    ): Boolean
}
