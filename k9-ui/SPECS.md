# kube kui
The application is a Kubernetes context viewer and manager. It allows users to view and manage their Kubernetes contexts, namespaces, and resources in a tree view. It also allows users to view resource details, YAML, events, logs, metrics, pods, containers, images, volumes, nodes, namespaces, services, deployments, statefulsets, daemonsets, and jobs in a table view.

## Tech stack
- Kotlin 2.3 
- SQLite 

Kotlin + GraalVM

The goal is to have a minimal and efficient application that can run on desktop devices. The application will be built using Kotlin and will leverage the power of GraalVM to create a native executable for desktop platforms. The use of SQLite will allow for efficient storage and retrieval of Kubernetes context data.

## UI
The UI is built using Material Design 3. It is designed to be simple and intuitive, with a focus on usability and accessibility. The UI is responsive and works well on desktop devices.

The UI must be clean and simple.

Layout:
- Left sidebar:  Tree view of Kubernetes contexts, namespaces, and resources.
- Main content area: Table view of resource details, YAML, events, logs, metrics,
- Top bar: Search bar, filter options, and action buttons.

## Features

### kubernetes context 
Have a page view to configure all the kubernetes contexts. The user can access this page from the top bar. The user can add, edit, and delete contexts. The user can also switch between contexts.

Each context will have the following information:
- Name
- context 
- color 
- port forwarding base port 
- sub-contexts (optional)

Color is important to differentiate between contexts in the tree view. The user can choose a color for each context to make it easier to identify them.

Context will be stored in the SQLite database and will be loaded when the application starts. 
The context must be append on every `kubectl` command execution. This will allow the user to have a history of the contexts they have used and easily switch between them.

Each context can have an optional list of sub-contexts. This will allow the user to group related contexts together and easily switch between them. For example, a user might have a group of contexts for different environments (e.g., development, staging, production) and can easily switch between them.

There could be more than one active context at the same time, the application will allow the user to switch between them and view their resources in the tree view. The user can also filter the resources in the tree view by context to only show the resources for a specific context.

### port forwarding
The application will have a port forwarding feature that allows users to forward ports from their local machine to their Kubernetes cluster. This will enable users to access services running in their cluster from their local machine.
The user can persist the port forwarding configuration in the SQLite database. This will allow the user to have a history of the port forwarding configurations they have used and easily switch between them.
The user can also view the status of their port forwarding configurations and stop them when they are no longer needed.
The are several pods running on the same port on the cluster, the application will automatically assign a different local port for each port forwarding configuration to avoid conflicts. The user can also specify a custom local port if they prefer, this preference must be saved in the database for future use.
The application will also have a feature to automatically stop port forwarding configurations after a certain period of time or when the user closes the application to prevent orphaned port forwarding configurations.
The application will have a page to show all the active port forwarding configurations, where the user can view the details of each configuration, such as the local port, remote port, and status. The user can also stop or start any port forwarding configuration from this page.

### Left navigation sidebar

#### Tree view of Kubernetes contexts, namespaces, and resources

The left navigation sidebar will display an accordion for each active context with its namespaces and resources. The user can expand each context to see its namespaces and resources. The user can also search for specific contexts, namespaces, or resources using the search bar in the top bar.

The tree view will be organized in a hierarchical manner, with contexts at the top level, followed by namespaces, and then resources. The user can click on any context, namespace, or resource to view its details in the main content area. The user can also right-click on any context, namespace, or resource to access additional actions such as editing, deleting, or port forwarding.

The tree view will also have a filter option that allows the user to filter the contexts, namespaces, and resources based on specific criteria such as name, type, or status. This will help the user quickly find the context, namespace, or resource they are looking for.

The tree must group resources by type (e.g., pods, services, deployments) under each namespace to make it easier for users to navigate and find specific resources. This grouping will also allow users to quickly identify the types of resources available in each namespace and manage them more efficiently.

Inside the grouping of resources by type, the resources will be sorted alphabetically to make it easier for users to find specific resources. This sorting will help users quickly locate the resource they are looking for, especially when there are many resources of the same type in a namespace. 

The tree view should be updated in real-time to reflect any changes in the Kubernetes cluster, such as the addition or deletion of contexts, namespaces, or resources. This will ensure that users always have an up-to-date view of their Kubernetes environment and can manage their resources effectively.

#2026-06-10
## Context view and Tree view 
In context add property to each context to add a list of sub-namespaces. To describe the 
sub-namespace I want to use two strings for each sub-namespace. This first string is a regular expression that will be used to match each sub-namespace 
to the context, the second string is a name that will be used to display the sub-namespace in 
the tree view. This will allow the user to group related namespaces together and easily switch between them. For example, a user might have a group of namespaces for different environments (e.g., development, staging, production) and can easily switch between them. 

