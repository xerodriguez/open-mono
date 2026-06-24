# AppViewModel Refactoring Plan

## Current state
`AppViewModel.kt` — **757 lines**, ~60 methods spanning 7+ unrelated concerns. Every page reads/writes this single god object directly.

| Concern | Methods (approx) | Lines |
|---|---|---|
| Context CRUD + lifecycle | `addContext`, `updateContext`, `deleteContext`, `activateContext`, `toggleContextActive` | ~80 |
| Sub-contexts + favorites | `loadSubContexts`, `toggleFavorite`, `groupNamespacesBySubContext` | ~40 |
| Tree expand state | `toggleContextExpanded`, `toggleNamespaceExpanded`, `toggleSubContextExpanded` | ~30 |
| Namespace + resource loading | `loadContextNamespaces`, `refreshActiveContextData`, `selectResource`, `selectNamespace` | ~160 |
| Resource detail data (YAML/events/logs/metrics) | `selectResource` inline fetches + state fields | ~80 |
| Port forwarding CRUD + processes | `addPortForwardConfig`, `startPortForward`, `stopPortForward`, `killAll` | ~80 |
| Unified logs (tabs, search) | `openLogsTab`, `closeLogsTab`, `updateLogSearchMatches` etc. | ~80 |
| UI state (theme, nav, dialogs, search) | `currentView`, `isDarkTheme`, `showDeleteContextDialog` | ~15 |

---

## New structure (6 new files)

```
shared/src/commonMain/kotlin/com/fgsoft/klusterui/ui/
├── AppViewModel.kt              → Coordinator (~80 lines)
├── ContextManager.kt             → Context domain (CRUD, activate, sub-contexts, favorites)
├── ResourceManager.kt            → Resource domain (namespaces, resources, YAML, events)
├── PortForwardManager.kt         → Port forwarding domain (configs + process lifecycle)
├── LogsManager.kt                → Unified logs domain (tabs, search, display settings)
├── TreeState.kt                  → Tree expand state (pure class with toggle methods)
└── UiStateManager.kt             → UI concerns (theme, nav view, dialogs, search query)
```

---

## 1. `TreeState` (~30 lines) — Pure state holder with toggle methods

**File:** `shared/src/commonMain/kotlin/com/fgsoft/klusterui/ui/TreeState.kt`

```kotlin
class TreeState {
    var expandedContexts: Set<Long> = emptySet()
    var expandedNamespaces: Set<String> = emptySet()
    var expandedSubContexts: Set<String> = emptySet()

    fun toggleContextExpanded(contextId: Long) {
        expandedContexts = if (contextId in expandedContexts) expandedContexts - contextId
                           else expandedContexts + contextId
    }

    fun toggleNamespaceExpanded(contextName: String, namespaceName: String): Boolean {
        val key = "$contextName/$namespaceName"
        expandedNamespaces = if (key in expandedNamespaces) expandedNamespaces - key
                             else expandedNamespaces + key
        return !(key in expandedNamespaces)  // true if now expanded (caller should load resources)
    }

    fun toggleSubContextExpanded(contextId: Long, subContextId: Long) {
        val key = "$contextId/$subContextId"
        expandedSubContexts = if (key in expandedSubContexts) expandedSubContexts - key
                              else expandedSubContexts + key
    }
}
```

**State moved from:** `AppViewModel` lines 48-50

---

## 2. `UiStateManager` (~50 lines) — UI concerns

**File:** `shared/src/commonMain/kotlin/com/fgsoft/klusterui/ui/UiStateManager.kt`

### State:
| Field | Source line | Description |
|---|---|---|
| `currentView: AppView` | 28 | Navigation target |
| `searchQuery: String` | 63 | Global search text |
| `isDarkTheme: Boolean` | 64 | Theme toggle |
| `showDeleteContextDialog: KubeContext?` | 87 | Dialog trigger state |
| `showPortForwardDialog: KubeResource?` | 88 | Dialog trigger state |

### Methods:
None — all fields are direct-access mutable state. Pages read/write these directly.

---

## 3. `ContextManager` (~180 lines) — Context domain

**File:** `shared/src/commonMain/kotlin/com/fgsoft/klusterui/ui/ContextManager.kt`

