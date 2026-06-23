# KubeKui - Status

## Overview
A Kubernetes context viewer and manager desktop application built with Kotlin Desktop (Compose Multiplatform) + GraalVM, using Material Design 3 and SQLite.

## Current Status: Resource Detail Page — Search & Copy (#2026-06-23)

### Implemented Features

#### Core Infrastructure
- [x] Kotlin Multiplatform project setup (shared + desktopApp modules)
- [x] SQLite database with JVM implementation (contexts, port_forward_configs, port_forward_processes, sub_contexts, favorite_namespaces tables)
- [x] Data models: KubeContext, SubContext, FavoriteNamespace, PortForwardConfig, PortForwardProcess, KubeResource, ResourceType, NamespaceInfo
- [x] Repository layer: ContextRepository (incl. getAllActive, deactivate, sub-context CRUD, favorites CRUD), PortForwardRepository
- [x] Kubectl client with JSON output parsing (namespaces, resources, YAML, events, logs, metrics, secrets, aggregated namespace logs)
- [x] Process manager with PID tracking, kill, and killAllProcesses
- [x] AppDependencies DI pattern via expect/actual

#### UI (Material Design 3)
- [x] Theme with light/dark mode support
- [x] TopBar with navigation (Resources, Contexts, Port Forward tabs) and search
- [x] Hierarchical sidebar tree: contexts → favorites → sub-contexts → namespaces → resource types → resources
  - [x] ★ Favorites section at top of each context listing starred namespaces
  - [x] Namespace star toggle (★/☆) to add/remove from favorites
  - [x] Favorite namespaces appear in both Favorites section and normal position
  - [x] Resizable sidebar with draggable divider (180dp–500dp range)
  - [x] Sub-context folders grouping namespaces by regex pattern (e.g. ^dev-.* → "Development")
  - [x] Unmatched namespaces shown directly under context below sub-context folders
  - [x] Context accordion + sub-context folder + namespace item expand/collapse
  - [x] Multiple simultaneous active contexts (toggle any context on/off independently)
  - [x] Resources grouped by type under each namespace (excluding NAMESPACES/NODES from tree)
  - [x] Resources sorted alphabetically within each type group
  - [x] Resource count badges on collapsed namespaces and sub-contexts
  - [x] Right-click context menu on contexts (activate/deactivate, edit, delete)
  - [x] Right-click context menu on resources (view details, port forward)
  - [x] Tree-aware search filtering (contexts, favorites, sub-contexts, namespaces, types, resources)
  - [x] Diff-based background refresh (10s polling, only updates tree on actual changes)
  - [x] Context color indicators in tree
- [x] Resource detail page with YAML, Events, Logs, Metrics tabs
  - [x] Per-tab search bar with next/prev match navigation and match counter
  - [x] Case-insensitive search highlighting (yellow bg for matches, orange for current)
  - [x] Scroll-to-match on search navigation (auto-scrolls to current match line)
  - [x] Native text selection via SelectionContainer (Cmd+C / right-click copy)
  - [x] Copy button (📋) in each content tab toolbar for one-tap full content copy
  - [x] Quick-copy icons (📋) next to resource name and namespace in ResourceHeader
  - [x] Snackbar notifications for copy confirmations ("Copied to clipboard" / errors)
  - [x] Search state preserved per-tab when switching between YAML/Events/Logs/Metrics
- [x] Context management page (CRUD with color picker, base port config, sub-namespace regex+display name editor, multi-active toggle)
- [x] Port forwarding page (CRUD configs, start/stop/kill-all processes, PID display)
- [x] Delete context confirmation dialog from sidebar right-click
- [x] Port forward dialog from sidebar resource right-click
- [x] Unified namespace logs view with:
  - [x] Tabbed interface for multiple namespace logs simultaneously
  - [x] Aggregated logs from all pods in a namespace (pod name headers)
  - [x] Search with next/previous match navigation and match count (refactored to shared SearchBar component)
  - [x] Search highlight in log text (current match in different color)
  - [x] Scroll-to-match on search navigation in logs
  - [x] Font size adjustment (+/- buttons, 9–24sp range)
  - [x] Text wrapping toggle
  - [x] Log level highlighting toggle (ERROR/WARN/DEBUG colors)
  - [x] Auto-scroll to bottom on new log content
  - [x] Manual refresh button
  - [x] Copy button (📋) for one-tap full log content copy
  - [x] Selectable text for clipboard copy (native SelectionContainer)
  - [x] Right-click "View Logs" on namespaces in tree view
  - [x] Logs icon (📄) on each namespace to open logs tab
  - [x] ✓/○ toggle indicators with primary/gray color for on/off state
  - [x] Compact control toolbar with smaller typography for better fit
- [x] Resizable sidebar divider with horizontal-resize cursor on hover
- [x] Reusable UI components extracted:
  - [x] SearchBar — controlled search input with prev/next/counter/clear (shared by ResourceDetailPage + LogsPage)
  - [x] CopyButton — one-tap clipboard copy with snackbar feedback (shared by ResourceDetailPage + LogsPage)
  - [x] HighlightedText — utilities for findAllMatches, buildHighlightedAnnotatedString, appendHighlightedLine
  - [x] Snackbar infrastructure — CompositionLocal-based SnackbarHostState for app-wide notifications

#### Process Management
- [x] Detached kubectl port-forward processes with PID tracking
- [x] Kill individual processes from UI
- [x] Kill all processes on app close
- [x] Process status tracking in database

### Tech Stack
- Kotlin 2.3.21
- Compose Multiplatform 1.11.0 (Material3)
- SQLite via sqlite-jdbc 3.49.1.0
- kotlinx-serialization-json 1.7.3
- kotlinx-coroutines 1.11.0

### Pending / Next Steps
- [ ] GraalVM native image configuration
- [ ] Auto-assign local ports to avoid conflicts
- [ ] Auto-stop port forwards after timeout
- [ ] kubectl context auto-discovery (list contexts from kubeconfig)
- [ ] Improved error handling and loading states
- [ ] Unit tests
- [ ] Context prefix appended on every kubectl command execution
- [ ] Resource metrics visualization
- [ ] Secret data decoding and display
