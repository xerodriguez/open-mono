package com.fgsoft.klusterui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fgsoft.klusterui.model.AppView
import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.ui.AppViewModel
import com.fgsoft.klusterui.ui.components.Sidebar
import com.fgsoft.klusterui.ui.components.TopBar
import com.fgsoft.klusterui.ui.pages.ContextPage
import com.fgsoft.klusterui.ui.pages.PortForwardPage
import com.fgsoft.klusterui.ui.pages.ResourceDetailPage
import com.fgsoft.klusterui.ui.theme.KlusterUiTheme

@Composable
fun App(deps: AppDependencies) {
    val viewModel = remember { AppViewModel(deps) }

    KlusterUiTheme(darkTheme = viewModel.isDarkTheme) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(viewModel)
            Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (viewModel.currentView) {
                    AppView.TREE_VIEW -> {
                        Sidebar(viewModel)
                        ResourceDetailPage(viewModel, modifier = Modifier.fillMaxSize())
                    }

                    AppView.CONTEXT_SETTINGS -> {
                        ContextPage(viewModel, modifier = Modifier.fillMaxSize())
                    }

                    AppView.PORT_FORWARD -> {
                        PortForwardPage(viewModel, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        viewModel.showDeleteContextDialog?.let { context ->
            AlertDialog(
                onDismissRequest = { viewModel.showDeleteContextDialog = null },
                title = { Text("Delete Context") },
                text = { Text("Delete \"${context.name}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteContext(context.id)
                        viewModel.showDeleteContextDialog = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showDeleteContextDialog = null }) { Text("Cancel") }
                },
            )
        }

        viewModel.showPortForwardDialog?.let { resource ->
            SidebarPortForwardDialog(
                resource = resource,
                viewModel = viewModel,
                onDismiss = { viewModel.showPortForwardDialog = null },
            )
        }
    }
}

@Composable
private fun SidebarPortForwardDialog(
    resource: KubeResource,
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
) {
    val basePort = viewModel.activeContext?.portForwardBasePort ?: 8000
    val defaultLocalPort = basePort + 80
    val localPort = remember { mutableStateOf(defaultLocalPort.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Port Forward") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Resource: ${resource.name}")
                Text("Namespace: ${resource.namespace}")
                Text("Type: ${resource.type.label}")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = localPort.value,
                    onValueChange = { localPort.value = it },
                    label = { Text("Local Port") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val lp = localPort.value.toIntOrNull() ?: return@TextButton
                val ctx = viewModel.activeContext ?: return@TextButton
                val config =
                    PortForwardConfig(
                        contextId = ctx.id,
                        namespace = resource.namespace,
                        resourceType = resource.type.kubectlName,
                        resourceName = resource.name,
                        remotePort = 80,
                        localPort = lp,
                        customLocalPort = true,
                        label = resource.name,
                    )
                viewModel.startPortForward(config, resource.name)
                onDismiss()
            }) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
