# Implementation Detail — #2026-06-23 Resource Detail Page Search & Copy

## Overview
Add in-content search, text selection, clipboard copy, and snackbar notifications to ResourceDetailPage. Extract reusable components (SearchBar, CopyButton, HighlightedText) and retrofit LogsPage to use them. Add scroll-to-match on search navigation.

## Architecture

### New Reusable Components (4 files)

| File | Type | Purpose |
|---|---|---|
| `ui/components/SnackbarState.kt` | CompositionLocal | `LocalSnackbarHostState` for app-wide snackbar access |
| `ui/components/SearchBar.kt` | @Composable | Controlled search bar: input + prev/next + counter + clear |
| `ui/components/CopyButton.kt` | @Composable | 📋 icon button: copies content to clipboard, shows snackbar |
| `ui/components/HighlightedText.kt` | Utility object | `findAllMatches()`, `buildHighlightedAnnotatedString()`, `appendHighlightedLine()` |

### Modified Files (3 files)

| File | Change |
|---|---|
| `App.kt` | Add SnackbarHost overlay, provide `LocalSnackbarHostState` |
| `ui/pages/ResourceDetailPage.kt` | Add `ContentCard` (search+selection+copy per tab), scroll-to-match, ResourceHeader quick copy |
| `ui/pages/LogsPage.kt` | Replace inline search with shared `SearchBar`, add `CopyButton`, add scroll-to-match, delegate to `HighlightedText` |

## Component Details

### SearchBar (controlled, stateless)
```
SearchBar(query, onQueryChange, matches, activeMatchIndex, onNext, onPrev, onClear, placeholder, modifier)
```
- `OutlinedTextField` + prev ▲ / next ▼ `TextButton`s + "n/N" counter + × clear
- Mirrors existing LogsPage search pattern exactly
- Stateless — all state owned by caller (ResourceDetailPage: local `TabSearchState`; LogsPage: ViewModel)

### CopyButton
```
CopyButton(content: String, modifier: Modifier)
```
- `IconButton` with 📋 icon
- `LocalClipboard.current.setText(AnnotatedString(content))`
- `snackbarHostState.showSnackbar("Copied to clipboard")`
- Error handling: catches exceptions, shows error snackbar

### HighlightedText (utility)
```kotlin
fun findAllMatches(content: String, query: String): List<Int>
fun buildHighlightedAnnotatedString(content, query, matches, activeIndex, matchBg, currentMatchBg): AnnotatedString
fun AnnotatedString.Builder.appendHighlightedLine(line, query, currentMatchPos, matchBg, currentMatchBg)
```
- `findAllMatches` — case-insensitive, returns all start positions
- `buildHighlightedAnnotatedString` — full-string highlighting for ResourceDetailPage tabs
- `appendHighlightedLine` — per-line builder for LogsPage (line-level coloring wraps search highlights)

### SnackbarState (infrastructure)
```kotlin
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState") }
```

## ResourceDetailPage Changes

### New: `ContentCard` composable
Replaces `YamlView` for all 4 tabs. Structure:
```
Card(12dp rounded)
├── Row (toolbar): SearchBar + Spacer(weight) + CopyButton
├── HorizontalDivider
└── SelectionContainer
    └── Box(horizontalScroll + verticalScroll + 16dp padding)
        └── Text(annotatedString, FontFamily.Monospace, bodySmall, onTextLayout=...)
```

### New: `TabSearchState` data class
```kotlin
data class TabSearchState(query: String, matches: List<Int>, activeMatchIndex: Int)
```
Stored in `mutableStateMapOf<String, TabSearchState>()` keyed by tab name. Persists across tab switches.

### Scroll-to-match
- `LaunchedEffect` keyed on `(activeMatchIndex, textLayoutResult)`
- Uses `TextLayoutResult.getLineForOffset()` + `getLineTop()` to find match line
- `scrollState.animateScrollTo()` to smoothly scroll to match position

### ResourceHeader quick copy
- 📋 `IconButton` next to resource name and namespace text
- Copies the specific string value, shows snackbar

## LogsPage Changes

### Search refactor
- Replace inline `OutlinedTextField` + prev/next `TextButton`s + counter with shared `SearchBar`
- Wire SearchBar params to ViewModel state (`logSearchQuery`, `logSearchMatches`, etc.)
- ~30 lines removed, deduplicated

### CopyButton addition
- Add `CopyButton(content = tab.logContent)` in the control toolbar row

### Scroll-to-match addition
- Add `onTextLayout` to the log `Text` composable
- `LaunchedEffect` keyed on `(activeLogSearchMatchIndex, textLayoutResult)`
- Same scroll-to-match logic as ResourceDetailPage

### HighlightedText integration
- LogsPage keeps `buildLogAnnotatedString` for log-level colors
- Delegates per-line search highlighting to `HighlightedText.appendHighlightedLine()`
- Removes duplicate `appendHighlightedLine` private function from LogsPage

## App.kt Changes

### Snackbar infrastructure
- `val snackbarHostState = remember { SnackbarHostState() }`
- Wrap existing `Column` in `Box` with `SnackbarHost` at `Alignment.BottomCenter`
- `CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) { ... }`

## File Creation/Modification Order
1. `SnackbarState.kt` (create)
2. `CopyButton.kt` (create)
3. `SearchBar.kt` (create)
4. `HighlightedText.kt` (create)
5. `App.kt` (modify)
6. `ResourceDetailPage.kt` (modify)
7. `LogsPage.kt` (modify)
8. `STATUS.md` (modify)
