package com.fgsoft.klusterui.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess
import com.fgsoft.klusterui.model.ResourceType
import com.fgsoft.klusterui.model.formatTimestamp
import com.fgsoft.klusterui.ui.AppViewModel

@Composable
fun PortForwardPage(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    var editConfig by remember { mutableStateOf<PortForwardConfig?>(null) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Port Forwarding",
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (viewModel.portForward.activeProcesses.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.portForward.killAll() },
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text("Stop All")
                    }
                }
                Button(onClick = { showDialog = true }) {
                    Text("+ Add Forward")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (viewModel.activeContext == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Select an active context to manage port forwarding",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        Text("Active Processes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(0.3f),
        ) {
            if (viewModel.portForward.activeProcesses.isEmpty()) {
                item {
                    Text(
                        "No active port forwards",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(viewModel.portForward.activeProcesses) { process ->
                ActiveProcessCard(
                    process = process,
                    onStop = { viewModel.portForward.stop(process.id) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Saved Configurations", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(0.7f),
        ) {
            if (viewModel.portForward.configs.isEmpty()) {
                item {
                    Text(
                        "No saved configurations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(viewModel.portForward.configs) { config ->
                val isRunning = viewModel.portForward.activeProcesses.any { it.configId == config.id }
                PortForwardConfigCard(
                    config = config,
                    isRunning = isRunning,
                    onStart = {
                        viewModel.startPortForward(config, "${config.resourceType}/${config.resourceName}")
                    },
                    onStop = {
                        viewModel.portForward.activeProcesses
                            .filter { it.configId == config.id }
                            .forEach { viewModel.portForward.stop(it.id) }
                    },
                    onEdit = {
                        editConfig = config
                        showDialog = true
                    },
                    onDelete = { viewModel.deletePortForwardConfig(config.id) },
                )
            }
        }
    }

    if (showDialog) {
        PortForwardDialog(
            config = editConfig,
            basePort = viewModel.activeContext?.portForwardBasePort ?: 8000,
            onDismiss = {
                showDialog = false
                editConfig = null
            },
            onSave = { config ->
                if (config.id == 0L) {
                    viewModel.portForward.addConfig(config)
                } else {
                    viewModel.portForward.updateConfig(config)
                }
                showDialog = false
                editConfig = null
            },
        )
    }
}

@Composable
private fun ActiveProcessCard(
    process: PortForwardProcess,
    onStop: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(process.podName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "PID: ${process.pid}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "localhost:${process.localPort} -> ${process.namespace}:${process.remotePort}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Started ${formatTimestamp(process.startedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(
                onClick = onStop,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Stop")
            }
        }
    }
}

@Composable
private fun PortForwardConfigCard(
    config: PortForwardConfig,
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${config.resourceType}/${config.resourceName}", style = MaterialTheme.typography.titleSmall)
                    AssistChip(
                        onClick = {},
                        label = { Text(config.namespace) },
                        modifier = Modifier.height(24.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("localhost:${config.localPort} -> ${config.remotePort}", style = MaterialTheme.typography.bodySmall)
                    if (config.customLocalPort) {
                        Text("custom port", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (config.label.isNotEmpty()) {
                    Text(config.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (isRunning) {
                TextButton(
                    onClick = onStop,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Stop") }
            } else {
                TextButton(onClick = onStart) { Text("Start") }
            }
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Del") }
        }
    }
}

@Composable
private fun PortForwardDialog(
    config: PortForwardConfig?,
    basePort: Int,
    onDismiss: () -> Unit,
    onSave: (PortForwardConfig) -> Unit,
) {
    var namespace by remember { mutableStateOf(config?.namespace ?: "default") }
    var resourceType by remember { mutableStateOf(config?.resourceType ?: ResourceType.PODS.kubectlName) }
    var resourceName by remember { mutableStateOf(config?.resourceName ?: "") }
    var remotePort by remember { mutableStateOf(config?.remotePort?.toString() ?: "") }
    var localPort by remember { mutableStateOf(config?.localPort?.toString() ?: "") }
    var customPort by remember { mutableStateOf(config?.customLocalPort ?: false) }
    var label by remember { mutableStateOf(config?.label ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (config != null) "Edit Port Forward" else "New Port Forward") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(value = namespace, onValueChange = {
                    namespace = it
                }, label = { Text("Namespace") }, singleLine = true, modifier = Modifier.width(300.dp))
                OutlinedTextField(value = resourceType, onValueChange = {
                    resourceType = it
                }, label = { Text("Resource Type (pod, svc, deploy)") }, singleLine = true, modifier = Modifier.width(300.dp))
                OutlinedTextField(value = resourceName, onValueChange = {
                    resourceName = it
                }, label = { Text("Resource Name") }, singleLine = true, modifier = Modifier.width(300.dp))
                OutlinedTextField(value = remotePort, onValueChange = {
                    remotePort = it
                }, label = { Text("Remote Port") }, singleLine = true, modifier = Modifier.width(300.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = localPort, onValueChange = {
                        localPort = it
                    }, label = { Text("Local Port") }, singleLine = true, modifier = Modifier.weight(1f))
                    TextButton(onClick = { customPort = !customPort }) { Text(if (customPort) "Auto" else "Custom") }
                }
                OutlinedTextField(value = label, onValueChange = {
                    label = it
                }, label = { Text("Label (optional)") }, singleLine = true, modifier = Modifier.width(300.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rp = remotePort.toIntOrNull() ?: return@Button
                    val lp = localPort.toIntOrNull() ?: return@Button
                    onSave(
                        PortForwardConfig(
                            id = config?.id ?: 0,
                            contextId = config?.contextId ?: 0,
                            namespace = namespace,
                            resourceType = resourceType,
                            resourceName = resourceName,
                            remotePort = rp,
                            localPort = if (customPort) lp else basePort + rp,
                            customLocalPort = customPort,
                            label = label,
                        ),
                    )
                },
                enabled = namespace.isNotBlank() && resourceType.isNotBlank() && resourceName.isNotBlank() && remotePort.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
