package com.fgsoft.klusterui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ResourceType(val label: String, val kubectlName: String) {
    PODS("Pods", "pods"),
    SERVICES("Services", "services"),
    DEPLOYMENTS("Deployments", "deployments"),
    STATEFULSETS("StatefulSets", "statefulsets"),
    DAEMONSETS("DaemonSets", "daemonsets"),
    JOBS("Jobs", "jobs"),
    CONFIGMAPS("ConfigMaps", "configmaps"),
    SECRETS("Secrets", "secrets"),
    NAMESPACES("Namespaces", "namespaces"),
    NODES("Nodes", "nodes"),
    INGRESSES("Ingresses", "ingresses"),
    CRONJOBS("CronJobs", "cronjobs"),
}

@Serializable
data class KubeResource(
    val name: String,
    val namespace: String,
    val type: ResourceType,
    val status: String = "",
    val age: String = "",
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class NamespaceInfo(
    val name: String,
    val status: String = "",
    val age: String = "",
)
