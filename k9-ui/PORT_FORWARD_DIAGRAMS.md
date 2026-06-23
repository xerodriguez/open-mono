# Port Forwarding — Architecture & Sequence Diagrams

## 1. Class Diagram

```mermaid
classDiagram
    direction TB

    class AppViewModel {
        +portForwardConfigs: List~PortForwardConfig~
        +activePortForwardProcesses: List~PortForwardProcess~
        +showPortForwardDialog: KubeResource?
        +loadPortForwardConfigs()
        +addPortForwardConfig(config)
        +updatePortForwardConfig(config)
        +deletePortForwardConfig(id)
        +startPortForward(config, podName)
        +stopPortForward(processId)
        +stopPortForwardByConfigId(configId)
        +refreshActiveProcesses()
        +killAllPortForwards()
    }

    class AppDependencies {
        +portForwardRepository: PortForwardRepository
        +processManager: ProcessManager
    }

    class PortForwardConfig {
        +id: Long
        +contextId: Long
        +namespace: String
        +resourceType: String
        +resourceName: String
        +remotePort: Int
        +localPort: Int
        +customLocalPort: Boolean
        +label: String
    }

    class PortForwardProcess {
        +id: Long
        +configId: Long
        +localPort: Int
        +remotePort: Int
        +podName: String
        +namespace: String
        +pid: Long
        +isRunning: Boolean
        +startedAt: Long
    }

    class KubeContext {
        +portForwardBasePort: Int
    }

    class PortForwardRepository {
        +getAllConfigs()
        +getConfigsForContext(contextId)
        +createConfig(config)
        +updateConfig(config)
        +deleteConfig(id)
        +createProcess(process)
        +updateProcess(process)
        +getActiveProcesses()
        +getProcessesForConfig(configId)
        +deleteProcess(id)
    }

    class ProcessManager {
        <<interface>>
        +startPortForward(context, namespace, resourceType, resourceName, localPort, remotePort, onOutput, onError) PortForwardHandle
        +killProcess(handle)
        +killProcessByPid(pid)
        +killAllProcesses()
    }

    class PortForwardHandle {
        +processId: Long
        +localPort: Int
        +remotePort: Int
    }

    class JvmProcessManager {
        -activeProcesses: Map~Long, Process~
        +startPortForward(...)
        +killProcessByPid(pid)
        +killAllProcesses()
    }

    class Database {
        <<interface>>
        +getAllPortForwardConfigs()
        +getPortForwardConfigsForContext(contextId)
        +insertPortForwardConfig(config)
        +updatePortForwardConfig(config)
        +deletePortForwardConfig(id)
        +insertPortForwardProcess(process)
        +updatePortForwardProcess(process)
        +getAllActiveProcesses()
        +getProcessesForConfig(configId)
        +deletePortForwardProcess(id)
    }

    class JvmDatabase {
        -port_forward_configs table
        -port_forward_processes table
    }

    class PortForwardPage {
        <<Composable>>
    }

    class PortForwardDialog {
        <<Composable>>
    }

    class SidebarPortForwardDialog {
        <<Composable>>
    }

    class Sidebar {
        <<Composable>>
        +right-click context menu
    }

    AppViewModel --> AppDependencies
    AppDependencies --> PortForwardRepository
    AppDependencies --> ProcessManager
    PortForwardRepository --> Database
    ProcessManager <|.. JvmProcessManager
    Database <|.. JvmDatabase
    ProcessManager --> PortForwardHandle

    AppViewModel ..> PortForwardConfig : manages
    AppViewModel ..> PortForwardProcess : manages
    PortForwardProcess --> PortForwardConfig : configId FK
    PortForwardConfig --> KubeContext : contextId FK

    PortForwardPage --> AppViewModel
    PortForwardPage --> PortForwardDialog
    AppViewModel --> SidebarPortForwardDialog
    Sidebar --> AppViewModel

    JvmDatabase --> PortForwardConfig : persists
    JvmDatabase --> PortForwardProcess : persists
```

---

## 2. Sequence: Create Config (from PortForwardPage)