### State:
| Field | Source line |
|---|---|
| `contexts: List<KubeContext>` | 30-31 |
| `activeContexts: List<KubeContext>` | 32-33 |
| `subContextsByContextId: Map<Long, List<SubContext>>` | 44-45 |
| `favoriteNamespacesByContextId: Map<Long, Set<String>>` | 46-47 |

### Methods:
| Method | Source line(s) | Description |
|---|---|---|
| `load(deps: AppDependencies)` | 168-176 | Loads contexts, sub-contexts, favorites, namespaces for all active |
| `activateContext(context: KubeContext)` | 207-215 | Sets active, loads namespaces + port configs (triggers reload) |
| `toggleContextActive(context: KubeContext)` | 217-228 | Activates/deactivates, updates expanded state |
| `addContext(context: KubeContext, subDefs)` | 361-373 | Creates context + sub-contexts, reloads |
| `updateContext(context: KubeContext, subDefs)` | 375-387 | Updates context + sub-contexts, reloads |
| `deleteContext(id: Long)` | 389-406 | Deletes context + cascades sub-contexts, reloads |
| `toggleFavorite(contextId: Long, namespace: String)` | 188-198 | Add/remove from favorites, reloads |
| `isFavorite(contextId: Long, namespace: String)` | 200-203 | O(1) lookup |
| `getFavoriteNamespacesForContext(contextId: Long)` | 205 | Returns set of favorite namespace names |
| `groupNamespacesBySubContext(contextId, namespaces)` | 271-301 | Groups namespaces by sub-context regex patterns |

### Dependencies:
- `contextRepository` — for all CRUD operations
- **No kubectlClient** (namespace fetching belongs to ResourceManager)

---

## 4. `ResourceManager` (~280 lines) — Resource domain

**File:** `shared/src/commonMain/kotlin/com/fgsoft/klusterui/ui/ResourceManager.kt`

### State:
| Field | Source line |
|---|---|
| `contextNamespaces: Map<String, List<NamespaceInfo>>` | 38-39 |
| `contextResources: Map<String, Map<ResourceType, List<KubeResource>>>` | 41-42 |
| `selectedResource: KubeResource?` | 56 |
| `resources: List<KubeResource>` | 54-55 |
| `selectedNamespace: String` | 52 |
| `selectedResourceType: ResourceType` | 53 |
| `resourceYaml: String` | 66-67 |
| `resourceEvents: String` | 68-69 |
| `podLogs: String` | 70-71 |
| `podMetrics: String` | 72-73 |

### Methods:
| Method | Source line(s) | Description |
|---|---|---|
| `loadContextNamespaces(ctx: KubeContext)` | 408-417 | Fetches namespaces for context, updates state |
| `refreshActiveContextData(activeContexts)` | 104-123 | 10s timer: diff-based namespace update, refreshes expanded namespaces |
| `refreshContextResources(ctx, namespace)` | 133-153 | Fetches all resource types for a namespace, diff-based update |
| `selectResource(resource: KubeResource?)` | 484-539 | Selects resource, loads YAML + events + logs (if pod) + metrics (if pod) |
| `selectResourceAndNamespace(resource: KubeResource)` | 303-359 | Same as selectResource but also sets selectedNamespace + selectedResourceType |
| `selectNamespace(namespace: String)` | 446-457 | Sets selected namespace, loads resources for current type |
| `selectResourceType(type: ResourceType)` | 459-470 | Sets type, reloads resources for current namespace |
| `loadResources(contextName, namespace, type)` | 472-482 | Fetches resources from kubectlClient |
| `treeMatchesSearch(name, vararg extras)` | 625-632 | Search filtering helper |
| `filteredResources` (val) | 634-644 | Filtered list based on searchQuery |
| `namespacesDiffer(a, b)` (private) | 125-131 | Helper for diff-based refresh |
| `resourcesByTypeDiffer(a, b)` (private) | 155-166 | Helper for diff-based refresh |

### Dependencies:
- `kubectlClient` — all data fetching (getNamespaces, getResources, getResourceYaml, etc.)
- **No contextRepository** (context CRUD belongs to ContextManager)

---

## 5. `PortForwardManager` (~120 lines) — Port forwarding domain

