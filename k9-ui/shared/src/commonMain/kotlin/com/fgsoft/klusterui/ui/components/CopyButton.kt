package com.fgsoft.klusterui.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fgsoft.klusterui.copyToClipboard
import kotlinx.coroutines.launch

@Composable
fun CopyButton(
    content: String,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    IconButton(
        onClick = {
            scope.launch {
                try {
                    copyToClipboard(content)
                    snackbarHostState.showSnackbar("Copied to clipboard")
                } catch (_: Exception) {
                    snackbarHostState.showSnackbar("Failed to copy")
                }
            }
        },
        modifier = modifier.size(32.dp),
    ) {
        Text("\uD83D\uDCCB", style = MaterialTheme.typography.labelMedium)
    }
}
