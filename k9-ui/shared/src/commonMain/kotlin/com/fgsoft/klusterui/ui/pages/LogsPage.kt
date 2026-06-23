package com.fgsoft.klusterui.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fgsoft.klusterui.ui.AppViewModel
import com.fgsoft.klusterui.ui.components.CopyButton
import com.fgsoft.klusterui.ui.components.SearchBar
import com.fgsoft.klusterui.ui.components.appendHighlightedLine
import com.fgsoft.klusterui.ui.store.LogTabState

@Composable
fun LogsPage(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        if (viewModel.logs.tabs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No log tabs open. Right-click a namespace in the tree and select \"View Logs\".",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        ScrollableTabRow(
            selectedTabIndex = viewModel.logs.activeTabIndex.coerceIn(0, viewModel.logs.tabs.size - 1),
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 0.dp,
        ) {
            viewModel.logs.tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == viewModel.logs.activeTabIndex,
                    onClick = { viewModel.logs.setActive(index) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tab.namespace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 140.dp),
                            )
                            if (tab.isLoading) {
                                Spacer(Modifier.width(4.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            TextButton(
                                onClick = { viewModel.logs.close(index) },
                                modifier = Modifier.size(24.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text("\u00D7", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    },
                )
            }
        }

        val activeTab = viewModel.logs.tabs.getOrNull(viewModel.logs.activeTabIndex)
        if (activeTab != null) {
            LogViewer(
                tab = activeTab,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun LogViewer(
    tab: LogTabState,
    viewModel: AppViewModel,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(tab.logContent) {
        if (viewModel.logs.autoScroll && tab.logContent.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val searchMatches = viewModel.logs.searchMatches
    val activeMatchIdx = viewModel.logs.activeMatchIndex
    val currentMatchPos = searchMatches.getOrNull(activeMatchIdx)

    val annotatedLog =
        remember(tab.logContent, viewModel.logs.searchQuery, viewModel.logs.highlightLevel, currentMatchPos) {
            buildLogAnnotatedString(
                tab.logContent,
                viewModel.logs.searchQuery,
                viewModel.logs.highlightLevel,
                currentMatchPos,
            )
        }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(activeMatchIdx, textLayoutResult) {
        val layout = textLayoutResult ?: return@LaunchedEffect
        val pos = searchMatches.getOrNull(activeMatchIdx) ?: return@LaunchedEffect
        val safePos = pos.coerceIn(0, maxOf(0, tab.logContent.length - 1))
        val line = layout.getLineForOffset(safePos)
        val lineTop = layout.getLineTop(line)
        val paddingPx = with(density) { 8.dp.toPx() }
        scrollState.animateScrollTo(maxOf(0, (lineTop - paddingPx).toInt()))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchBar(
                query = viewModel.logs.searchQuery,
                onQueryChange = {
                    viewModel.logs.searchQuery = it
                    viewModel.logs.updateMatchPositions()
                },
                matches = searchMatches,
                activeMatchIndex = activeMatchIdx,
                onNext = { viewModel.logs.searchNext() },
                onPrev = { viewModel.logs.searchPrev() },
                onClear = {
                    viewModel.logs.searchQuery = ""
                    viewModel.logs.searchMatches = emptyList()
                    viewModel.logs.activeMatchIndex = -1
                },
                placeholder = "Search logs...",
            )

            TextButton(onClick = { viewModel.logs.autoScroll = !viewModel.logs.autoScroll }) {
                Text(
                    if (viewModel.logs.autoScroll) "\u2713 Auto" else "Auto",
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (viewModel.logs.autoScroll) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            TextButton(onClick = { viewModel.logs.wrapText = !viewModel.logs.wrapText }) {
                Text(
                    if (viewModel.logs.wrapText) "\u2713 Wrap" else "Wrap",
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (viewModel.logs.wrapText) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            TextButton(onClick = { viewModel.logs.highlightLevel = !viewModel.logs.highlightLevel }) {
                Text(
                    if (viewModel.logs.highlightLevel) "\u2713 HL" else "HL",
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (viewModel.logs.highlightLevel) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    viewModel.logs.fontSize = (viewModel.logs.fontSize - 1f).coerceAtLeast(9f)
                }) { Text("\u2212", style = MaterialTheme.typography.labelLarge) }
                Text(
                    "${viewModel.logs.fontSize.toInt()}px",
                    style = MaterialTheme.typography.labelSmall,
                )
                TextButton(onClick = {
                    viewModel.logs.fontSize = (viewModel.logs.fontSize + 1f).coerceAtMost(24f)
                }) { Text("+", style = MaterialTheme.typography.labelLarge) }
            }

            TextButton(onClick = { viewModel.logs.refresh() }) {
                Text("\u21BB", style = MaterialTheme.typography.labelSmall)
            }

            CopyButton(content = tab.logContent)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        if (tab.isLoading && tab.logContent.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (tab.logContent.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No logs available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            SelectionContainer {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            ).verticalScroll(scrollState)
                            .padding(8.dp),
                ) {
                    Text(
                        text = annotatedLog,
                        fontFamily = FontFamily.Monospace,
                        fontSize = viewModel.logs.fontSize.sp,
                        softWrap = viewModel.logs.wrapText,
                        style = MaterialTheme.typography.bodySmall,
                        onTextLayout = { textLayoutResult = it },
                    )
                }
            }
        }
    }
}

private fun buildLogAnnotatedString(
    content: String,
    searchQuery: String,
    highlightLevel: Boolean,
    currentMatchPos: Int?,
): androidx.compose.ui.text.AnnotatedString =
    buildAnnotatedString {
        if (content.isEmpty()) return@buildAnnotatedString
        if (!highlightLevel && searchQuery.isBlank()) {
            append(content)
            return@buildAnnotatedString
        }

        val errorColor = Color(0xFFEF5350)
        val warnColor = Color(0xFFFFA726)
        val debugColor = Color(0xFF42A5F5)

        val lines = content.lines()
        lines.forEachIndexed { lineIdx, line ->
            if (lineIdx > 0) append("\n")

            if (highlightLevel) {
                val lineStyle =
                    when {
                        line.contains(
                            "ERROR",
                            ignoreCase = true,
                        ) || line.contains("FATAL", ignoreCase = true) -> SpanStyle(color = errorColor)

                        line.contains("WARN", ignoreCase = true) -> SpanStyle(color = warnColor)

                        line.contains("DEBUG", ignoreCase = true) -> SpanStyle(color = debugColor)

                        else -> SpanStyle()
                    }
                withStyle(lineStyle) {
                    appendHighlightedLine(line, searchQuery, currentMatchPos)
                }
            } else {
                appendHighlightedLine(line, searchQuery, currentMatchPos)
            }
        }
    }