Provide some samples of the sub-namespace configuration for different contexts and the 
sub-namespace configuration that could be a prefix or a suffix of the namespace name. For example, if the user has a context for development and wants to group all namespaces that start with "dev-" under that context, they could use the following sub-namespace configuration:
- Regular expression: `^dev-.*`
- Display name: `Development`

If the user has a context for staging and wants to group all namespaces that end with "-staging" under that context, they could use the following sub-namespace configuration:
- Regular expression: `.*-staging$`
- Display name: `Staging`

### Tree view 
On the tree view group sub-namespaces under the context and display the sub-namespace name instead of the actual namespace name. This will allow the user to easily identify which namespaces belong to which contexts and manage them more efficiently. For example, if a user has a context for development with a sub-namespace configuration that matches all namespaces that start with "dev-", the tree view will group all those namespaces under the development context and display them with the sub-namespace name "Development" instead of their actual namespace names. This will make it easier for users to navigate and manage their Kubernetes resources based on their contexts and environments.

## Implementation Notes (#2026-06-10)

### Data Model
- `SubContext` data class: `id`, `contextId`, `regexPattern`, `displayName`
- Sub-contexts stored in separate `sub_contexts` table (not embedded in KubeContext)

### Database
- New table: `sub_contexts (id, context_id, regex_pattern, display_name)` with FK CASCADE on context delete
- `PRAGMA foreign_keys = ON` enabled on connect
- CRUD methods: `getAllSubContexts`, `getSubContexts(contextId)`, `insertSubContext`, `updateSubContext`, `deleteSubContext`, `deleteSubContextsForContext(contextId)`

### Tree Structure
New hierarchy: `Context → SubContext folder → Namespace → ResourceType → Resource`
- Sub-context folders are expandable/collapsible (tracked by `expandedSubContexts: Set<String>` keyed `"$contextId/$subContextId"`)
- Namespaces not matching any sub-context regex appear below all sub-context folders as standalone items
- Sub-context folder tinted with context color at reduced alpha
- Search filtering includes sub-context display names

### Context Page
- Context dialog includes sub-namespace editor section below base port field
- Each sub-namespace entry: regex input + display name input + remove button
- "+ Add Sub-Namespace" button to add new rows
- Sub-contexts synced on save: delete all for context, re-create from dialog list

#2026-06-11
### Tree View 
* Tree view has to be resizable. Allow the user to adjust the width of the tree view by dragging the divider between the tree view and the main content area.
* Group by favorite, the user can mark any namespace as a favorite and it will be grouped under 
  a "Favorites" section at the top of the context of the tree view. This will allow the user to quickly access their most frequently used namespaces without having to search for them in the tree view. The user can also unmark a namespace as a favorite to remove it from the "Favorites" section. This feature will help users manage their Kubernetes resources more efficiently by providing quick access to their most important namespaces.
* Favorite namespaces appear in both the Favorites section AND their normal position (sub-context or unmatched). Sub-contexts remain exclusive (a namespace belongs to only one sub-context).

## Implementation Notes (#2026-06-11)

### Resizable Tree View
- Sidebar `.width(280.dp)` removed from Sidebar.kt; width now controlled from App.kt
- `sidebarWidthDp` (mutableFloatStateOf, default 280f) managed in App.kt
- 4dp-thick draggable divider between sidebar and content using `detectHorizontalDragGestures`
- Width clamped between 180dp and 500dp; density-aware drag delta conversion
- Content area uses `Modifier.weight(1f).fillMaxHeight()` to fill remaining space

### Favorites
- New `FavoriteNamespace` data class: `id`, `contextId`, `namespace`
- New table: `favorite_namespaces (id, context_id, namespace)` with FK CASCADE + UNIQUE(context_id, namespace)
- Insert uses `INSERT OR IGNORE` to handle duplicate toggle attempts
- `deleteContext` cascades to favorite_namespaces
- ViewModel: `favoriteNamespacesByContextId: Map<Long, Set<String>>` — loaded via `loadFavorites()`
- `toggleFavorite(contextId, namespace)` — add/remove via repository
- `isFavorite(contextId, namespace)` — O(1) lookup from state map
- `getFavoriteNamespacesForContext(contextId)` — returns set of namespace names

### Tree Structure
New hierarchy: `Context → ★ Favorites → SubContext folders → Unmatched namespaces → ...`
- `FavoritesSection` composable (similar to SubContextFolder): expandable, shows ★ icon in context color, lists favorite namespaces as `NamespaceItem`s
- Each `NamespaceItem` has a clickable star (★ filled for favorited, ☆ outline for not) to toggle
- Favorites section only rendered when favorites exist for that context
- Namespace appears in both Favorites and normal position (non-exclusive)

