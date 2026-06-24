package com.fgsoft.klusterui.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.ui.AppViewModel

@Composable
fun ContextPage(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    var editContext by remember { mutableStateOf<KubeContext?>(null) }

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
                "Kubernetes Contexts",
                style = MaterialTheme.typography.headlineMedium,
            )
            Button(onClick = { showDialog = true }) {
                Text("+ Add Context")
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(viewModel.contexts.allContexts) { context ->
                ContextCard(
                    context = context,
                    isActive = context.isActive,
                    onActivate = { viewModel.toggleContextActive(context) },
                    onEdit = {
                        editContext = context
                        showDialog = true
                    },
                    onDelete = { viewModel.deleteContext(context.id) },
                )
            }
        }
    }

    if (showDialog) {
        val editingCtx = editContext
        val existingSubDefs =
            remember(editingCtx) {
                editingCtx?.let { ctx ->
                    val subs = viewModel.contexts.subContextsByContextId[ctx.id] ?: emptyList()
                    subs.map { it.regexPattern to it.displayName }
                } ?: emptyList()
            }

        ContextDialog(
            initial = editContext,
            initialSubDefs = existingSubDefs,
            onDismiss = {
                showDialog = false
                editContext = null
            },
            onSave = { context, subDefs ->
                if (context.id == 0L) {
                    viewModel.contexts.add(context, subDefs)
                } else {
                    viewModel.contexts.update(context, subDefs)
                }
                showDialog = false
                editContext = null
            },
        )
    }
}

@Composable
private fun ContextCard(
    context: KubeContext,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .background(Color(context.color), RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    context.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    context.context,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Base port: ${context.portForwardBasePort}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!isActive) {
                TextButton(onClick = onActivate) {
                    Text("Activate")
                }
            } else {
                AssistChip(
                    onClick = {},
                    label = { Text("Active") },
                )
            }
            TextButton(onClick = onEdit) {
                Text("Edit")
            }
            TextButton(
                onClick = onDelete,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("Del")
            }
        }
    }
}

@Composable
private fun ContextDialog(
    initial: KubeContext?,
    initialSubDefs: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (KubeContext, List<Pair<String, String>>) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var contextName by remember { mutableStateOf(initial?.context ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: 0xFF1976D2) }
    var basePort by remember { mutableStateOf((initial?.portForwardBasePort ?: 8000).toString()) }
    val subEntries = remember { mutableStateListOf<Pair<String, String>>() }

    LaunchedEffect(initialSubDefs) {
        subEntries.clear()
        subEntries.addAll(initialSubDefs)
    }

    val colors =
        listOf(
            0xFF1976D2L,
            0xFF388E3C,
            0xFFF57C00,
            0xFFD32F2F,
            0xFF7B1FA2,
            0xFF0097A7,
            0xFF5D4037,
            0xFF455A64,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) "Edit Context" else "Add Context") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = contextName,
                    onValueChange = { contextName = it },
                    label = { Text("kubectl context") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { c ->
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .background(
                                        Color(c),
                                        RoundedCornerShape(8.dp),
                                    ).clickable { color = c }
                                    .then(
                                        if (color == c) {
                                            Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(8.dp),
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ),
                        )
                    }
                }
                OutlinedTextField(
                    value = basePort,
                    onValueChange = { basePort = it },
                    label = { Text("Port Forward Base Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                HorizontalDivider()

                Text("Sub-Namespaces", style = MaterialTheme.typography.labelLarge)

                subEntries.forEachIndexed { index, entry ->
                    val (regex, displayName) = entry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = regex,
                            onValueChange = { subEntries[index] = it to displayName },
                            label = { Text("Regex") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { subEntries[index] = regex to it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { subEntries.removeAt(index) },
                            colors =
                                ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Text("X")
                        }
                    }
                }

                TextButton(
                    onClick = { subEntries.add("" to "") },
                ) {
                    Text("+ Add Sub-Namespace")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = basePort.toIntOrNull() ?: 8000
                    onSave(
                        KubeContext(
                            id = initial?.id ?: 0,
                            name = name,
                            context = contextName,
                            color = color,
                            portForwardBasePort = port,
                            isActive = initial?.isActive ?: false,
                        ),
                        subEntries.toList(),
                    )
                },
                enabled = name.isNotBlank() && contextName.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
