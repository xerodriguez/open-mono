@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.fgsoft.klusterui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fgsoft.klusterui.model.AppView
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.NamespaceInfo
import com.fgsoft.klusterui.model.ResourceType
import com.fgsoft.klusterui.ui.AppViewModel

private val resourceTypesInTree = ResourceType.entries.filter { it !in setOf(ResourceType.NAMESPACES, ResourceType.NODES) }

@Composable
fun Sidebar(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .width(280.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
    ) {
        if (viewModel.activeContexts.isEmpty()) {
            Text(
                "No active contexts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            return
        }

        viewModel.activeContexts.forEach { context ->
            ContextAccordion(context, viewModel)
        }
    }
}

@Composable
private fun ContextAccordion(
    context: KubeContext,
    viewModel: AppViewModel,
) {
    val isExpanded = context.id in viewModel.expandedContexts
    var showContextMenu by remember { mutableStateOf(false) }
    val contextColor = Color(context.color)
    val namespaces = viewModel.contextNamespaces[context.name] ?: emptyList()
    val matchesSearch =
        viewModel.treeMatchesSearch(context.name) ||
            namespaces.any { viewModel.treeMatchesSearch(it.name) } ||
            viewModel.searchQuery.isBlank()

    if (!matchesSearch) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isExpanded) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { viewModel.toggleContextExpanded(context.id) }
                    .onPointerEvent(PointerEventType.Press) { event ->
                        if (event.button == androidx.compose.ui.input.pointer.PointerButton.Secondary) {
                            showContextMenu = true
                        }
                    }.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(contextColor),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                context.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (isExpanded) "▾" else "▸",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text(if (context.isActive) "Deactivate" else "Activate") },
                    onClick = {
                        showContextMenu = false
                        viewModel.toggleContextActive(context)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showContextMenu = false
                        viewModel.currentView = AppView.CONTEXT_SETTINGS
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showContextMenu = false
                        viewModel.showDeleteContextDialog = context
                    },
                )
            }
        }

        if (isExpanded) {
            if (namespaces.isEmpty()) {
                Text(
                    "Loading namespaces...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp, top = 4.dp, bottom = 4.dp),
                )
            } else {
                namespaces.forEach { ns ->
                    val nsMatches = viewModel.treeMatchesSearch(ns.name) || viewModel.searchQuery.isBlank()
                    if (!nsMatches) return@forEach

                    NamespaceItem(
                        namespace = ns,
                        context = context,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun NamespaceItem(
    namespace: NamespaceInfo,
    context: KubeContext,
    viewModel: AppViewModel,
) {
    val key = "${context.name}/${namespace.name}"
    val isSelected = key in viewModel.expandedNamespaces
    val nsResources = viewModel.contextResources[key] ?: emptyMap()
    val hasResources = nsResources.isNotEmpty()
    val totalCount = nsResources.values.sumOf { it.size }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            Color.Transparent
                        },
                    ).clickable {
                        viewModel.toggleNamespaceExpanded(context.name, namespace.name)
                    }.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (isSelected) "▾" else "▸",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                namespace.name,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!isSelected && totalCount > 0) {
                Text(
                    "$totalCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isSelected) {
            if (!hasResources) {
                Text(
                    "Loading resources...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 52.dp, top = 2.dp, bottom = 4.dp),
                )
            } else {
                resourceTypesInTree.forEach { type ->
                    val resources = nsResources[type]
                    if (resources.isNullOrEmpty()) return@forEach
                    val filtered =
                        if (viewModel.searchQuery.isBlank()) {
                            resources
                        } else {
                            resources.filter { viewModel.treeMatchesSearch(it.name, it.status) }
                        }
                    if (filtered.isEmpty()) return@forEach

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${type.label} (${filtered.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 52.dp, top = 6.dp, bottom = 2.dp),
                        )

                        filtered.forEach { resource ->
                            ResourceItem(
                                resource = resource,
                                viewModel = viewModel,
                                indent = 60.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceItem(
    resource: KubeResource,
    viewModel: AppViewModel,
    indent: androidx.compose.ui.unit.Dp,
) {
    var showMenu by remember { mutableStateOf(false) }
    val isSelected = resource == viewModel.selectedResource

    Box {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = indent)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                    ).clickable { viewModel.selectResourceAndNamespace(resource) }
                    .onPointerEvent(PointerEventType.Press) { event ->
                        if (event.button == androidx.compose.ui.input.pointer.PointerButton.Secondary) {
                            showMenu = true
                        }
                    }.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    resource.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                if (resource.status.isNotEmpty()) {
                    Text(
                        resource.status,
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("View Details") },
                onClick = {
                    showMenu = false
                    viewModel.selectResourceAndNamespace(resource)
                },
            )
            DropdownMenuItem(
                text = { Text("Port Forward") },
                onClick = {
                    showMenu = false
                    viewModel.showPortForwardDialog = resource
                },
            )
        }
    }
}
