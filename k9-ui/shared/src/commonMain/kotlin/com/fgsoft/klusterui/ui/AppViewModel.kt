package com.fgsoft.klusterui.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fgsoft.klusterui.AppDependencies
import com.fgsoft.klusterui.model.AppView
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.NamespaceInfo
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess
import com.fgsoft.klusterui.model.ResourceType
import com.fgsoft.klusterui.model.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppViewModel(
    private val deps: AppDependencies,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var currentView: AppView by mutableStateOf(AppView.TREE_VIEW)

    var contexts: List<KubeContext> by mutableStateOf(emptyList())
        private set
    var activeContexts: List<KubeContext> by mutableStateOf(emptyList())
        private set

    val activeContext: KubeContext?
        get() = activeContexts.firstOrNull()

    var contextNamespaces: Map<String, List<NamespaceInfo>> by mutableStateOf(emptyMap())
        private set

    var contextResources: Map<String, Map<ResourceType, List<KubeResource>>> by mutableStateOf(emptyMap())
        private set

    var expandedContexts: Set<Long> by mutableStateOf(emptySet())
    var expandedNamespaces: Set<String> by mutableStateOf(emptySet())

    var selectedNamespace: String by mutableStateOf("")
    var selectedResourceType: ResourceType by mutableStateOf(ResourceType.PODS)
    var resources: List<KubeResource> by mutableStateOf(emptyList())
        private set
    var selectedResource: KubeResource? by mutableStateOf(null)

    var portForwardConfigs: List<PortForwardConfig> by mutableStateOf(emptyList())
        private set
    var activePortForwardProcesses: List<PortForwardProcess> by mutableStateOf(emptyList())
        private set

    var searchQuery: String by mutableStateOf("")
    var isDarkTheme: Boolean by mutableStateOf(false)

    var resourceYaml: String by mutableStateOf("")
        private set
    var resourceEvents: String by mutableStateOf("")
        private set
    var podLogs: String by mutableStateOf("")
        private set
    var podMetrics: String by mutableStateOf("")
        private set

    var showDeleteContextDialog: KubeContext? by mutableStateOf(null)
    var showPortForwardDialog: KubeResource? by mutableStateOf(null)

    init {
        loadContexts()
        startRefreshTimer()
    }

    private fun startRefreshTimer() {
        scope.launch {
            while (true) {
                delay(10_000)
                refreshActiveContextData()
            }
        }
    }

    private fun refreshActiveContextData() {
        activeContexts.forEach { ctx ->
            scope.launch {
                try {
                    val nss = deps.kubectlClient.getNamespaces(ctx.context)
                    val oldNss = contextNamespaces[ctx.name]
                    if (oldNss == null || namespacesDiffer(oldNss, nss)) {
                        contextNamespaces = contextNamespaces + (ctx.name to nss)
                    }
                    expandedNamespaces.filter { it.startsWith(ctx.name + "/") }.forEach { key ->
                        val ns = key.substringAfter(ctx.name + "/")
                        if (ns in nss.map { it.name }) {
                            refreshContextResources(ctx, ns)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun namespacesDiffer(
        a: List<NamespaceInfo>,
        b: List<NamespaceInfo>,
    ): Boolean {
        if (a.size != b.size) return true
        return a.map { it.name }.toSet() != b.map { it.name }.toSet()
    }

    private suspend fun refreshContextResources(
        ctx: KubeContext,
        namespace: String,
    ) {
        try {
            val allResources =
                ResourceType.entries.flatMap { type ->
                    deps.kubectlClient.getResources(ctx.context, namespace, type).map { it to type }
                }
            val byType =
                allResources
                    .groupBy({ it.second }, { it.first })
                    .mapValues { it.value.sortedBy { r -> r.name.lowercase() } }
            val key = "${ctx.name}/$namespace"
            val old = contextResources[key]
            if (old == null || resourcesByTypeDiffer(old, byType)) {
                contextResources = contextResources + (key to byType)
            }
        } catch (_: Exception) {
        }
    }

    private fun resourcesByTypeDiffer(
        a: Map<ResourceType, List<KubeResource>>,
        b: Map<ResourceType, List<KubeResource>>,
    ): Boolean {
        if (a.size != b.size) return true
        for ((type, resList) in b) {
            val oldList = a[type] ?: return true
            if (oldList.size != resList.size) return true
            if (resList.zip(oldList).any { (n, o) -> n != o }) return true
        }
        return false
    }

    fun loadContexts() {
        contexts = deps.contextRepository.getAll()
        activeContexts = deps.contextRepository.getAllActive()
        activeContexts.forEach { loadContextNamespaces(it) }
        loadPortForwardConfigs()
        refreshActiveProcesses()
    }

    fun activateContext(context: KubeContext) {
        deps.contextRepository.setActive(context.id)
        contexts = deps.contextRepository.getAll()
        activeContexts = deps.contextRepository.getAllActive()
        loadContextNamespaces(context)
        loadPortForwardConfigs()
        refreshActiveProcesses()
        expandedContexts = expandedContexts + context.id
    }

    fun toggleContextActive(context: KubeContext) {
        if (context.isActive) {
            deps.contextRepository.deactivate(context.id)
        } else {
            deps.contextRepository.setActive(context.id)
        }
        contexts = deps.contextRepository.getAll()
        activeContexts = deps.contextRepository.getAllActive()
        if (!context.isActive) {
            expandedContexts = expandedContexts - context.id
        }
    }

    fun toggleContextExpanded(contextId: Long) {
        expandedContexts =
            if (contextId in expandedContexts) {
                expandedContexts - contextId
            } else {
                expandedContexts + contextId
            }
    }

    fun toggleNamespaceExpanded(
        contextName: String,
        namespaceName: String,
    ) {
        val key = "$contextName/$namespaceName"
        val ctx = activeContexts.find { it.name == contextName } ?: return
        expandedNamespaces =
            if (key in expandedNamespaces) {
                expandedNamespaces - key
            } else {
                expandedNamespaces + key
            }
        if (key in expandedNamespaces) {
            scope.launch {
                loadContextResources(ctx, namespaceName)
            }
        }
    }

    fun selectResourceAndNamespace(resource: KubeResource) {
        selectedResource = resource
        selectedNamespace = resource.namespace
        selectedResourceType = resource.type
        resourceYaml = ""
        resourceEvents = ""
        podLogs = ""
        podMetrics = ""

        val ctx = activeContext ?: return
        scope.launch {
            try {
                resourceYaml =
                    deps.kubectlClient.getResourceYaml(
                        ctx.context,
                        resource.namespace,
                        resource.type,
                        resource.name,
                    )
            } catch (_: Exception) {
                resourceYaml = "Error loading YAML"
            }
            try {
                resourceEvents =
                    deps.kubectlClient.getResourceEvents(
                        ctx.context,
                        resource.namespace,
                        resource.type,
                        resource.name,
                    )
            } catch (_: Exception) {
                resourceEvents = "No events found"
            }
            if (resource.type == ResourceType.PODS) {
                try {
                    podLogs =
                        deps.kubectlClient.getPodLogs(
                            ctx.context,
                            resource.namespace,
                            resource.name,
                        )
                } catch (_: Exception) {
                    podLogs = "Error loading logs"
                }
                try {
                    podMetrics =
                        deps.kubectlClient.getPodMetrics(
                            ctx.context,
                            resource.namespace,
                            resource.name,
                        )
                } catch (_: Exception) {
                    podMetrics = "No metrics available"
                }
            }
        }
    }

    fun addContext(context: KubeContext) {
        deps.contextRepository.create(context)
        loadContexts()
    }

    fun updateContext(context: KubeContext) {
        deps.contextRepository.update(context)
        loadContexts()
    }

    fun deleteContext(id: Long) {
        deps.contextRepository.delete(id)
        loadContexts()
    }

    private fun loadContextNamespaces(ctx: KubeContext) {
        scope.launch {
            try {
                val nss = deps.kubectlClient.getNamespaces(ctx.context)
                contextNamespaces = contextNamespaces + (ctx.name to nss)
            } catch (_: Exception) {
                contextNamespaces = contextNamespaces + (ctx.name to emptyList())
            }
        }
    }

    private suspend fun loadContextResources(
        ctx: KubeContext,
        namespace: String,
    ) {
        try {
            val allResources =
                ResourceType.entries.flatMap { type ->
                    deps.kubectlClient.getResources(ctx.context, namespace, type).map { it to type }
                }
            val byType =
                allResources
                    .groupBy({ it.second }, { it.first })
                    .mapValues { entry ->
                        entry.value.sortedBy { it.name.lowercase() }
                    }
            val key = "${ctx.name}/$namespace"
            contextResources = contextResources + (key to byType)

            if (selectedResource != null && selectedResource?.namespace == namespace) {
                resources = allResources.map { it.first }
            }
        } catch (_: Exception) {
            val key = "${ctx.name}/$namespace"
            contextResources = contextResources + (key to emptyMap<ResourceType, List<KubeResource>>())
        }
    }

    fun selectNamespace(namespace: String) {
        selectedNamespace = namespace
        activeContext?.let { ctx ->
            scope.launch {
                try {
                    loadResources(ctx.context, namespace, selectedResourceType)
                } catch (_: Exception) {
                    resources = emptyList()
                }
            }
        }
    }

    fun selectResourceType(type: ResourceType) {
        selectedResourceType = type
        activeContext?.let { ctx ->
            scope.launch {
                try {
                    loadResources(ctx.context, selectedNamespace, type)
                } catch (_: Exception) {
                    resources = emptyList()
                }
            }
        }
    }

    private suspend fun loadResources(
        contextName: String,
        namespace: String,
        type: ResourceType,
    ) {
        try {
            resources = deps.kubectlClient.getResources(contextName, namespace, type)
        } catch (_: Exception) {
            resources = emptyList()
        }
    }

    fun selectResource(resource: KubeResource?) {
        selectedResource = resource
        resourceYaml = ""
        resourceEvents = ""
        podLogs = ""
        podMetrics = ""

        if (resource != null && activeContext != null) {
            scope.launch {
                try {
                    resourceYaml =
                        deps.kubectlClient.getResourceYaml(
                            activeContext!!.context,
                            resource.namespace,
                            resource.type,
                            resource.name,
                        )
                } catch (_: Exception) {
                    resourceYaml = "Error loading YAML"
                }
                try {
                    resourceEvents =
                        deps.kubectlClient.getResourceEvents(
                            activeContext!!.context,
                            resource.namespace,
                            resource.type,
                            resource.name,
                        )
                } catch (_: Exception) {
                    resourceEvents = "No events found"
                }
                if (resource.type == ResourceType.PODS) {
                    try {
                        podLogs =
                            deps.kubectlClient.getPodLogs(
                                activeContext!!.context,
                                resource.namespace,
                                resource.name,
                            )
                    } catch (_: Exception) {
                        podLogs = "Error loading logs"
                    }
                    try {
                        podMetrics =
                            deps.kubectlClient.getPodMetrics(
                                activeContext!!.context,
                                resource.namespace,
                                resource.name,
                            )
                    } catch (_: Exception) {
                        podMetrics = "No metrics available"
                    }
                }
            }
        }
    }

    private fun loadPortForwardConfigs() {
        val ctxId = activeContext?.id ?: return
        portForwardConfigs = deps.portForwardRepository.getConfigsForContext(ctxId)
    }

    fun addPortForwardConfig(config: PortForwardConfig): Long {
        val id = deps.portForwardRepository.createConfig(config)
        loadPortForwardConfigs()
        return id
    }

    fun updatePortForwardConfig(config: PortForwardConfig) {
        deps.portForwardRepository.updateConfig(config)
        loadPortForwardConfigs()
    }

    fun deletePortForwardConfig(id: Long) {
        stopPortForwardByConfigId(id)
        deps.portForwardRepository.deleteConfig(id)
        loadPortForwardConfigs()
        refreshActiveProcesses()
    }

    fun startPortForward(
        config: PortForwardConfig,
        podName: String,
    ) {
        val ctx = activeContext ?: return
        val handle =
            deps.processManager.startPortForward(
                context = ctx.context,
                namespace = config.namespace,
                resourceType = config.resourceType,
                resourceName = config.resourceName,
                localPort = config.localPort,
                remotePort = config.remotePort,
                onOutput = { /* log output */ },
                onError = { /* log error */ },
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
        refreshActiveProcesses()
    }

    fun stopPortForward(processId: Long) {
        val process = activePortForwardProcesses.find { it.id == processId } ?: return
        deps.processManager.killProcessByPid(process.pid)
        val updated = process.copy(isRunning = false)
        deps.portForwardRepository.updateProcess(updated)
        refreshActiveProcesses()
    }

    private fun stopPortForwardByConfigId(configId: Long) {
        val processes = deps.portForwardRepository.getProcessesForConfig(configId)
        processes.forEach { process ->
            if (process.isRunning) {
                deps.processManager.killProcessByPid(process.pid)
            }
            deps.portForwardRepository.updateProcess(process.copy(isRunning = false))
        }
    }

    fun refreshActiveProcesses() {
        activePortForwardProcesses = deps.portForwardRepository.getActiveProcesses()
    }

    fun killAllPortForwards() {
        activePortForwardProcesses.forEach { process ->
            deps.processManager.killProcessByPid(process.pid)
            deps.portForwardRepository.updateProcess(process.copy(isRunning = false))
        }
        activePortForwardProcesses = emptyList()
    }

    fun treeMatchesSearch(
        name: String,
        vararg extras: String,
    ): Boolean {
        if (searchQuery.isBlank()) return true
        return name.contains(searchQuery, ignoreCase = true) ||
            extras.any { it.contains(searchQuery, ignoreCase = true) }
    }

    val filteredResources: List<KubeResource>
        get() =
            if (searchQuery.isBlank()) {
                resources
            } else {
                resources.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.namespace.contains(searchQuery, ignoreCase = true) ||
                        it.status.contains(searchQuery, ignoreCase = true)
                }
            }
}