```mermaid
sequenceDiagram
    actor User
    participant PortForwardPage
    participant PortForwardDialog
    participant AppViewModel
    participant PortForwardRepository
    participant Database
    participant JvmDatabase
    participant SQLite

    User->>PortForwardPage: clicks "+ Add Forward"
    PortForwardPage->>PortForwardDialog: showDialog = true
    PortForwardDialog-->>User: renders form (namespace, resource type, name, ports, label)

    User->>PortForwardDialog: fills fields & clicks "Save"
    PortForwardDialog->>PortForwardDialog: validates fields
    PortForwardDialog->>PortForwardDialog: creates PortForwardConfig object
    PortForwardDialog->>AppViewModel: onSave(config)
    Note over PortForwardDialog: id=0 (new), contextId set, localPort = auto (basePort+remotePort) or custom

    alt New config (id == 0)
        AppViewModel->>AppViewModel: addPortForwardConfig(config)
        AppViewModel->>PortForwardRepository: createConfig(config)
        PortForwardRepository->>Database: insertPortForwardConfig(config)
        Database->>JvmDatabase: SQL INSERT INTO port_forward_configs
        JvmDatabase->>SQLite: INSERT ... RETURNING id
        SQLite-->>JvmDatabase: new row id
        JvmDatabase-->>Database: new id
        Database-->>PortForwardRepository: id
        PortForwardRepository-->>AppViewModel: id
    else Edit existing (id > 0)
        AppViewModel->>AppViewModel: updatePortForwardConfig(config)
        AppViewModel->>PortForwardRepository: updateConfig(config)
        PortForwardRepository->>Database: updatePortForwardConfig(config)
        Database->>JvmDatabase: SQL UPDATE port_forward_configs
        JvmDatabase->>SQLite: UPDATE ... WHERE id = ?
    end

    AppViewModel->>AppViewModel: loadPortForwardConfigs()
    AppViewModel->>PortForwardRepository: getConfigsForContext(activeContext.id)
    PortForwardRepository->>Database: getPortForwardConfigsForContext(contextId)
    Database->>JvmDatabase: SELECT FROM port_forward_configs WHERE context_id = ?
    JvmDatabase->>SQLite: query
    SQLite-->>JvmDatabase: rows
    JvmDatabase-->>AppViewModel: List<PortForwardConfig>
    AppViewModel->>AppViewModel: portForwardConfigs = new list

    PortForwardPage-->>User: UI updates, config card appears
```

---

## 3. Sequence: Quick Port Forward from Sidebar

```mermaid
sequenceDiagram
    actor User
    participant Sidebar
    participant AppViewModel
    participant SidebarPortForwardDialog
    participant JvmProcessManager
    participant PortForwardRepository
    participant kubectl

    User->>Sidebar: right-click a resource
    Sidebar->>AppViewModel: showPortForwardDialog = resource
    AppViewModel-->>User: renders SidebarPortForwardDialog
    Note over SidebarPortForwardDialog: auto-suggests localPort = basePort + 80, remotePort = 80

    User->>SidebarPortForwardDialog: (optionally edit local port) clicks "Start"
    SidebarPortForwardDialog->>SidebarPortForwardDialog: builds PortForwardConfig
    Note over SidebarPortForwardDialog: config = (contextId, namespace, resourceType/kubectlName, resourceName, remotePort=80, localPort, customLocalPort=true, label=resource.name)

    SidebarPortForwardDialog->>AppViewModel: startPortForward(config, resourceName)
    Note over AppViewModel: no config is persisted first — on-the-fly forward

    AppViewModel->>JvmProcessManager: startPortForward(context, namespace, resourceType, resourceName, localPort, remotePort, onOutput, onError)
    JvmProcessManager->>kubectl: ProcessBuilder("kubectl --context <ctx> port-forward <type>/<name> -n <ns> <localPort>:<remotePort>")
    kubectl-->>JvmProcessManager: Process started (PID)
    JvmProcessManager-->>AppViewModel: PortForwardHandle(pid, localPort, remotePort)

    Note over JvmProcessManager: daemon threads read stdout/stderr

    AppViewModel->>PortForwardRepository: createProcess(PortForwardProcess)
    PortForwardRepository->>PortForwardRepository: insert into port_forward_processes

    SidebarPortForwardDialog->>AppViewModel: dismiss dialog
    User-->>SidebarPortForwardDialog: dialog closes
```

---

## 4. Sequence: Start Port Forward (from Saved Config)

```mermaid
sequenceDiagram
    actor User
    participant PortForwardPage
    participant AppViewModel
    participant JvmProcessManager
    participant PortForwardRepository
    participant SQLite
    participant kubectl

    User->>PortForwardPage: clicks "Start" on a config card
    PortForwardPage->>AppViewModel: startPortForward(config, podName)

    AppViewModel->>JvmProcessManager: startPortForward(context, namespace, resourceType, resourceName, localPort, remotePort, onOutput, onError)
    JvmProcessManager->>JvmProcessManager: build command args ["kubectl", "--context", ctx, "port-forward", "type/name", "-n", ns, "localPort:remotePort"]
    JvmProcessManager->>kubectl: ProcessBuilder.start()
    kubectl-->>JvmProcessManager: Process (pid)
    JvmProcessManager->>JvmProcessManager: activeProcesses[pid] = process
    JvmProcessManager->>JvmProcessManager: spawn daemon thread reading stdout → onOutput
    JvmProcessManager->>JvmProcessManager: spawn daemon thread reading stderr → onError
    JvmProcessManager-->>AppViewModel: PortForwardHandle(pid, localPort, remotePort)

    AppViewModel->>AppViewModel: build PortForwardProcess(configId, localPort, remotePort, podName, namespace, pid, isRunning=true, startedAt=now)
    AppViewModel->>PortForwardRepository: createProcess(process)
    PortForwardRepository->>SQLite: INSERT INTO port_forward_processes (...)
    SQLite-->>PortForwardRepository: id

    AppViewModel->>AppViewModel: refreshActiveProcesses()
    AppViewModel->>PortForwardRepository: getActiveProcesses()
    PortForwardRepository->>SQLite: SELECT * FROM port_forward_processes WHERE is_running = true
    SQLite-->>AppViewModel: List<PortForwardProcess>
    AppViewModel->>AppViewModel: activePortForwardProcesses = list

    PortForwardPage-->>User: active process card appears, "Stop" button shown
```

