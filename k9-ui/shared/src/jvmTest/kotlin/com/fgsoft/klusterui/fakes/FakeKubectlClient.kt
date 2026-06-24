package com.fgsoft.klusterui.fakes

import com.fgsoft.klusterui.kubectl.KubectlClient
import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.NamespaceInfo
import com.fgsoft.klusterui.model.ResourceType

class FakeKubectlClient : KubectlClient {
    var namespaces: List<NamespaceInfo> = emptyList()
    var resources: List<KubeResource> = emptyList()
    var currentContext: String? = null
    var contexts: List<String> = emptyList()

    val calledMethods = mutableListOf<String>()

    override suspend fun getNamespaces(context: String): List<NamespaceInfo> {
        calledMethods.add("getNamespaces")
        return namespaces
    }

    override suspend fun getResources(
        context: String,
        namespace: String,
        type: ResourceType,
    ): List<KubeResource> {
        calledMethods.add("getResources")
        return resources
    }

    override suspend fun getResourceYaml(
        context: String,
        namespace: String,
        type: ResourceType,
        name: String,
    ): String {
        calledMethods.add("getResourceYaml")
        return "yaml: $name"
    }

    override suspend fun getResourceEvents(
        context: String,
        namespace: String,
        type: ResourceType,
        name: String,
    ): String {
        calledMethods.add("getResourceEvents")
        return "events for $name"
    }

    override suspend fun getPodLogs(
        context: String,
        namespace: String,
        podName: String,
        container: String?,
        tail: Int,
    ): String {
        calledMethods.add("getPodLogs")
        return "logs for $podName"
    }

    override suspend fun getPodMetrics(
        context: String,
        namespace: String,
        podName: String,
    ): String {
        calledMethods.add("getPodMetrics")
        return "metrics for $podName"
    }

    override suspend fun getSecretData(
        context: String,
        namespace: String,
        secretName: String,
    ): String {
        calledMethods.add("getSecretData")
        return "secret: $secretName"
    }

    override suspend fun getNamespacePodLogs(
        context: String,
        namespace: String,
        tail: Int,
    ): String {
        calledMethods.add("getNamespacePodLogs")
        return "namespace logs for $namespace"
    }

    override suspend fun getCurrentContext(): String? = currentContext

    override suspend fun listContexts(): List<String> = contexts
}