#2026-06-11_v2
### Tree View
- Convert pointer icon to horizontal resize when hovering over divider between tree view and 
  content area for better UX feedback. Use `pointerHoverIcon` modifier on divider with `PointerIcon(Cursor(Cursor.H_RESIZE))`
- Namespace logs. Put an icon on each namespace to show the unified logs view. Clicking the icon 
  will open logs view for all pods in the namespace. This will allow the user to quickly access the logs for all pods in a namespace without having to open each pod individually. The logs view will aggregate logs from all pods in the namespace and display them in a unified view, making it easier for users to monitor and troubleshoot their applications running in Kubernetes.

### Unified Logs View
- Search unified logs. Add a search bar in the unified logs view to allow users to filter logs across all pods in the namespace. This will help users quickly find relevant log entries when dealing with large volumes of logs from multiple pods. The search functionality should support basic text search and possibly regex for advanced filtering, making it easier for users to pinpoint specific events or issues in their Kubernetes environment.
- Unified logs view should be displayed on tabs, I want to see several tabs for different namespaces at the same time. This will allow users to monitor logs from multiple namespaces simultaneously, which is especially useful in environments where multiple applications or services are running across different namespaces. Each tab will represent a different namespace's unified logs view, enabling users to easily switch between them and keep an eye on the logs that matter most to them without losing context.
- Add a "View Logs" option to the right-click context menu for namespaces in the tree view. This will provide users with a convenient way to access the unified logs view for a specific namespace directly from the tree view. When a user right-clicks on a namespace, they can select "View Logs" from the context menu, which will open the unified logs view for that namespace in a new tab. This feature will enhance the usability of the application by allowing users to quickly access important log information without having to navigate through multiple steps.
- Unified logs view should allow to resize the log viewer area and the text should wrap properly. 
- Logs should autoupdate and scroll to bottom when new logs arrive. 
- Search should work backwards and forwards with buttons to jump to next/previous match.
- Search should allow to search on logs not retrieved yet, by fetching more logs from the server until a match is found or no more logs are available. This will ensure that users can find relevant log entries even if they are not currently loaded in the viewer, providing a more comprehensive search experience across all available logs for the namespace.
- Make the view user friendly by allowing users to customize the log viewer's appearance, such as changing the font size, color scheme, and enabling/disabling log level highlighting. This will help users tailor the log viewing experience to their preferences and make it easier to read and analyze logs, especially when dealing with large volumes of log data from multiple pods in a namespace. Customization options will enhance the overall usability of the unified logs view and allow users to focus on the information that matters most to them.
- Allow to copy all logs or a selected portion of the logs to the clipboard. This will enable users to easily share log information with colleagues or paste it into other tools for further analysis. Providing a simple way to copy logs will enhance the usability of the unified logs view and facilitate collaboration when troubleshooting issues in Kubernetes environments.

## Implementation Notes (#2026-06-11_v2)

### Divider Cursor
- `expect fun cursorHorizontalResize(): PointerIcon?` in Platform.kt
- `actual` in Platform.jvm.kt uses `PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))`
- Applied via `Modifier.pointerHoverIcon()` on the 4dp divider Box in App.kt

### Kubectl Client
- New method: `getNamespacePodLogs(context, namespace, tail=500)` — fetches all pods in namespace, then `kubectl logs` for each pod sequentially, aggregated with `=== podName ===` headers

### Unified Logs View
- New `AppView.LOGS` enum value
- TopBar "Logs" button navigates to LogsPage
- New `LogsPage.kt` composable with `ScrollableTabRow` for multiple namespace tabs
- Each tab shows namespace name, loading spinner, close button
- Log viewer area:
  - Search bar with next (▲) / prev (▼) buttons and match count (n/N)
  - Toggle buttons: Auto-scroll, Wrap text, Highlight levels
  - Font size: A⁻ / A⁺ buttons (9–24sp)
  - Refresh button (↻)
  - `SelectionContainer` + `Text` for native text selection and clipboard copy
- `buildLogAnnotatedString()`: highlights log levels (ERROR=red, WARN=orange, DEBUG=blue) and search matches (yellow background, current=orange)
- Auto-scroll via `LaunchedEffect` watching log content

### ViewModel Log State
- `LogTabState` data class: contextName, kubectlContext, namespace, logContent, isLoading
- `logTabs`, `activeLogTabIndex` — tab management
- `openLogsTab()` — opens existing tab or creates new, triggers log fetch
- `closeLogsTab()`, `setActiveLogTab()` — tab navigation
- `refreshActiveLogTab()` — manual refresh
- `logSearchQuery`, `logSearchMatches`, `activeLogSearchMatchIndex` — search state
- `searchLogsNext()`, `searchLogsPrev()` — match navigation (wraps around)
- `updateLogSearchMatches()` — finds all case-insensitive positions in current log
- `logFontSize`, `logAutoScroll`, `logWrapText`, `logHighlightLevel` — display toggles