**File:** `shared/src/commonMain/kotlin/com/fgsoft/klusterui/ui/PortForwardManager.kt`

### State:
| Field | Source line |
|---|---|
| `portForwardConfigs: List<PortForwardConfig>` | 58-59 |
| `activePortForwardProcesses: List<PortForwardProcess>` | 60-61 |

### Methods:
| Method | Source line(s) | Description |
|---|---|---|
| `loadForContext(ctxId: Long)` | 541-544 | Loads configs for a specific context |
| `addConfig(config: PortForwardConfig)` | 546-550 | Creates config, reloads list |
| `updateConfig(config: PortForwardConfig)` | 552-555 | Updates config, reloads list |
| `deleteConfig(id: Long)` | 557-562 | Stops all processes for config, deletes config, reloads + refreshes |
| `startForward(config: PortForwardConfig, podName: String)` | 564-593 | Launches kubectl process, creates DB record, refreshes |
| `stopProcess(processId: Long)` | 595-601 | Kills process by PID, updates DB, refreshes |
| `refreshActiveProcesses()` | 613-615 | Reloads active processes from DB |
| `killAll()` | 617-623 | Kills all active processes, clears state |

### Dependencies:
- `portForwardRepository` — CRUD for configs and processes
- `processManager` — kill process operations

---

## 6. `LogsManager` (~180 lines) — Unified logs domain

**File:** `shared/src/commonMain/kotlin/com/fgsoft/klusterui/ui/LogsManager.kt`

### State:
| Field | Source line |
|---|---|
| `logTabs: List<LogTabState>` | 75-76 |
| `activeLogTabIndex: Int` | 77 |
| `logSearchQuery: String` | 78 |
| `logSearchMatches: List<Int>` | 79-80 |
| `activeLogSearchMatchIndex: Int` | 81 |
| `logFontSize: Float` | 82 |
| `logAutoScroll: Boolean` | 83 |
| `logWrapText: Boolean` | 84 |
| `logHighlightLevel: Boolean` | 85 |

### Methods:
| Method | Source line(s) | Description |
|---|---|---|
| `openTab(contextName, kubectlContext, namespace)` | 646-660 | Opens existing tab or creates new, fetches logs |
| `closeTab(index: Int)` | 662-673 | Removes tab, adjusts active index |
| `setActiveTab(index: Int)` | 675-680 | Switches active tab, loads if empty |
| `refreshActiveTab()` | 682-687 | Reloads logs for active tab |
| `updateSearchMatches()` | 709-727 | Finds all case-insensitive match positions in current tab content |
| `searchNext()` | 729-735 | Cycles to next match (wraps around) |
| `searchPrev()` | 737-748 | Cycles to previous match (wraps around) |

### Data class:
```kotlin
data class LogTabState(
    val contextName: String,
    val kubectlContext: String,
    val namespace: String,
    val logContent: String = "",
    val isLoading: Boolean = false,
)
```

### Dependencies:
- `kubectlClient.getNamespacePodLogs()` — aggregated namespace logs

---

## 7. `AppViewModel` — Coordinator (~80 lines)

**File:** `shared/src/commonMain/kotlin/com/fgsoft/klusterui/ui/AppViewModel.kt` (after refactor)

