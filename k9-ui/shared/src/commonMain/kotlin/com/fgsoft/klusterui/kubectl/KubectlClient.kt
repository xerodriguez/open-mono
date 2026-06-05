package com.fgsoft.klusterui.kubectl

import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.NamespaceInfo
import com.fgsoft.klusterui.model.ResourceType

interface KubectlClient {
    suspend fun getNamespaces(context: String): List<NamespaceInfo>
    suspend fun getResources(context: String, namespace: String, type: ResourceType): List<KubeResource>
    suspend fun getResourceYaml(context: String, namespace: String, type: ResourceType, name: String): String
    suspend fun getResourceEvents(context: String, namespace: String, type: ResourceType, name: String): String
    suspend fun getPodLogs(context: String, namespace: String, podName: String, container: String? = null, tail: Int = 100): String
    suspend fun getPodMetrics(context: String, namespace: String, podName: String): String
    suspend fun getSecretData(context: String, namespace: String, secretName: String): String
    suspend fun getCurrentContext(): String?
    suspend fun listContexts(): List<String>
}
