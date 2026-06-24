# Port Forwarding Implementation Plan

## Feature 1: Auto-assign local ports to avoid conflicts (SPECS.md:47)

**Current state:** Auto-port = `basePort + remotePort` with zero conflict detection. Two configs with the same remote port collide.

### Changes needed:

| # | File | Change |
|---|------|--------|
| 1 | `JvmDatabase.kt` | Add `UNIQUE(local_port)` constraint on `port_forward_configs` table (or skip — prefer in-memory check) |
| 2 | `PortForwardRepository.kt` | Add `getUsedLocalPorts(): Set<Int>` — returns all local ports from active processes + saved configs |
| 3 | `AppViewModel.kt` | Add `resolveAutoPort(contextId, remotePort): Int` — scans used ports starting from `basePort + remotePort`, returns next available |
| 4 | `AppViewModel.kt` | Update `addPortForwardConfig()` to resolve auto-port before creating config |
| 5 | `PortForwardPage.kt` (dialog) | When "Auto" selected, call `resolveAutoPort()` and display resolved port (not just `basePort + remotePort`) |
| 6 | `App.kt` (`SidebarPortForwardDialog`) | Same — resolve auto-port when user hasn't entered a custom port. Remove hardcoded `customLocalPort = true` |
| 7 | `AppViewModel.kt` — `startPortForward()` | Pre-flight check: reject if resolved port already in use by another active process (show error toast) |

### Algorithm for `resolveAutoPort`:
```
start = basePort + remotePort
candidate = start
while candidate in usedPorts: candidate++
return candidate
```

**Tradeoff:** Bounded (max 100 attempts) or unbounded? Recommended: bounded with "no available ports" error if exhausted.

---

## Feature 2: Auto-stop port forwards after timeout (SPECS.md:48)

**Current state:** `startedAt` is stored but never checked. No timeout field on model or DB. Unused `PORT_FORWARD_TIMEOUT_MS = 5000L` constant is dead code.

### Changes needed:

| # | File | Change |
|---|------|--------|
| 1 | `PortForwardProcess` model | Add `timeoutSeconds: Long = 0` (0 = no timeout / infinite) |
| 2 | `JvmDatabase.kt` | Add `timeout_seconds INTEGER NOT NULL DEFAULT 0` to `port_forward_processes` table (additive, no migration needed) |
| 3 | `PortForwardRepository.kt` | Add `updateTimeout(processId, timeoutSeconds)` and `getExpiredProcesses(): List<PortForwardProcess>` (WHERE `is_running = 1 AND timeout_seconds > 0 AND started_at + timeout_seconds * 1000 < now`) |
| 4 | `AppViewModel.kt` | Add `checkExpiredPortForwards()` — calls repo, kills expired processes via existing `stopPortForward` path |
| 5 | `AppViewModel.kt` — lifecycle timer (lines 95-102) | Extend existing `repeat(10_000)` coroutine to also call `checkExpiredPortForwards()` every 60s |
| 6 | `PortForwardPage.kt` (dialog + config card) | Add timeout input: numeric field with presets (15min, 30min, 1hr, 4hr, Never). Store on process creation. |
| 7 | `PortForwardPage.kt` (ActiveProcessCard) | Show remaining time countdown when timeout > 0. Format: "Expires in 23m" or "Expired". |
| 8 | `App.kt` (`SidebarPortForwardDialog`) | Pass timeout to process creation (default: 30 min or "Never") |

### Design decisions:
- Store `timeoutSeconds` on the **process** record (not config) — timeout is per-instance, a single config may be started/stopped multiple times
- Default: 30 minutes, "Never" (0) as an option

---

## Shared considerations

- Both features touch `AppViewModel.kt` and the port-forward dialog — coordinate to avoid conflicts
- The existing 10s polling loop in `AppViewModel` (lines 95-102) can be extended for timeout checking rather than adding a new coroutine
- No database migration needed — both changes are additive (new column with default, no schema breakage)
- `PortForwardProcess` model is serializable and used in state — adding a field requires default value for backward compatibility

---

## Open questions

1. **Auto-port bounded scan?** Max 100 attempts above basePort, or unbounded?
2. **Default timeout value?** 30 minutes seems reasonable — confirm or propose different
3. **Where to store default timeout?** Per-context on `KubeContext` (like base port), or global app setting?
