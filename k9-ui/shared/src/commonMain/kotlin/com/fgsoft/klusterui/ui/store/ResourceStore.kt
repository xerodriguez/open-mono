package com.fgsoft.klusterui.ui.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fgsoft.klusterui.AppDependencies
import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.ResourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ResourceStore(
    private val deps: AppDependencies,
    private val scope: CoroutineScope,
) {
    var yaml: String by mutableStateOf("")
        private set
    var events: String by mutableStateOf("")
        private set
    var podLogs: String by mutableStateOf("")
        private set
    var podMetrics: String by mutableStateOf("")
        private set

    fun clear() {
        yaml = ""
        events = ""
        podLogs = ""
        podMetrics = ""
    }

    fun load(
        resource: KubeResource,
        kubectlContext: String,
    ) {
        clear()
        scope.launch {
            try {
                yaml =
                    deps.kubectlClient.getResourceYaml(
                        kubectlContext,
                        resource.namespace,
                        resource.type,
                        resource.name,
                    )
            } catch (_: Exception) {
                yaml = "Error loading YAML"
            }
            try {
                events =
                    deps.kubectlClient.getResourceEvents(
                        kubectlContext,
                        resource.namespace,
                        resource.type,
                        resource.name,
                    )
            } catch (_: Exception) {
                events = "No events found"
            }
            if (resource.type == ResourceType.PODS) {
                try {
                    podLogs =
                        deps.kubectlClient.getPodLogs(
                            kubectlContext,
                            resource.namespace,
                            resource.name,
                        )
                } catch (_: Exception) {
                    podLogs = "Error loading logs"
                }
                try {
                    podMetrics =
                        deps.kubectlClient.getPodMetrics(
                            kubectlContext,
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
