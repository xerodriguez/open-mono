package com.fgsoft.klusterui.kubectl

import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.NamespaceInfo
import com.fgsoft.klusterui.model.ResourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JvmKubectlClient : KubectlClient {
    private val commandPrefix: List<String> = listOf("kubectl")

    private fun buildCommand(
        context: String,
        vararg args: String,
    ): List<String> =
        buildList {
            addAll(commandPrefix)
            if (context.isNotEmpty()) {
                add("--context")
                add(context)
            }
            addAll(args)
        }

    private suspend fun execute(vararg args: String): String =
        withContext(Dispatchers.IO) {
            val process =
                ProcessBuilder(*args)
                    .redirectErrorStream(true)
                    .start()
            process.inputStream.bufferedReader().readText().also {
                process.waitFor()
            }
        }

    private suspend fun executeWithContext(
        context: String,
        vararg args: String,
    ): String = execute(*buildCommand(context, *args).toTypedArray())

    override suspend fun getNamespaces(context: String): List<NamespaceInfo> {
        val output = executeWithContext(context, "get", "namespaces", "-o", "json")
        val json = Json { ignoreUnknownKeys = true }
        val tree = json.parseToJsonElement(output)
        val items = tree.jsonObject["items"]?.jsonArray ?: return emptyList()
        return items.map { item ->
            val metadata = item.jsonObject["metadata"]?.jsonObject
            val status = item.jsonObject["status"]?.jsonObject
            NamespaceInfo(
                name = metadata?.get("name")?.jsonPrimitive?.content ?: "",
                status = status?.get("phase")?.jsonPrimitive?.content ?: "",
                age = metadata?.get("creationTimestamp")?.jsonPrimitive?.content ?: "",
            )
        }
    }

    override suspend fun getResources(
        context: String,
        namespace: String,
        type: ResourceType,
    ): List<KubeResource> {
        val args =
            buildList {
                add("get")
                add(type.kubectlName)
                if (type != ResourceType.NAMESPACES && type != ResourceType.NODES && namespace.isNotEmpty()) {
                    add("-n")
                    add(namespace)
                }
                add("-o")
                add("json")
            }
        val output = executeWithContext(context, *args.toTypedArray())
        val json = Json { ignoreUnknownKeys = true }
        val tree = json.parseToJsonElement(output)
        val items = tree.jsonObject["items"]?.jsonArray ?: return emptyList()
        return items.map { item ->
            val metadata = item.jsonObject["metadata"]?.jsonObject
            val itemStatus = item.jsonObject["status"]?.jsonObject
            KubeResource(
                name = metadata?.get("name")?.jsonPrimitive?.content ?: "",
                namespace = metadata?.get("namespace")?.jsonPrimitive?.content ?: "",
                type = type,
                status =
                    itemStatus?.get("phase")?.jsonPrimitive?.content
                        ?: itemStatus?.get("readyReplicas")?.jsonPrimitive?.content
                        ?: "",
                age = metadata?.get("creationTimestamp")?.jsonPrimitive?.content ?: "",
            )
        }
    }

    override suspend fun getResourceYaml(
        context: String,
        namespace: String,
        type: ResourceType,
        name: String,
    ): String {
        val args =
            buildList {
                add("get")
                add(type.kubectlName)
                add(name)
                if (type != ResourceType.NAMESPACES && type != ResourceType.NODES && namespace.isNotEmpty()) {
                    add("-n")
                    add(namespace)
                }
                add("-o")
                add("yaml")
            }
        return executeWithContext(context, *args.toTypedArray())
    }

    override suspend fun getResourceEvents(
        context: String,
        namespace: String,
        type: ResourceType,
        name: String,
    ): String {
        val args =
            buildList {
                add("get")
                add("events")
                if (namespace.isNotEmpty()) {
                    add("-n")
                    add(namespace)
                }
                add("--field-selector")
                add("involvedObject.name=$name")
                add("-o")
                add("yaml")
            }
        return executeWithContext(context, *args.toTypedArray())
    }

    override suspend fun getPodLogs(
        context: String,
        namespace: String,
        podName: String,
        container: String?,
        tail: Int,
    ): String {
        val args =
            buildList {
                add("logs")
                add(podName)
                if (namespace.isNotEmpty()) {
                    add("-n")
                    add(namespace)
                }
                container?.let {
                    add("-c")
                    add(it)
                }
                add("--tail")
                add(tail.toString())
            }
        return executeWithContext(context, *args.toTypedArray())
    }

    override suspend fun getPodMetrics(
        context: String,
        namespace: String,
        podName: String,
    ): String {
        val args =
            buildList {
                add("top")
                add("pod")
                add(podName)
                if (namespace.isNotEmpty()) {
                    add("-n")
                    add(namespace)
                }
            }
        return executeWithContext(context, *args.toTypedArray())
    }

    override suspend fun getSecretData(
        context: String,
        namespace: String,
        secretName: String,
    ): String {
        val args =
            buildList {
                add("get")
                add("secret")
                add(secretName)
                add("-n")
                add(namespace)
                add("-o")
                add("json")
            }
        return executeWithContext(context, *args.toTypedArray())
    }

    override suspend fun getNamespacePodLogs(
        context: String,
        namespace: String,
        tail: Int,
    ): String {
        val podsOutput = executeWithContext(context, "get", "pods", "-n", namespace, "-o", "json")
        val json = Json { ignoreUnknownKeys = true }
        val tree = json.parseToJsonElement(podsOutput)
        val items = tree.jsonObject["items"]?.jsonArray ?: return "No pods found in namespace $namespace"
        val podNames =
            items.mapNotNull { item ->
                item.jsonObject["metadata"]
                    ?.jsonObject
                    ?.get("name")
                    ?.jsonPrimitive
                    ?.content
            }
        if (podNames.isEmpty()) return "No pods found in namespace $namespace"
        val results = mutableListOf<String>()
        for (podName in podNames) {
            val logText =
                try {
                    executeWithContext(context, "logs", podName, "-n", namespace, "--tail=$tail")
                } catch (_: Exception) {
                    "[Error fetching logs for $podName]"
                }
            results.add("=== $podName ===\n$logText")
        }
        return results.joinToString("\n")
    }

    override suspend fun getCurrentContext(): String? =
        try {
            execute("kubectl", "config", "current-context").trim()
        } catch (_: Exception) {
            null
        }

    override suspend fun listContexts(): List<String> =
        try {
            val output = execute("kubectl", "config", "get-contexts", "-o", "name")
            output.lines().map { it.trim() }.filter { it.isNotEmpty() }
        } catch (_: Exception) {
            emptyList()
        }
}
