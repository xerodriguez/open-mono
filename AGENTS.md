## Project Overview
Mono-repo. The only active project is `k9-ui/`, a Kotlin Desktop (Compose Multiplatform) app for managing Kubernetes contexts.

## Naming
| Context | Name |
|---|---|
| Directory | `k9-ui/` |
| Gradle root project | `KlusterUi` |
| Kotlin package | `com.fgsoft.klusterui` |
| App window title | `KubeKui` |

## Commands (run from `k9-ui/`)
./gradlew :desktopApp:hotRun --auto   # run with hot reload
./gradlew :desktopApp:run             # standard run
./gradlew :shared:jvmTest             # run tests

## Architecture
Two Gradle modules:
- `:shared` — KMP library (JVM only, despite KMP structure)
- `:desktopApp` — JVM app entry point (`main.kt`)

Single ViewModel (`AppViewModel`) owns all state. Nav via `AppView` enum (no navigation lib).
DI via `expect`/`actual`: commonMain declares `createAppDependencies()`, jvmMain wires JVM impls.
Repository pattern over SQLite at `~/.klusterui/klusterui.db`.
All cluster ops shell out to `kubectl` via `ProcessBuilder`. Port-forward processes tracked by PID.

## Conventions
- `k9-ui/SPECS.md` is the source of truth for requirements. Refer to it for all feature work.
- `k9-ui/STATUS.md` must be kept updated with feature progress as you implement.
- `kotlin.code.style=official` (enforced in `gradle.properties`)
- No meaningful tests exist — only placeholder assertions in `shared/src/jvmTest/`
- `local.properties` (not version-controlled) points to Android SDK
