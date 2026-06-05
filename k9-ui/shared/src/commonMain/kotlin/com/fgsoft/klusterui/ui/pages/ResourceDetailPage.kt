package com.fgsoft.klusterui.ui.pages

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.ResourceType
import com.fgsoft.klusterui.ui.AppViewModel

@Composable
fun ResourceDetailPage(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val resource = viewModel.selectedResource

    if (resource == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Select a resource from the sidebar",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ResourceHeader(resource)

        var selectedTab by remember { mutableStateOf(0) }
        val tabs = buildList {
            add("YAML")
            add("Events")
            if (resource.type == ResourceType.PODS) {
                add("Logs")
                add("Metrics")
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        when (tabs.getOrElse(selectedTab) { "YAML" }) {
            "YAML" -> YamlView(viewModel.resourceYaml)
            "Events" -> YamlView(viewModel.resourceEvents)
            "Logs" -> YamlView(viewModel.podLogs)
            "Metrics" -> YamlView(viewModel.podMetrics)
        }
    }
}

@Composable
private fun ResourceHeader(resource: KubeResource) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    resource.name,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${resource.type.label} • ${resource.namespace}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (resource.status.isNotEmpty()) {
                AssistChip(
                    onClick = {},
                    label = { Text(resource.status) },
                )
            }
        }
    }
}

@Composable
private fun YamlView(content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = content.ifEmpty { "Loading..." },
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
