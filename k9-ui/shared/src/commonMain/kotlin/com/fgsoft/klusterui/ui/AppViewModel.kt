package com.fgsoft.klusterui.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fgsoft.klusterui.AppDependencies
import com.fgsoft.klusterui.model.AppView
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.NamespaceInfo
import com.fgsoft.klusterui.model.ResourceType
import com.fgsoft.klusterui.model.currentTimeMillis
import com.fgsoft.klusterui.ui.store.ContextStore
import com.fgsoft.klusterui.ui.store.ExplorerStore
import com.fgsoft.klusterui.ui.store.LogsStore
import com.fgsoft.klusterui.ui.store.PortForwardStore
import com.fgsoft.klusterui.ui.store.ResourceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppViewModel(
    private val deps: AppDependencies,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val contexts = ContextStore(deps, scope)
    val explorer = ExplorerStore(deps, scope)
    val portForward = PortForwardStore(deps, scope)
    val resource = ResourceStore(deps, scope)
    val logs = LogsStore(deps, scope)

    // ── Shared state ──

    var currentView: AppView by mutableStateOf(AppView.TREE_VIEW)
    var searchQuery: String by mutableStateOf("")
    var isDarkTheme: Boolean by mutableStateOf(false)
    var showDeleteContextDialog: KubeContext? by mutableStateOf(null)
    var showPortForwardDialog: KubeResource? by mutableStateOf(null)

    val activeContext: KubeContext?
        get() = contexts.activeContexts.firstOrNull()

    // ── Search helpers ──

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
                explorer.resources
            } else {
                explorer.resources.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.namespace.contains(searchQuery, ignoreCase = true) ||
                        it.status.contains(searchQuery, ignoreCase = true)
                }
            }

    // ── Cross-store orchestration ──

    fun loadContexts() {
        contexts.load()
        val ctxId = activeContext?.id
        if (ctxId != null) {
            portForward.loadConfigs(ctxId)
        }
        portForward.refreshProcesses()
    }

    fun activateContext(context: KubeContext) {
        contexts.activate(context)
        loadContexts()
        explorer.expandContext(context.id)
    }

    fun toggleContextActive(context: KubeContext) {
        contexts.toggleActive(context)
        if (!context.isActive) {
            explorer.collapseContext(context.id)
        }
    }

    fun deleteContext(id: Long) {
        contexts.delete(id)
    }

    fun toggleFavorite(
        contextId: Long,
        namespace: String,
    ) {
        contexts.toggleFavorite(contextId, namespace)
    }

    fun toggleNamespaceExpanded(
        contextName: String,
        namespaceName: String,
    ) {
        val wasExpanded = "$contextName/$namespaceName" in explorer.expandedNamespaces
        explorer.toggleNamespace(contextName, namespaceName)
        if (!wasExpanded) {
            val ctx = contexts.activeContexts.find { it.name == contextName } ?: return
            explorer.refreshResources(ctx, namespaceName)
        }
    }

    fun deletePortForwardConfig(id: Long) {
        portForward.deleteConfig(id)
        val ctxId = activeContext?.id ?: return
        portForward.loadConfigs(ctxId)
    }

    fun selectNamespace(namespace: String) {
        explorer.selectNamespace(namespace)
        val ctx = activeContext ?: return
        explorer.loadResources(ctx, namespace, explorer.selectedResourceType)
    }

    fun selectResourceType(type: ResourceType) {
        explorer.selectResourceType(type)
        val ctx = activeContext ?: return
        explorer.loadResources(ctx, explorer.selectedNamespace, type)
    }

    fun selectResourceAndNamespace(resource: KubeResource) {
        explorer.selectResource(resource)
        val ctx = activeContext ?: return
        this.resource.load(resource, ctx.context)
    }

    fun selectResource(res: KubeResource?) {
        explorer.selectResource(res)
        if (res != null) {
            val ctx = activeContext ?: return
            resource.load(res, ctx.context)
        }
    }

    fun openLogsTab(
        contextName: String,
        kubectlContext: String,
        namespace: String,
    ) {
        logs.open(contextName, kubectlContext, namespace)
        currentView = AppView.LOGS
    }

    fun startPortForward(
        config: com.fgsoft.klusterui.model.PortForwardConfig,
        podName: String,
    ) {
        val ctx = activeContext ?: return
        portForward.start(config, podName, ctx.context)
    }

    fun findAvailableLocalPort(
        contextId: Long,
        desiredPort: Int,
    ): Int {
        val usedPorts =
            (
                portForward.configs.map { it.localPort } +
                    portForward.activeProcesses.map { it.localPort }
            ).toSet()
        var port = desiredPort
        while (port in usedPorts) port++
        return port
    }

    internal fun checkPortForwardTimeouts() {
        val now = currentTimeMillis()
        portForward.activeProcesses.forEach { process ->
            val config = portForward.configs.find { it.id == process.configId }
            val timeout = config?.timeoutMinutes ?: return@forEach
            val elapsed = (now - process.startedAt) / 60_000
            if (elapsed >= timeout) {
                portForward.stop(process.id)
            }
        }
    }

    init {
        loadContexts()
        startRefreshTimer()
    }

    // ── Periodic refresh ──

    private fun startRefreshTimer() {
        scope.launch {
            while (true) {
                delay(10_000)
                refreshActiveContextData()
                checkPortForwardTimeouts()
            }
        }
    }

    private fun refreshActiveContextData() {
        contexts.activeContexts.forEach { ctx ->
            scope.launch {
                try {
                    val nss = deps.kubectlClient.getNamespaces(ctx.context)
                    val oldNss = contexts.namespaces[ctx.name]
                    if (oldNss == null || namespacesDiffer(oldNss, nss)) {
                        contexts.setNamespaces(ctx.name, nss)
                    }
                    explorer.expandedNamespacesFor(ctx.name).forEach { ns ->
                        if (ns in nss.map { it.name }) {
                            explorer.refreshResources(ctx, ns)
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
}
