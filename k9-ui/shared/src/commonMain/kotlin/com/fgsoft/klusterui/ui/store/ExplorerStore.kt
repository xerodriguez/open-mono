package com.fgsoft.klusterui.ui.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fgsoft.klusterui.AppDependencies
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.NamespaceInfo
import com.fgsoft.klusterui.model.ResourceType
import com.fgsoft.klusterui.model.SubContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ExplorerStore(
    private val deps: AppDependencies,
    private val scope: CoroutineScope,
) {
    var expandedContexts: Set<Long> by mutableStateOf(emptySet())
    var expandedNamespaces: Set<String> by mutableStateOf(emptySet())
    var expandedSubContexts: Set<String> by mutableStateOf(emptySet())

    var resourcesByKey: Map<String, Map<ResourceType, List<KubeResource>>> by mutableStateOf(emptyMap())
        private set

    var selectedNamespace: String by mutableStateOf("")
    var selectedResourceType: ResourceType by mutableStateOf(ResourceType.PODS)
    var resources: List<KubeResource> by mutableStateOf(emptyList())
        private set
    var selectedResource: KubeResource? by mutableStateOf(null)

    fun toggleContext(contextId: Long) {
        expandedContexts =
            if (contextId in expandedContexts) {
                expandedContexts - contextId
            } else {
                expandedContexts + contextId
            }
    }

    fun expandContext(contextId: Long) {
        expandedContexts = expandedContexts + contextId
    }

    fun collapseContext(contextId: Long) {
        expandedContexts = expandedContexts - contextId
    }

    fun toggleNamespace(
        contextName: String,
        namespaceName: String,
    ) {
        val key = "$contextName/$namespaceName"
        expandedNamespaces =
            if (key in expandedNamespaces) {
                expandedNamespaces - key
            } else {
                expandedNamespaces + key
            }
    }

    fun toggleSubContext(
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
        subContexts: List<SubContext>,
        namespaces: List<NamespaceInfo>,
    ): Map<Long?, List<NamespaceInfo>> {
        if (subContexts.isEmpty()) return mapOf(null to namespaces)

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

    fun expandedNamespacesFor(ctxName: String): List<String> =
        expandedNamespaces.filter { it.startsWith("$ctxName/") }.map { it.substringAfter("$ctxName/") }

    fun selectNamespace(namespace: String) {
        selectedNamespace = namespace
    }

    fun selectResourceType(type: ResourceType) {
        selectedResourceType = type
    }

    fun selectResource(resource: KubeResource?) {
        selectedResource = resource
        if (resource != null) {
            selectedNamespace = resource.namespace
            selectedResourceType = resource.type
        }
    }

    fun loadResources(
        ctx: KubeContext,
        namespace: String,
        type: ResourceType,
    ) {
        scope.launch {
            try {
                resources = deps.kubectlClient.getResources(ctx.context, namespace, type)
            } catch (_: Exception) {
                resources = emptyList()
            }
        }
    }

    fun refreshResources(
        ctx: KubeContext,
        namespace: String,
    ) {
        scope.launch {
            try {
                val allResources =
                    ResourceType.entries.flatMap { type ->
                        deps.kubectlClient.getResources(ctx.context, namespace, type).map { it to type }
                    }
                val byType =
                    allResources
                        .groupBy({ it.second }, { it.first })
                        .mapValues { entry -> entry.value.sortedBy { r -> r.name.lowercase() } }
                val key = "${ctx.name}/$namespace"
                resourcesByKey = resourcesByKey + (key to byType)

                if (selectedResource != null && selectedResource?.namespace == namespace) {
                    resources = allResources.map { it.first }
                }
            } catch (_: Exception) {
                val key = "${ctx.name}/$namespace"
                resourcesByKey = resourcesByKey + (key to emptyMap())
            }
        }
    }

    fun resourcesByTypeDiffer(
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
}
