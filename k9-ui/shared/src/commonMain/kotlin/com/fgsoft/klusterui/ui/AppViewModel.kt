package com.fgsoft.klusterui.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fgsoft.klusterui.AppDependencies
import com.fgsoft.klusterui.model.AppView
import com.fgsoft.klusterui.model.FavoriteNamespace
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.NamespaceInfo
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess
import com.fgsoft.klusterui.model.ResourceType
import com.fgsoft.klusterui.model.SubContext
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

    var subContextsByContextId: Map<Long, List<SubContext>> by mutableStateOf(emptyMap())
        private set
    var favoriteNamespacesByContextId: Map<Long, Set<String>> by mutableStateOf(emptyMap())
        private set
    var expandedContexts: Set<Long> by mutableStateOf(emptySet())
    var expandedNamespaces: Set<String> by mutableStateOf(emptySet())
    var expandedSubContexts: Set<String> by mutableStateOf(emptySet())

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

    var logTabs: List<LogTabState> by mutableStateOf(emptyList())
        private set
    var activeLogTabIndex: Int by mutableStateOf(-1)
    var logSearchQuery: String by mutableStateOf("")
    var logSearchMatches: List<Int> by mutableStateOf(emptyList())
        internal set
    var activeLogSearchMatchIndex: Int by mutableStateOf(-1)
    var logFontSize: Float by mutableStateOf(13f)
    var logAutoScroll: Boolean by mutableStateOf(true)
    var logWrapText: Boolean by mutableStateOf(true)
    var logHighlightLevel: Boolean by mutableStateOf(true)

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
        loadSubContexts()
        loadFavorites()
        activeContexts.forEach { loadContextNamespaces(it) }
        loadPortForwardConfigs()
        refreshActiveProcesses()
    }

    private fun loadSubContexts() {
        val all = deps.contextRepository.getAllSubContexts()
        subContextsByContextId = all.groupBy { it.contextId }
    }

    private fun loadFavorites() {
        val all = deps.contextRepository.getAllFavoriteNamespaces()
        favoriteNamespacesByContextId = all.groupBy({ it.contextId }, { it.namespace }).mapValues { it.value.toSet() }
    }

    fun toggleFavorite(
        contextId: Long,
        namespace: String,
    ) {
        if (isFavorite(contextId, namespace)) {
            deps.contextRepository.removeFavoriteNamespace(contextId, namespace)
        } else {
            deps.contextRepository.addFavoriteNamespace(FavoriteNamespace(contextId = contextId, namespace = namespace))
        }
        loadFavorites()
    }

    fun isFavorite(
        contextId: Long,
        namespace: String,
    ): Boolean = favoriteNamespacesByContextId[contextId]?.contains(namespace) ?: false

    fun getFavoriteNamespacesForContext(contextId: Long): Set<String> = favoriteNamespacesByContextId[contextId] ?: emptySet()

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

    fun toggleSubContextExpanded(
        contextId: Long,
        subContextId: Long,
    ) {
        val key = "$contextId/$subContextId"
        expandedSubContexts =
            if (key in expandedSubContexts) {
                expandedSubContexts - key
            } else {
                expandedSubContexts + key
            }
    }

    fun groupNamespacesBySubContext(
        contextId: Long,
        namespaces: List<NamespaceInfo>,
    ): Map<Long?, List<NamespaceInfo>> {
        val subContexts = subContextsByContextId[contextId] ?: return mapOf(null to namespaces)
        val result = mutableMapOf<Long?, MutableList<NamespaceInfo>>()
        val matchedNs = mutableSetOf<String>()

        subContexts.forEach { sc ->
            val matched =
                namespaces.filter { ns ->
                    ns.name !in matchedNs &&
                        try {
                            Regex(sc.regexPattern).matches(ns.name)
                        } catch (_: Exception) {
                            false
                        }
                }
            if (matched.isNotEmpty()) {
                result[sc.id] = matched.toMutableList()
                matchedNs.addAll(matched.map { it.name })
            }
        }

        val unmatched = namespaces.filterNot { it.name in matchedNs }
        if (unmatched.isNotEmpty()) {
            result[null] = unmatched.toMutableList()
        }

        return result
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
        val id = deps.contextRepository.create(context)
        loadContexts()
    }

    fun addContext(
        context: KubeContext,
        subDefs: List<Pair<String, String>>,
    ) {
        val id = deps.contextRepository.create(context)
        syncSubContexts(id, subDefs)
        loadContexts()
    }

    fun updateContext(context: KubeContext) {
        deps.contextRepository.update(context)
        loadContexts()
    }

    fun updateContext(
        context: KubeContext,
        subDefs: List<Pair<String, String>>,
    ) {
        deps.contextRepository.update(context)
        syncSubContexts(context.id, subDefs)
        loadContexts()
    }

    fun deleteContext(id: Long) {
        deps.contextRepository.delete(id)
        loadContexts()
    }

    private fun syncSubContexts(
        contextId: Long,
        subDefs: List<Pair<String, String>>,
    ) {
        deps.contextRepository.deleteSubContextsForContext(contextId)
        subDefs.forEach { (regex, displayName) ->
            if (regex.isNotBlank() && displayName.isNotBlank()) {
                deps.contextRepository.createSubContext(
                    SubContext(contextId = contextId, regexPattern = regex.trim(), displayName = displayName.trim()),
                )
            }
        }
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

    fun openLogsTab(
        contextName: String,
        kubectlContext: String,
        namespace: String,
    ) {
        val existingIndex = logTabs.indexOfFirst { it.kubectlContext == kubectlContext && it.namespace == namespace }
        if (existingIndex >= 0) {
            activeLogTabIndex = existingIndex
            return
        }
        val tab = LogTabState(contextName = contextName, kubectlContext = kubectlContext, namespace = namespace)
        logTabs = logTabs + tab
        activeLogTabIndex = logTabs.size - 1
        loadLogsForTab(activeLogTabIndex)
    }

    fun closeLogsTab(index: Int) {
        if (index < 0 || index >= logTabs.size) return
        logTabs = logTabs.toMutableList().also { it.removeAt(index) }
        activeLogTabIndex =
            if (logTabs.isEmpty()) {
                -1
            } else if (index >= logTabs.size) {
                logTabs.size - 1
            } else {
                index.coerceIn(0, logTabs.size - 1)
            }
    }

    fun setActiveLogTab(index: Int) {
        activeLogTabIndex = index.coerceIn(0, logTabs.size - 1)
        if (logTabs.getOrNull(activeLogTabIndex)?.logContent?.isEmpty() != false) {
            loadLogsForTab(activeLogTabIndex)
        }
    }

    fun refreshActiveLogTab() {
        val idx = activeLogTabIndex
        if (idx >= 0 && idx < logTabs.size) {
            loadLogsForTab(idx)
        }
    }

    private fun loadLogsForTab(index: Int) {
        val tab = logTabs.getOrNull(index) ?: return
        if (tab.isLoading) return
        logTabs = logTabs.toMutableList().also { it[index] = tab.copy(isLoading = true) }
        scope.launch {
            try {
                val content = deps.kubectlClient.getNamespacePodLogs(tab.kubectlContext, tab.namespace)
                logTabs = logTabs.toMutableList().also { it[index] = tab.copy(logContent = content, isLoading = false) }
                if (index == activeLogTabIndex) {
                    updateLogSearchMatches()
                }
            } catch (_: Exception) {
                logTabs =
                    logTabs.toMutableList().also {
                        it[index] = tab.copy(logContent = "Error loading logs", isLoading = false)
                    }
            }
        }
    }

    fun updateLogSearchMatches() {
        val query = logSearchQuery
        val content = logTabs.getOrNull(activeLogTabIndex)?.logContent ?: ""
        if (query.isBlank()) {
            logSearchMatches = emptyList()
            activeLogSearchMatchIndex = -1
            return
        }
        val matches = mutableListOf<Int>()
        var startIndex = 0
        while (true) {
            val idx = content.indexOf(query, startIndex, ignoreCase = true)
            if (idx < 0) break
            matches.add(idx)
            startIndex = idx + 1
        }
        logSearchMatches = matches
        activeLogSearchMatchIndex = if (matches.isNotEmpty()) 0 else -1
    }

    fun searchLogsNext() {
        if (logSearchMatches.isEmpty()) {
            updateLogSearchMatches()
            return
        }
        activeLogSearchMatchIndex = (activeLogSearchMatchIndex + 1) % logSearchMatches.size
    }

    fun searchLogsPrev() {
        if (logSearchMatches.isEmpty()) {
            updateLogSearchMatches()
            return
        }
        activeLogSearchMatchIndex =
            if (activeLogSearchMatchIndex - 1 < 0) {
                logSearchMatches.size - 1
            } else {
                activeLogSearchMatchIndex - 1
            }
    }
}

data class LogTabState(
    val contextName: String,
    val kubectlContext: String,
    val namespace: String,
    val logContent: String = "",
    val isLoading: Boolean = false,
)
