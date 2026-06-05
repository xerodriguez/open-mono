# KubeKui - Status

## Overview
A Kubernetes context viewer and manager desktop application built with Kotlin Desktop (Compose Multiplatform) + GraalVM, using Material Design 3 and SQLite.

## Current Status: Hierarchical Sidebar with Multi-Active Contexts

### Implemented Features

#### Core Infrastructure
- [x] Kotlin Multiplatform project setup (shared + desktopApp modules)
- [x] SQLite database with JVM implementation (contexts, port_forward_configs, port_forward_processes tables)
- [x] Data models: KubeContext, PortForwardConfig, PortForwardProcess, KubeResource, ResourceType, NamespaceInfo
- [x] Repository layer: ContextRepository (incl. getAllActive, deactivate), PortForwardRepository
- [x] Kubectl client with JSON output parsing (namespaces, resources, YAML, events, logs, metrics, secrets)
- [x] Process manager with PID tracking, kill, and killAllProcesses
- [x] AppDependencies DI pattern via expect/actual

#### UI (Material Design 3)
- [x] Theme with light/dark mode support
- [x] TopBar with navigation (Resources, Contexts, Port Forward tabs) and search
- [x] Hierarchical sidebar tree: contexts → namespaces → resource types → resources
  - [x] Two-level expand/collapse (context accordion + namespace click-to-select) — no excessive nesting
  - [x] Multiple simultaneous active contexts (toggle any context on/off independently)
  - [x] Resources grouped by type under each namespace (excluding NAMESPACES/NODES from tree)
  - [x] Resources sorted alphabetically within each type group
  - [x] Resource count badges on collapsed namespaces
  - [x] Right-click context menu on contexts (activate/deactivate, edit, delete)
  - [x] Right-click context menu on resources (view details, port forward)
  - [x] Tree-aware search filtering (contexts, namespaces, types, resources)
  - [x] Diff-based background refresh (10s polling, only updates tree on actual changes)
  - [x] Context color indicators in tree
- [x] Resource detail page with YAML, Events, Logs, Metrics tabs
- [x] Context management page (CRUD with color picker, base port config, multi-active toggle)
- [x] Port forwarding page (CRUD configs, start/stop/kill-all processes, PID display)
- [x] Delete context confirmation dialog from sidebar right-click
- [x] Port forward dialog from sidebar resource right-click

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