```kotlin
class AppViewModel(deps: AppDependencies) {
    val contextManager = ContextManager()
    val resourceManager = ResourceManager()
    val portForwardManager = PortForwardManager()
    val logsManager = LogsManager()
    val treeState = TreeState()
    val uiState = UiStateManager()

    init {
        loadAll(deps)
        startRefreshTimer(deps)
    }

    private fun loadAll(deps: AppDependencies) {
        contextManager.load(deps)
    }

    private fun startRefreshTimer(deps: AppDependencies) {
        // Launches coroutine that calls resourceManager.refreshActiveContextData() every 10s
    }

    // Cross-manager operations that need coordination:
    fun deleteContext(id: Long) {
        contextManager.deleteContext(id)
        uiState.showDeleteContextDialog = null
    }

    fun selectResourceAndNamespace(resource: KubeResource) {
        resourceManager.selectResourceAndNamespace(resource)
    }

    fun toggleFavorite(contextId: Long, namespace: String) {
        contextManager.toggleFavorite(contextId, namespace)
    }

    fun toggleContextExpanded(contextId: Long) {
        treeState.toggleContextExpanded(contextId)
    }

    fun toggleNamespaceExpanded(contextName: String, namespaceName: String): Boolean {
        return treeState.toggleNamespaceExpanded(contextName, namespaceName)
    }

    fun toggleSubContextExpanded(contextId: Long, subContextId: Long) {
        treeState.toggleSubContextExpanded(contextId, subContextId)
    }

    fun activateContext(context: KubeContext) {
        contextManager.activateContext(context)
    }

    fun toggleContextActive(context: KubeContext) {
        contextManager.toggleContextActive(context)
    }

    fun selectResource(resource: KubeResource?) {
        resourceManager.selectResource(resource)
    }

    fun selectNamespace(namespace: String) {
        resourceManager.selectNamespace(namespace)
    }

    fun selectResourceType(type: ResourceType) {
        resourceManager.selectResourceType(type)
    }

    fun addContext(context: KubeContext, subDefs: List<Pair<String, String>>) {
        contextManager.addContext(context, subDefs)
    }

    fun updateContext(context: KubeContext, subDefs: List<Pair<String, String>>) {
        contextManager.updateContext(context, subDefs)
    }

    fun groupNamespacesBySubContext(contextId: Long, namespaces: List<NamespaceInfo>) =
        contextManager.groupNamespacesBySubContext(contextId, namespaces)
}
```

---

## Page changes — each page accepts individual managers

### Before / After signature comparison:

| Page | Before | After |
|---|---|---|
| `Sidebar.kt` | `Sidebar(viewModel: AppViewModel, modifier)` | `Sidebar(contextManager: ContextManager, resourceManager: ResourceManager, treeState: TreeState, uiState: UiStateManager, modifier)` |
| `TopBar.kt` | `TopBar(viewModel: AppViewModel, modifier)` | `TopBar(uiState: UiStateManager, contextManager: ContextManager, modifier)` |
| `ResourceDetailPage.kt` | `ResourceDetailPage(viewModel: AppViewModel, modifier)` | `ResourceDetailPage(resourceManager: ResourceManager, uiState: UiStateManager, modifier)` |
| `ContextPage.kt` | `ContextPage(viewModel: AppViewModel, modifier)` | `ContextPage(contextManager: ContextManager, uiState: UiStateManager, modifier)` |
| `PortForwardPage.kt` | `PortForwardPage(viewModel: AppViewModel, modifier)` | `PortForwardPage(portForwardManager: PortForwardManager, contextManager: ContextManager, uiState: UiStateManager, modifier)` |
| `LogsPage.kt` | `LogsPage(viewModel: AppViewModel, modifier)` | `LogsPage(logsManager: LogsManager, modifier)` |

### What each page reads/writes:

**Sidebar.kt:**
- Reads: `contextManager.activeContexts`, `uiState.searchQuery`, `contextManager.contextNamespaces`
- Writes: `treeState.toggleContextExpanded()`, `treeState.toggleNamespaceExpanded()`, `treeState.toggleSubContextExpanded()`
- Calls: `resourceManager.selectResourceAndNamespace()`, `contextManager.toggleFavorite()`, `uiState.showPortForwardDialog = resource`

**TopBar.kt:**
- Reads: `uiState.searchQuery`, `contextManager.activeContexts`
- Writes: `uiState.currentView = ...`, `uiState.searchQuery = ...`, `uiState.isDarkTheme = !it`

**ResourceDetailPage.kt:**
- Reads: `resourceManager.selectedResource`, `resourceManager.resourceYaml`, `resourceManager.resourceEvents`, `resourceManager.podLogs`, `resourceManager.podMetrics`
- Writes: `resourceManager.selectResource()`, `contextManager.activateContext()`

**ContextPage.kt:**
- Reads: `contextManager.contexts`, `contextManager.subContextsByContextId`
- Writes: `contextManager.addContext()`, `contextManager.updateContext()`, `contextManager.deleteContext()`

**PortForwardPage.kt:**
- Reads: `portForwardManager.portForwardConfigs`, `portForwardManager.activePortForwardProcesses`
- Writes: `portForwardManager.addConfig()`, `portForwardManager.updateConfig()`, `portForwardManager.deleteConfig()`, `portForwardManager.startForward()`, `portForwardManager.stopProcess()`, `portForwardManager.killAll()`

