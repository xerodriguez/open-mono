package com.fgsoft.klusterui.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fgsoft.klusterui.model.AppView
import com.fgsoft.klusterui.ui.AppViewModel

@Composable
fun TopBar(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "KubeKui",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.weight(1f))

            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                placeholder = { Text("Search resources...") },
                modifier = Modifier.width(280.dp).height(48.dp),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(Modifier.weight(1f))

            TextButton(onClick = { viewModel.currentView = AppView.TREE_VIEW }) {
                Text("Resources")
            }
            TextButton(onClick = { viewModel.currentView = AppView.CONTEXT_SETTINGS }) {
                Text("Contexts")
            }
            TextButton(onClick = { viewModel.currentView = AppView.PORT_FORWARD }) {
                Text("Port Forward")
            }

            TextButton(onClick = { viewModel.isDarkTheme = !viewModel.isDarkTheme }) {
                Text(if (viewModel.isDarkTheme) "Light" else "Dark")
            }
        }
    }
}
