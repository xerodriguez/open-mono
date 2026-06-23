package com.fgsoft.klusterui.ui.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fgsoft.klusterui.AppDependencies
import com.fgsoft.klusterui.model.FavoriteNamespace
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.NamespaceInfo
import com.fgsoft.klusterui.model.SubContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ContextStore(
    private val deps: AppDependencies,
    private val scope: CoroutineScope,
) {
    var allContexts: List<KubeContext> by mutableStateOf(emptyList())
        private set
    var activeContexts: List<KubeContext> by mutableStateOf(emptyList())
        private set
    var namespaces: Map<String, List<NamespaceInfo>> by mutableStateOf(emptyMap())
        private set
    var subContextsByContextId: Map<Long, List<SubContext>> by mutableStateOf(emptyMap())
        private set
    var favoritesByContextId: Map<Long, Set<String>> by mutableStateOf(emptyMap())
        private set

    fun load() {
        allContexts = deps.contextRepository.getAll()
        activeContexts = deps.contextRepository.getAllActive()
        loadSubContexts()
        loadFavorites()
        activeContexts.forEach { loadNamespaces(it) }
    }

    private fun loadSubContexts() {
        val all = deps.contextRepository.getAllSubContexts()
        subContextsByContextId = all.groupBy { it.contextId }
    }

    private fun loadFavorites() {
        val all = deps.contextRepository.getAllFavoriteNamespaces()
        favoritesByContextId = all.groupBy({ it.contextId }, { it.namespace }).mapValues { it.value.toSet() }
    }

    fun loadNamespaces(ctx: KubeContext) {
        scope.launch {
            try {
                val nss = deps.kubectlClient.getNamespaces(ctx.context)
                namespaces = namespaces + (ctx.name to nss)
            } catch (_: Exception) {
                namespaces = namespaces + (ctx.name to emptyList())
            }
        }
    }

    fun setNamespaces(
        name: String,
        nss: List<NamespaceInfo>,
    ) {
        namespaces = namespaces + (name to nss)
    }

    fun add(context: KubeContext) {
        deps.contextRepository.create(context)
        load()
    }

    fun add(
        context: KubeContext,
        subDefs: List<Pair<String, String>>,
    ) {
        val id = deps.contextRepository.create(context)
        syncSubContexts(id, subDefs)
        load()
    }

    fun update(context: KubeContext) {
        deps.contextRepository.update(context)
        load()
    }

    fun update(
        context: KubeContext,
        subDefs: List<Pair<String, String>>,
    ) {
        deps.contextRepository.update(context)
        syncSubContexts(context.id, subDefs)
        load()
    }

    fun delete(id: Long) {
        deps.contextRepository.delete(id)
        load()
    }

    fun activate(context: KubeContext) {
        deps.contextRepository.setActive(context.id)
        allContexts = deps.contextRepository.getAll()
        activeContexts = deps.contextRepository.getAllActive()
    }

    fun toggleActive(context: KubeContext) {
        if (context.isActive) {
            deps.contextRepository.deactivate(context.id)
        } else {
            deps.contextRepository.setActive(context.id)
        }
        allContexts = deps.contextRepository.getAll()
        activeContexts = deps.contextRepository.getAllActive()
    }

    fun toggleFavorite(
        contextId: Long,
        namespace: String,
    ) {
        if (isFavorite(contextId, namespace)) {
            deps.contextRepository.removeFavoriteNamespace(contextId, namespace)
        } else {
            deps.contextRepository.addFavoriteNamespace(
                FavoriteNamespace(contextId = contextId, namespace = namespace),
            )
        }
        loadFavorites()
    }

    fun isFavorite(
        contextId: Long,
        namespace: String,
    ): Boolean = favoritesByContextId[contextId]?.contains(namespace) ?: false

    fun favoriteNamespacesFor(contextId: Long): Set<String> = favoritesByContextId[contextId] ?: emptySet()

    private fun syncSubContexts(
        contextId: Long,
        subDefs: List<Pair<String, String>>,
    ) {
        deps.contextRepository.deleteSubContextsForContext(contextId)
        subDefs.forEach { (regex, displayName) ->
            if (regex.isNotBlank() && displayName.isNotBlank()) {
                deps.contextRepository.createSubContext(
                    SubContext(
                        contextId = contextId,
                        regexPattern = regex.trim(),
                        displayName = displayName.trim(),
                    ),
                )
            }
        }
    }
}