**LogsPage.kt:**
- Reads: `logsManager.logTabs`, `logsManager.activeLogTabIndex`, `logsManager.logSearchQuery`, etc.
- Writes: `logsManager.openTab()`, `logsManager.closeTab()`, `logsManager.setActiveTab()`, `logsManager.searchNext()`, etc.

### App.kt (main) changes:

```kotlin
@Composable fun App(deps: AppDependencies) {
    val vm = remember { AppViewModel(deps) }

    KlusterUiTheme(darkTheme = vm.uiState.isDarkTheme) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(vm.uiState, vm.contextManager)
            Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (vm.uiState.currentView) {
                    AppView.TREE_VIEW -> {
                        Sidebar(vm.contextManager, vm.resourceManager, vm.treeState, vm.uiState, Modifier.width(sidebarWidthDp.dp))
                        // ... divider ...
                        ResourceDetailPage(vm.resourceManager, vm.uiState, Modifier.weight(1f).fillMaxHeight())
                    }
                    AppView.CONTEXT_SETTINGS -> ContextPage(vm.contextManager, vm.uiState, Modifier.weight(1f).fillMaxHeight())
                    AppView.PORT_FORWARD -> PortForwardPage(vm.portForwardManager, vm.contextManager, vm.uiState, Modifier.weight(1f).fillMaxHeight())
                    AppView.LOGS -> LogsPage(vm.logsManager, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }

        vm.uiState.showDeleteContextDialog?.let { context -> ... }
        vm.uiState.showPortForwardDialog?.let { resource -> ... }
    }
}
```

---

## Migration steps (execution order)

1. **Create `TreeState.kt`** — extract expand state + methods from AppViewModel
2. **Create `UiStateManager.kt`** — extract theme, nav, dialogs, search from AppViewModel
3. **Create `ContextManager.kt`** — extract context CRUD + sub-contexts + favorites from AppViewModel
4. **Create `ResourceManager.kt`** — extract namespace/resource loading + detail data from AppViewModel
5. **Create `PortForwardManager.kt`** — extract port forwarding CRUD + process lifecycle from AppViewModel
6. **Create `LogsManager.kt`** — extract logs tabs + search from AppViewModel (includes LogTabState)
7. **Rewrite `AppViewModel.kt`** — thin coordinator wiring all managers together, forwarding cross-manager ops
8. **Update `Sidebar.kt`** — accept individual managers instead of AppViewModel
9. **Update `TopBar.kt`** — accept UiStateManager + ContextManager
10. **Update `ResourceDetailPage.kt`** — accept ResourceManager + UiStateManager
11. **Update `ContextPage.kt`** — accept ContextManager + UiStateManager
12. **Update `PortForwardPage.kt`** — accept PortForwardManager + ContextManager + UiStateManager
13. **Update `LogsPage.kt`** — accept LogsManager
14. **Update `App.kt`** — pass individual managers to pages, use uiState for navigation/dialogs
15. **Build + verify** — `./gradlew :desktopApp:compileKotlin`

---

## Lines of code estimate

| File | Current (in AppViewModel) | After refactor |
|---|---|---|
| AppViewModel.kt | 757 | ~80 |
| TreeState.kt | (part of AppViewModel) | ~30 |
| UiStateManager.kt | (part of AppViewModel) | ~50 |
| ContextManager.kt | (part of AppViewModel) | ~180 |
| ResourceManager.kt | (part of AppViewModel) | ~280 |
| PortForwardManager.kt | (part of AppViewModel) | ~120 |
| LogsManager.kt | (part of AppViewModel) | ~180 |
| **Total** | **757** | **~920** (more lines, but each is focused and testable) |

The line count increases because state + behavior that was shared across concerns is now properly separated. Each class has a single responsibility and can be tested independently.

---

## Tradeoffs

| Approach | Pros | Cons |
|---|---|---|
| **Current (single ViewModel)** | Simple, no wiring needed | God object (~750 lines), hard to test, every change touches everything |
| **Domain managers (plan above)** | Each class has one responsibility, easy to test in isolation, changes are localized | More files (6 new), need to wire them together, pages accept multiple params |
