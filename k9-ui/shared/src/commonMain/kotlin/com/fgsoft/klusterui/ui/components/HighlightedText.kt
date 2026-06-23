package com.fgsoft.klusterui.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

fun findAllMatches(
    content: String,
    query: String,
): List<Int> {
    if (query.isBlank()) return emptyList()
    val matches = mutableListOf<Int>()
    val lowerContent = content.lowercase()
    val lowerQuery = query.lowercase()
    var startIndex = 0
    while (true) {
        val idx = lowerContent.indexOf(lowerQuery, startIndex)
        if (idx < 0) break
        matches.add(idx)
        startIndex = idx + 1
    }
    return matches
}

fun buildHighlightedAnnotatedString(
    content: String,
    query: String,
    matches: List<Int>,
    activeIndex: Int,
    matchBg: Color = Color(0xFFFFF176),
    currentMatchBg: Color = Color(0xFFFF8F00),
): AnnotatedString {
    if (content.isEmpty()) return AnnotatedString("")
    if (query.isBlank() || matches.isEmpty()) return AnnotatedString(content)

    return buildAnnotatedString {
        val activeMatchPos = matches.getOrNull(activeIndex)
        val lowerQuery = query.lowercase()
        val lowerContent = content.lowercase()
        var pos = 0

        while (pos < content.length) {
            val nextMatch = lowerContent.indexOf(lowerQuery, pos)
            if (nextMatch < 0) {
                append(content.substring(pos))
                break
            }
            if (nextMatch > pos) {
                append(content.substring(pos, nextMatch))
            }
            val isCurrent = nextMatch == activeMatchPos
            pushStyle(
                SpanStyle(
                    background = if (isCurrent) currentMatchBg else matchBg,
                    color = Color.Black,
                ),
            )
            append(content.substring(nextMatch, nextMatch + query.length))
            pop()
            pos = nextMatch + query.length
        }
    }
}

fun AnnotatedString.Builder.appendHighlightedLine(
    line: String,
    query: String,
    currentMatchPos: Int?,
    matchBg: Color = Color(0xFFFFF176),
    currentMatchBg: Color = Color(0xFFFF8F00),
) {
    if (query.isBlank()) {
        append(line)
        return
    }

    var remaining = line
    var startOffset = 0
    val lowerQuery = query.lowercase()
    while (remaining.isNotEmpty()) {
        val idx = remaining.lowercase().indexOf(lowerQuery)
        if (idx < 0) {
            append(remaining)
            break
        }
        if (idx > 0) {
            append(remaining.substring(0, idx))
        }
        val globalPos = startOffset + idx
        val isCurrentMatch = currentMatchPos != null && globalPos == currentMatchPos
        pushStyle(
            SpanStyle(
                background = if (isCurrentMatch) currentMatchBg else matchBg,
                color = Color.Black,
            ),
        )
        append(remaining.substring(idx, idx + query.length))
        pop()
        startOffset += idx + query.length
        remaining = remaining.substring(idx + query.length)
    }
}
