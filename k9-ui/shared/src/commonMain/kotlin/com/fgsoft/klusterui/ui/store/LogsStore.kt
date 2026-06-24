package com.fgsoft.klusterui.ui.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fgsoft.klusterui.AppDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class LogTabState(
    val contextName: String,
    val kubectlContext: String,
    val namespace: String,
    val logContent: String = "",
    val isLoading: Boolean = false,
)

class LogsStore(
    private val deps: AppDependencies,
    private val scope: CoroutineScope,
) {
    var tabs: List<LogTabState> by mutableStateOf(emptyList())
        private set
    var activeTabIndex: Int by mutableStateOf(-1)
    var searchQuery: String by mutableStateOf("")
    var searchMatches: List<Int> by mutableStateOf(emptyList())
        internal set
    var activeMatchIndex: Int by mutableStateOf(-1)
    var fontSize: Float by mutableStateOf(13f)
    var autoScroll: Boolean by mutableStateOf(true)
    var wrapText: Boolean by mutableStateOf(true)
    var highlightLevel: Boolean by mutableStateOf(true)

    fun open(
        contextName: String,
        kubectlContext: String,
        namespace: String,
    ) {
        val existingIndex = tabs.indexOfFirst { it.kubectlContext == kubectlContext && it.namespace == namespace }
        if (existingIndex >= 0) {
            activeTabIndex = existingIndex
            return
        }
        val tab = LogTabState(contextName = contextName, kubectlContext = kubectlContext, namespace = namespace)
        tabs = tabs + tab
        activeTabIndex = tabs.size - 1
        loadContent(activeTabIndex)
    }

    fun close(index: Int) {
        if (index < 0 || index >= tabs.size) return
        tabs = tabs.toMutableList().also { it.removeAt(index) }
        activeTabIndex =
            if (tabs.isEmpty()) {
                -1
            } else if (index >= tabs.size) {
                tabs.size - 1
            } else {
                index.coerceIn(0, tabs.size - 1)
            }
    }

    fun setActive(index: Int) {
        activeTabIndex = index.coerceIn(0, tabs.size - 1)
        if (tabs.getOrNull(activeTabIndex)?.logContent?.isEmpty() != false) {
            loadContent(activeTabIndex)
        }
    }

    fun refresh() {
        val idx = activeTabIndex
        if (idx >= 0 && idx < tabs.size) {
            loadContent(idx)
        }
    }

    fun updateMatchPositions() {
        val query = searchQuery
        val content = tabs.getOrNull(activeTabIndex)?.logContent ?: ""
        if (query.isBlank()) {
            searchMatches = emptyList()
            activeMatchIndex = -1
            return
        }
        val matches = mutableListOf<Int>()
        var startIndex = 0
        while (true) {
            val idx = content.indexOf(query, startIndex, ignoreCase = true)
            if (idx < 0) break
            matches.add(idx)
            startIndex = idx + 1
        }
        searchMatches = matches
        activeMatchIndex = if (matches.isNotEmpty()) 0 else -1
    }

    fun searchNext() {
        if (searchMatches.isEmpty()) {
            updateMatchPositions()
            return
        }
        activeMatchIndex = (activeMatchIndex + 1) % searchMatches.size
    }

    fun searchPrev() {
        if (searchMatches.isEmpty()) {
            updateMatchPositions()
            return
        }
        activeMatchIndex =
            if (activeMatchIndex - 1 < 0) {
                searchMatches.size - 1
            } else {
                activeMatchIndex - 1
            }
    }

    private fun loadContent(index: Int) {
        val tab = tabs.getOrNull(index) ?: return
        if (tab.isLoading) return
        tabs = tabs.toMutableList().also { it[index] = tab.copy(isLoading = true) }
        scope.launch {
            try {
                val content = deps.kubectlClient.getNamespacePodLogs(tab.kubectlContext, tab.namespace)
                tabs = tabs.toMutableList().also { it[index] = tab.copy(logContent = content, isLoading = false) }
                if (index == activeTabIndex) {
                    updateMatchPositions()
                }
            } catch (_: Exception) {
                tabs =
                    tabs.toMutableList().also {
                        it[index] = tab.copy(logContent = "Error loading logs", isLoading = false)
                    }
            }
        }
    }
}
