package com.fgsoft.klusterui.fakes

import com.fgsoft.klusterui.data.Database
import com.fgsoft.klusterui.model.FavoriteNamespace
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess
import com.fgsoft.klusterui.model.SubContext

class FakeDatabase : Database {
    var connected = false
        private set
    var closed = false
        private set

    val contexts = mutableListOf<KubeContext>()
    val configs = mutableListOf<PortForwardConfig>()
    val processes = mutableListOf<PortForwardProcess>()
    val subContexts = mutableListOf<SubContext>()
    val favorites = mutableListOf<FavoriteNamespace>()

    private var nextId: Long = 1

    fun nextId(): Long = nextId++

    override fun connect() {
        connected = true
    }

    override fun close() {
        closed = true
    }

    override fun getAllContexts() = contexts.toList()

    override fun getContext(id: Long) = contexts.find { it.id == id }

    override fun insertContext(context: KubeContext): Long {
        val id = nextId()
        contexts.add(context.copy(id = id))
        return id
    }

    override fun updateContext(context: KubeContext) {
        val idx = contexts.indexOfFirst { it.id == context.id }
        if (idx >= 0) contexts[idx] = context
    }

    override fun deleteContext(id: Long) {
        contexts.removeAll { it.id == id }
        subContexts.removeAll { it.contextId == id }
        favorites.removeAll { it.contextId == id }
        val configIds = configs.filter { it.contextId == id }.map { it.id }.toSet()
        configs.removeAll { it.contextId == id }
        processes.removeAll { it.configId in configIds }
    }

    override fun setActiveContext(id: Long) {
        val idx = contexts.indexOfFirst { it.id == id }
        if (idx >= 0) contexts[idx] = contexts[idx].copy(isActive = true)
    }

    override fun deactivateContext(id: Long) {
        val idx = contexts.indexOfFirst { it.id == id }
        if (idx >= 0) contexts[idx] = contexts[idx].copy(isActive = false)
    }

    override fun getActiveContexts() = contexts.filter { it.isActive }

    override fun getAllPortForwardConfigs() = configs.toList()

    override fun getPortForwardConfigsForContext(contextId: Long) = configs.filter { it.contextId == contextId }

    override fun insertPortForwardConfig(config: PortForwardConfig): Long {
        val id = nextId()
        configs.add(config.copy(id = id))
        return id
    }

    override fun updatePortForwardConfig(config: PortForwardConfig) {
        val idx = configs.indexOfFirst { it.id == config.id }
        if (idx >= 0) configs[idx] = config
    }

    override fun deletePortForwardConfig(id: Long) {
        configs.removeAll { it.id == id }
        processes.removeAll { it.configId == id }
    }

    override fun insertPortForwardProcess(process: PortForwardProcess): Long {
        val id = nextId()
        processes.add(process.copy(id = id))
        return id
    }

    override fun updatePortForwardProcess(process: PortForwardProcess) {
        val idx = processes.indexOfFirst { it.id == process.id }
        if (idx >= 0) processes[idx] = process
    }

    override fun getAllActiveProcesses() = processes.filter { it.isRunning }

    override fun getProcessesForConfig(configId: Long) = processes.filter { it.configId == configId }

    override fun deletePortForwardProcess(id: Long) {
        processes.removeAll { it.id == id }
    }

    override fun getAllSubContexts() = subContexts.toList()

    override fun getSubContexts(contextId: Long) = subContexts.filter { it.contextId == contextId }

    override fun insertSubContext(subContext: SubContext): Long {
        val id = nextId()
        subContexts.add(subContext.copy(id = id))
        return id
    }

    override fun updateSubContext(subContext: SubContext) {
        val idx = subContexts.indexOfFirst { it.id == subContext.id }
        if (idx >= 0) subContexts[idx] = subContext
    }

    override fun deleteSubContext(id: Long) {
        subContexts.removeAll { it.id == id }
    }

    override fun deleteSubContextsForContext(contextId: Long) {
        subContexts.removeAll { it.contextId == contextId }
    }

    override fun getAllFavoriteNamespaces() = favorites.toList()

    override fun getFavoriteNamespaces(contextId: Long) = favorites.filter { it.contextId == contextId }

    override fun insertFavoriteNamespace(fav: FavoriteNamespace): Long {
        if (favorites.any { it.contextId == fav.contextId && it.namespace == fav.namespace }) return -1
        val id = nextId()
        favorites.add(fav.copy(id = id))
        return id
    }

    override fun deleteFavoriteNamespace(
        contextId: Long,
        namespace: String,
    ) {
        favorites.removeAll { it.contextId == contextId && it.namespace == namespace }
    }

    override fun isFavoriteNamespace(
        contextId: Long,
        namespace: String,
    ) = favorites.any { it.contextId == contextId && it.namespace == namespace }
}