---

## 5. Sequence: Stop Port Forward

```mermaid
sequenceDiagram
    actor User
    participant PortForwardPage
    participant AppViewModel
    participant JvmProcessManager
    participant PortForwardRepository
    participant SQLite
    participant kubectl

    User->>PortForwardPage: clicks "Stop" on an active process card
    PortForwardPage->>AppViewModel: stopPortForward(processId)

    AppViewModel->>AppViewModel: find process by id in activePortForwardProcesses
    AppViewModel->>JvmProcessManager: killProcessByPid(process.pid)

    JvmProcessManager->>JvmProcessManager: activeProcesses.remove(pid)
    alt process still alive
        JvmProcessManager->>kubectl: process.destroy()
        JvmProcessManager->>kubectl: process.waitFor()
        Note over JvmProcessManager: on InterruptedException: destroyForcibly()
    end

    AppViewModel->>PortForwardRepository: updateProcess(process.copy(isRunning = false))
    PortForwardRepository->>SQLite: UPDATE port_forward_processes SET is_running = false WHERE id = ?
    SQLite-->>PortForwardRepository: ok

    AppViewModel->>AppViewModel: refreshActiveProcesses()
    AppViewModel->>PortForwardRepository: getActiveProcesses()
    PortForwardRepository->>SQLite: SELECT ... WHERE is_running = true
    SQLite-->>AppViewModel: updated list

    PortForwardPage-->>User: process card disappears from Active section
```

---

## 6. Sequence: Stop All / Delete Config

```mermaid
sequenceDiagram
    actor User
    participant PortForwardPage
    participant AppViewModel
    participant JvmProcessManager
    participant PortForwardRepository
    participant SQLite
    participant kubectl

    alt Stop All
        User->>PortForwardPage: clicks "Stop All"
        PortForwardPage->>AppViewModel: killAllPortForwards()
        AppViewModel->>AppViewModel: for each activePortForwardProcesses

        loop for each active process
            AppViewModel->>JvmProcessManager: killProcessByPid(pid)
            JvmProcessManager->>kubectl: process.destroy() / destroyForcibly()
            AppViewModel->>PortForwardRepository: updateProcess(copy(isRunning = false))
            PortForwardRepository->>SQLite: UPDATE port_forward_processes SET is_running = false
        end

        AppViewModel->>AppViewModel: activePortForwardProcesses = emptyList()
    else Delete Config
        User->>PortForwardPage: clicks "Del" on a config card
        PortForwardPage->>AppViewModel: deletePortForwardConfig(configId)

        AppViewModel->>AppViewModel: stopPortForwardByConfigId(configId)
        AppViewModel->>PortForwardRepository: getProcessesForConfig(configId)
        PortForwardRepository->>SQLite: SELECT FROM port_forward_processes WHERE config_id = ?
        SQLite-->>AppViewModel: List<PortForwardProcess>

        loop for each process of this config
            alt process.isRunning
                AppViewModel->>JvmProcessManager: killProcessByPid(pid)
                JvmProcessManager->>kubectl: process.destroy()
            end
            AppViewModel->>PortForwardRepository: updateProcess(copy(isRunning = false))
            PortForwardRepository->>SQLite: UPDATE port_forward_processes SET is_running = false
        end

        AppViewModel->>PortForwardRepository: deleteConfig(configId)
        PortForwardRepository->>SQLite: DELETE FROM port_forward_configs WHERE id = ?
        Note over SQLite: FK CASCADE also deletes related process rows

        AppViewModel->>AppViewModel: loadPortForwardConfigs()
        AppViewModel->>AppViewModel: refreshActiveProcesses()
    end

    PortForwardPage-->>User: UI updates
```

---

## Summary of Key Interactions

| Action | Trigger | ViewModel Method | ProcessManager | DB Write | DB Read |
|--------|---------|-----------------|---------------|----------|---------|
| Create config | Dialog Save | `addPortForwardConfig` | — | INSERT config | loadConfigs |
| Update config | Dialog Save (edit) | `updatePortForwardConfig` | — | UPDATE config | loadConfigs |
| Quick forward | Sidebar right-click → Start | `startPortForward` | `startPortForward` (spawn kubectl) | INSERT process | refresh |
| Start forward | Config card "Start" | `startPortForward` | `startPortForward` (spawn kubectl) | INSERT process | refresh |
| Stop forward | Process card "Stop" | `stopPortForward` | `killProcessByPid` | UPDATE process.isRunning=false | refresh |
| Stop all | "Stop All" button | `killAllPortForwards` | `killProcessByPid` × N | UPDATE each | — |
| Delete config | Config card "Del" | `deletePortForwardConfig` | `killProcessByPid` × N | UPDATE process, DELETE config | reload configs |
