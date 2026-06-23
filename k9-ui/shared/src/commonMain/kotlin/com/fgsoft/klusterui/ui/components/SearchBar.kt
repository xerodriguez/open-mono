package com.fgsoft.klusterui.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    matches: List<Int>,
    activeMatchIndex: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClear: () -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(200.dp),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Text(
                        "\u00D7",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier.clickable { onClear() },
                    )
                }
            },
        )

        TextButton(onClick = onPrev, enabled = matches.isNotEmpty()) {
            Text("\u25B2", style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = onNext, enabled = matches.isNotEmpty()) {
            Text("\u25BC", style = MaterialTheme.typography.labelSmall)
        }
        if (matches.isNotEmpty()) {
            Text(
                "${activeMatchIndex + 1}/${matches.size}",
                style = MaterialTheme.typography.labelSmall,
            )
        } else if (query.isNotEmpty()) {
            Text(
                "0/0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