### Sidebar Changes
- NamespaceItem now has right-click context menu with "View Logs" option
- 📄 logs icon on each namespace row, clickable to open logs tab
- Both icon click and right-click menu navigate to AppView.LOGS

### NOT Implemented (pending)
- Searching on logs not yet fetched from server
- Auto-refresh timer (currently manual refresh only)
- Color scheme customization beyond light/dark theme

### UI Polish (post-#2026-06-11_v2)
- Search field text and placeholder use `bodySmall` typography for proper fit
- Toggle buttons use ✓ checkmark prefix with primary color (on) vs gray (off) instead of confusing √ suffix
- Font size buttons use plain −/+ text instead of unicode superscripts that rendered poorly
- Font size label shows "13px" format for clarity
- Clear button in search field uses plain clickable × instead of TextButton to avoid padding issues
- All control toolbar texts use `labelSmall`/`labelLarge` for compact sizing
- Search next/prev and refresh buttons use `labelSmall` typography

#2026-06-23
### Resource Detail Page — Search & Copy

#### Text Selection (Native Keyboard Shortcuts)
- All content tabs (YAML, Events, Logs, Metrics) must support native text selection. Replace plain `Text` in `YamlView` with a `SelectionContainer` wrapping the monospace `Text`.
- `SelectionContainer` on Compose Desktop automatically handles Cmd+C (copy), Cmd+V (paste), and mouse-based select-all/cut operations. No additional keyboard handling is needed — the platform provides these shortcuts out of the box when text is inside a `SelectionContainer`.
- Users can highlight any range and use Cmd+C to copy to clipboard, or right-click for the native context menu with Copy.

#### In-Content Search (UI Convenience)
- Add a search bar at the top of each content tab with:
  - Text input for case-insensitive query
  - Next (▲) / Previous (▼) buttons and match counter `n/N`
  - Clear button (×) to dismiss query and highlight
- All matching substrings highlighted with background color (yellow in light theme, dark yellow/orange in dark). Current match gets a distinct border/accent.
- Search wraps around on navigation. Operates on currently loaded content only (static — no server fetching).

#### Copy Button (One-Tap Full Content)
- "Copy" button (📋 icon) in each tab's toolbar that copies the entire tab content to clipboard when no text is selected.
- If text IS selected, the button copies only the selected portion (use `LocalClipboard` with the selection range).
- Snackbar confirmation on success ("Copied to clipboard"), error Snackbar on failure.

#### Resource Header Quick Copy
- Small copy icon (📋) next to the resource name and namespace in `ResourceHeader` for one-tap copying of that specific value.
- Shows Snackbar confirmation on copy.

## Implementation Notes (#2026-06-23)

### SelectionContainer (Native Cmd+C)
- Wrap `YamlView` content: `SelectionContainer { Text(text = ..., fontFamily = FontFamily.Monospace, ...) }`.
- Compose Desktop's `SelectionContainer` natively supports Cmd+C, Cmd+V, Cmd+A, and right-click context menu — no custom key listeners required.
- Import: `androidx.compose.foundation.selectioncontainer`.

### Clipboard (Copy Button)
- Use `LocalClipboard` from `androidx.compose.ui.platform.LocalClipboard`.
- For full-content copy: `clipboardManager.setText(AnnotatedString(content))`.
- For selected-text copy via button: read selection state from `SelectionContainer` or use a separate remembered selection range tracked by the composable.

### Search Implementation
- View-local state in `ResourceDetailPage` (not ViewModel):
  - `searchQuery: String`, `searchMatches: List<Int>`, `activeMatchIndex: Int`
  - `buildHighlightedAnnotatedString(content, query, matches, activeIndex)` — returns `AnnotatedString` with background spans for each match.
- Replace plain `Text` in `YamlView` with: `SelectionContainer { Text(annotatedString, fontFamily = FontFamily.Monospace, ...) }`.
- Search bar UI mirrors existing pattern from Unified Logs View (next/prev buttons, match count).

### Copy Button Placement
- Card toolbar area: copy icon button in top-right corner of each content card.

### Snackbar
- Add `SnackbarHost` to the page layout via `ModalNavigationManager` or top-level `Scaffold`.
- Use `SnackbarHostState.showSnackbar()` for copy confirmations.

### ResourceHeader Quick Copy
- Inline `IconButton` with copy icon next to each field value.
- Captures the string value and calls `clipboardManager.setText()` directly.

### NOT Implemented (pending)
- Parsing structured values from YAML raw text (e.g., extracting port numbers, IPs automatically)
- Search across multiple tabs simultaneously
