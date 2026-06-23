package com.fgsoft.klusterui.ui.pages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.ResourceType
import com.fgsoft.klusterui.ui.AppViewModel
import com.fgsoft.klusterui.ui.components.CopyButton
import com.fgsoft.klusterui.ui.components.SearchBar
import com.fgsoft.klusterui.ui.components.buildHighlightedAnnotatedString
import com.fgsoft.klusterui.ui.components.findAllMatches

private data class TabSearchState(
    val query: String = "",
    val activeMatchIndex: Int = -1,
)

@Composable
fun ResourceDetailPage(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val resource = viewModel.explorer.selectedResource

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
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ResourceHeader(resource)

        var selectedTab by remember { mutableStateOf(0) }
        val tabs =
            buildList {
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

        val tabName = tabs.getOrElse(selectedTab) { "YAML" }
        val content =
            when (tabName) {
                "YAML" -> viewModel.resource.yaml
                "Events" -> viewModel.resource.events
                "Logs" -> viewModel.resource.podLogs
                "Metrics" -> viewModel.resource.podMetrics
                else -> ""
            }

        ContentCard(
            content = content,
            tabKey = tabName,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ContentCard(
    content: String,
    tabKey: String,
    modifier: Modifier = Modifier,
) {
    val tabSearchStates = remember { mutableStateMapOf<String, TabSearchState>() }
    val searchState = tabSearchStates.getOrPut(tabKey) { TabSearchState() }

    val matches =
        remember(searchState.query, content) {
            findAllMatches(content, searchState.query)
        }
    val safeActiveIndex = searchState.activeMatchIndex.coerceIn(-1, matches.size - 1)

    val annotatedText =
        remember(content, searchState.query, matches, safeActiveIndex) {
            buildHighlightedAnnotatedString(content, searchState.query, matches, safeActiveIndex)
        }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(safeActiveIndex, textLayoutResult) {
        val layout = textLayoutResult ?: return@LaunchedEffect
        val pos = matches.getOrNull(safeActiveIndex) ?: return@LaunchedEffect
        val safePos = pos.coerceIn(0, maxOf(0, content.length - 1))
        val line = layout.getLineForOffset(safePos)
        val lineTop = layout.getLineTop(line)
        val paddingPx = with(density) { 16.dp.toPx() }
        scrollState.animateScrollTo(maxOf(0, (lineTop - paddingPx).toInt()))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchBar(
                    query = searchState.query,
                    onQueryChange = { newQuery ->
                        tabSearchStates[tabKey] =
                            TabSearchState(
                                query = newQuery,
                                activeMatchIndex = if (newQuery.isNotEmpty()) 0 else -1,
                            )
                    },
                    matches = matches,
                    activeMatchIndex = safeActiveIndex,
                    onNext = {
                        if (matches.isNotEmpty()) {
                            tabSearchStates[tabKey] =
                                searchState.copy(
                                    activeMatchIndex = (safeActiveIndex + 1) % matches.size,
                                )
                        }
                    },
                    onPrev = {
                        if (matches.isNotEmpty()) {
                            tabSearchStates[tabKey] =
                                searchState.copy(
                                    activeMatchIndex =
                                        if (safeActiveIndex <= 0) {
                                            matches.size - 1
                                        } else {
                                            safeActiveIndex - 1
                                        },
                                )
                        }
                    },
                    onClear = {
                        tabSearchStates[tabKey] = TabSearchState()
                    },
                    modifier = Modifier.weight(1f),
                )
                CopyButton(content = content)
            }

            HorizontalDivider()

            SelectionContainer(modifier = Modifier.weight(1f)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .horizontalScroll(rememberScrollState())
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                ) {
                    if (content.isEmpty()) {
                        Text(
                            "Loading...",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            onTextLayout = { textLayoutResult = it },
                        )
                    } else {
                        Text(
                            text = annotatedText,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            onTextLayout = { textLayoutResult = it },
                        )
                    }
                }
            }
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        resource.name,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    CopyButton(content = resource.name, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${resource.type.label} • ${resource.namespace}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CopyButton(content = resource.namespace, modifier = Modifier.size(22.dp))
                }
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
