# AppViewModel Refactor — God Class Decomposition

## Problem
`AppViewModel.kt` is 757 lines covering 7 distinct feature areas in a single class.

## Target Structure

```
AppViewModel (~60 lines, coordinator)
├── ContextStore      — contexts CRUD, active mgmt, sub-contexts, favorites, namespace loading
├── ExplorerStore     — tree expand/collapse, namespace grouping, resource loading, search
├── PortForwardStore  — configs CRUD, process lifecycle, start/stop/kill-all
├── ResourceStore     — selected resource YAML, events, pod logs, pod metrics
└── LogsStore         — unified logs tabs, search, display settings
```

## File List

| # | File | Action | Lines |
|---|------|--------|-------|
| 1 | `ui/store/ContextStore.kt` | Create | ~150 |
| 2 | `ui/store/ExplorerStore.kt` | Create | ~130 |
| 3 | `ui/store/PortForwardStore.kt` | Create | ~90 |
| 4 | `ui/store/ResourceStore.kt` | Create | ~60 |
| 5 | `ui/store/LogsStore.kt` | Create | ~120 |
| 6 | `ui/AppViewModel.kt` | Rewrite | ~60 |
| 7 | `App.kt` | Update refs | |
| 8 | `ui/components/Sidebar.kt` | Update refs | |
| 9 | `ui/components/TopBar.kt` | Update refs | |
| 10 | `ui/pages/PortForwardPage.kt` | Update refs | |
| 11 | `ui/pages/ContextPage.kt` | Update refs | |
| 12 | `ui/pages/ResourceDetailPage.kt` | Update refs | |
| 13 | `ui/pages/LogsPage.kt` | Update refs | |

## Composable Access Pattern

| Before | After |
|--------|-------|
| `viewModel.contexts` | `viewModel.contexts.allContexts` |
| `viewModel.activeContexts` | `viewModel.contexts.activeContexts` |
| `viewModel.portForwardConfigs` | `viewModel.portForward.configs` |
| `viewModel.activePortForwardProcesses` | `viewModel.portForward.activeProcesses` |
| `viewModel.startPortForward(c, n)` | `viewModel.portForward.start(c, n, ctx)` |
| `viewModel.logTabs` | `viewModel.logs.tabs` |
| `viewModel.resourceYaml` | `viewModel.resource.yaml` |
| `viewModel.expandedContexts` | `viewModel.explorer.expandedContexts` |
| `viewModel.activeContext` | `viewModel.activeContext` (stays) |
| `viewModel.searchQuery` | `viewModel.searchQuery` (stays) |

## Cross-Store Coordination

1. **Active context**: Stays on `AppViewModel` as computed property from `contexts.activeContexts`
2. **Periodic refresh**: Timer in `AppViewModel` calls `contexts` for namespace refresh + `explorer` for resource refresh
3. **Context activation**: `AppViewModel.activateContext()` orchestrates `contexts` + `portForward` + `explorer`
4. **Resource selection**: `AppViewModel.selectResourceAndNamespace()` orchestrates `explorer` + `resource`
