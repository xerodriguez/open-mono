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
import com.fgsoft.klusterui.ui.LogTabState
import com.fgsoft.klusterui.ui.components.CopyButton
import com.fgsoft.klusterui.ui.components.SearchBar
import com.fgsoft.klusterui.ui.components.appendHighlightedLine

@Composable
fun LogsPage(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        if (viewModel.logTabs.isEmpty()) {
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
            selectedTabIndex = viewModel.activeLogTabIndex.coerceIn(0, viewModel.logTabs.size - 1),
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 0.dp,
        ) {
            viewModel.logTabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == viewModel.activeLogTabIndex,
                    onClick = { viewModel.setActiveLogTab(index) },
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
                                onClick = { viewModel.closeLogsTab(index) },
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

        val activeTab = viewModel.logTabs.getOrNull(viewModel.activeLogTabIndex)
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
        if (viewModel.logAutoScroll && tab.logContent.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val searchMatches = viewModel.logSearchMatches
    val activeMatchIdx = viewModel.activeLogSearchMatchIndex
    val currentMatchPos = searchMatches.getOrNull(activeMatchIdx)

    val annotatedLog =
        remember(tab.logContent, viewModel.logSearchQuery, viewModel.logHighlightLevel, currentMatchPos) {
            buildLogAnnotatedString(
                tab.logContent,
                viewModel.logSearchQuery,
                viewModel.logHighlightLevel,
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
                query = viewModel.logSearchQuery,
                onQueryChange = {
                    viewModel.logSearchQuery = it
                    viewModel.updateLogSearchMatches()
                },
                matches = searchMatches,
                activeMatchIndex = activeMatchIdx,
                onNext = { viewModel.searchLogsNext() },
                onPrev = { viewModel.searchLogsPrev() },
                onClear = {
                    viewModel.logSearchQuery = ""
                    viewModel.logSearchMatches = emptyList()
                    viewModel.activeLogSearchMatchIndex = -1
                },
                placeholder = "Search logs...",
            )

            TextButton(onClick = { viewModel.logAutoScroll = !viewModel.logAutoScroll }) {
                Text(
                    if (viewModel.logAutoScroll) "\u2713 Auto" else "Auto",
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (viewModel.logAutoScroll) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            TextButton(onClick = { viewModel.logWrapText = !viewModel.logWrapText }) {
                Text(
                    if (viewModel.logWrapText) "\u2713 Wrap" else "Wrap",
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (viewModel.logWrapText) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            TextButton(onClick = { viewModel.logHighlightLevel = !viewModel.logHighlightLevel }) {
                Text(
                    if (viewModel.logHighlightLevel) "\u2713 HL" else "HL",
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (viewModel.logHighlightLevel) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    viewModel.logFontSize = (viewModel.logFontSize - 1f).coerceAtLeast(9f)
                }) { Text("\u2212", style = MaterialTheme.typography.labelLarge) }
                Text(
                    "${viewModel.logFontSize.toInt()}px",
                    style = MaterialTheme.typography.labelSmall,
                )
                TextButton(onClick = {
                    viewModel.logFontSize = (viewModel.logFontSize + 1f).coerceAtMost(24f)
                }) { Text("+", style = MaterialTheme.typography.labelLarge) }
            }

            TextButton(onClick = { viewModel.refreshActiveLogTab() }) {
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
                        fontSize = viewModel.logFontSize.sp,
                        softWrap = viewModel.logWrapText,
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
