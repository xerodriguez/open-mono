package com.fgsoft.klusterui.data

import com.fgsoft.klusterui.model.FavoriteNamespace
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess
import com.fgsoft.klusterui.model.SubContext
import java.sql.Connection
import java.sql.DriverManager

class JvmDatabase(
    private val dbPath: String,
) : Database {
    private var connection: Connection? = null

    override fun connect() {
        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        connection?.createStatement()?.execute("PRAGMA foreign_keys = ON")
        createTables()
    }

    override fun close() {
        connection?.close()
        connection = null
    }

    private fun createTables() {
        val conn = connection ?: throw IllegalStateException("Database not connected")
        conn.createStatement().use { stmt ->
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS contexts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    context TEXT NOT NULL,
                    color INTEGER NOT NULL DEFAULT -14931777,
                    port_forward_base_port INTEGER NOT NULL DEFAULT 8000,
                    is_active INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )

            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS port_forward_configs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    context_id INTEGER NOT NULL,
                    namespace TEXT NOT NULL,
                    resource_type TEXT NOT NULL,
                    resource_name TEXT NOT NULL,
                    remote_port INTEGER NOT NULL,
                    local_port INTEGER NOT NULL,
                    custom_local_port INTEGER NOT NULL DEFAULT 0,
                    label TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY (context_id) REFERENCES contexts(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )

            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS port_forward_processes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    config_id INTEGER NOT NULL,
                    local_port INTEGER NOT NULL,
                    remote_port INTEGER NOT NULL,
                    pod_name TEXT NOT NULL,
                    namespace TEXT NOT NULL,
                    pid INTEGER NOT NULL DEFAULT 0,
                    is_running INTEGER NOT NULL DEFAULT 0,
                    started_at INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (config_id) REFERENCES port_forward_configs(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )

            try {
                stmt.executeUpdate(
                    "ALTER TABLE port_forward_configs ADD COLUMN timeout_minutes INTEGER",
                )
            } catch (_: Exception) {
                // Column already exists, skip
            }

            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS sub_contexts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    context_id INTEGER NOT NULL,
                    regex_pattern TEXT NOT NULL,
                    display_name TEXT NOT NULL,
                    FOREIGN KEY (context_id) REFERENCES contexts(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )

            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS favorite_namespaces (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    context_id INTEGER NOT NULL,
                    namespace TEXT NOT NULL,
                    FOREIGN KEY (context_id) REFERENCES contexts(id) ON DELETE CASCADE,
                    UNIQUE(context_id, namespace)
                )
                """.trimIndent(),
            )
        }
    }

    private fun requireConnection(): Connection = connection ?: throw IllegalStateException("Database not connected")

    override fun getAllContexts(): List<KubeContext> =
        requireConnection()
            .prepareStatement(
                "SELECT id, name, context, color, port_forward_base_port, is_active FROM contexts",
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                KubeContext(
                                    id = rs.getLong("id"),
                                    name = rs.getString("name"),
                                    context = rs.getString("context"),
                                    color = rs.getLong("color"),
                                    portForwardBasePort = rs.getInt("port_forward_base_port"),
                                    isActive = rs.getInt("is_active") != 0,
                                ),
                            )
                        }
                    }
                }
            }

    override fun getContext(id: Long): KubeContext? =
        requireConnection()
            .prepareStatement(
                "SELECT id, name, context, color, port_forward_base_port, is_active FROM contexts WHERE id = ?",
            ).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        KubeContext(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            context = rs.getString("context"),
                            color = rs.getLong("color"),
                            portForwardBasePort = rs.getInt("port_forward_base_port"),
                            isActive = rs.getInt("is_active") != 0,
                        )
                    } else {
                        null
                    }
                }
            }

    override fun insertContext(context: KubeContext): Long =
        requireConnection()
            .prepareStatement(
                "INSERT INTO contexts (name, context, color, port_forward_base_port, is_active) VALUES (?, ?, ?, ?, ?)",
            ).use { stmt ->
                stmt.setString(1, context.name)
                stmt.setString(2, context.context)
                stmt.setLong(3, context.color)
                stmt.setInt(4, context.portForwardBasePort)
                stmt.setInt(5, if (context.isActive) 1 else 0)
                stmt.executeUpdate()
                stmt.generatedKeys.use { keys ->
                    if (keys.next()) keys.getLong(1) else -1
                }
            }

    override fun updateContext(context: KubeContext) {
        requireConnection()
            .prepareStatement(
                "UPDATE contexts SET name = ?, context = ?, color = ?, port_forward_base_port = ?, is_active = ? WHERE id = ?",
            ).use { stmt ->
                stmt.setString(1, context.name)
                stmt.setString(2, context.context)
                stmt.setLong(3, context.color)
                stmt.setInt(4, context.portForwardBasePort)
                stmt.setInt(5, if (context.isActive) 1 else 0)
                stmt.setLong(6, context.id)
                stmt.executeUpdate()
            }
    }

    override fun deleteContext(id: Long) {
        val conn = requireConnection()
        conn.prepareStatement("DELETE FROM sub_contexts WHERE context_id = ?").use { stmt ->
            stmt.setLong(1, id)
            stmt.executeUpdate()
        }
        conn.prepareStatement("DELETE FROM favorite_namespaces WHERE context_id = ?").use { stmt ->
            stmt.setLong(1, id)
            stmt.executeUpdate()
        }
        conn.prepareStatement("DELETE FROM contexts WHERE id = ?").use { stmt ->
            stmt.setLong(1, id)
            stmt.executeUpdate()
        }
    }

    override fun setActiveContext(id: Long) {
        requireConnection()
            .prepareStatement(
                "UPDATE contexts SET is_active = 1 WHERE id = ?",
            ).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeUpdate()
            }
    }

    override fun deactivateContext(id: Long) {
        requireConnection()
            .prepareStatement(
                "UPDATE contexts SET is_active = 0 WHERE id = ?",
            ).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeUpdate()
            }
    }

    override fun getActiveContexts(): List<KubeContext> =
        requireConnection()
            .prepareStatement(
                "SELECT id, name, context, color, port_forward_base_port, is_active FROM contexts WHERE is_active = 1",
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                KubeContext(
                                    id = rs.getLong("id"),
                                    name = rs.getString("name"),
                                    context = rs.getString("context"),
                                    color = rs.getLong("color"),
                                    portForwardBasePort = rs.getInt("port_forward_base_port"),
                                    isActive = rs.getInt("is_active") != 0,
                                ),
                            )
                        }
                    }
                }
            }

    override fun getAllPortForwardConfigs(): List<PortForwardConfig> =
        requireConnection()
            .prepareStatement(
                "SELECT id, context_id, namespace, resource_type, resource_name, remote_port, local_port, custom_local_port, label, timeout_minutes FROM port_forward_configs",
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                PortForwardConfig(
                                    id = rs.getLong("id"),
                                    contextId = rs.getLong("context_id"),
                                    namespace = rs.getString("namespace"),
                                    resourceType = rs.getString("resource_type"),
                                    resourceName = rs.getString("resource_name"),
                                    remotePort = rs.getInt("remote_port"),
                                    localPort = rs.getInt("local_port"),
                                    customLocalPort = rs.getInt("custom_local_port") != 0,
                                    label = rs.getString("label"),
                                    timeoutMinutes = rs.getObject("timeout_minutes") as? Int,
                                ),
                            )
                        }
                    }
                }
            }

    override fun getPortForwardConfigsForContext(contextId: Long): List<PortForwardConfig> =
        requireConnection()
            .prepareStatement(
                "SELECT id, context_id, namespace, resource_type, resource_name, remote_port, local_port, custom_local_port, label, timeout_minutes FROM port_forward_configs WHERE context_id = ?",
            ).use { stmt ->
                stmt.setLong(1, contextId)
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                PortForwardConfig(
                                    id = rs.getLong("id"),
                                    contextId = rs.getLong("context_id"),
                                    namespace = rs.getString("namespace"),
                                    resourceType = rs.getString("resource_type"),
                                    resourceName = rs.getString("resource_name"),
                                    remotePort = rs.getInt("remote_port"),
                                    localPort = rs.getInt("local_port"),
                                    customLocalPort = rs.getInt("custom_local_port") != 0,
                                    label = rs.getString("label"),
                                    timeoutMinutes = rs.getObject("timeout_minutes") as? Int,
                                ),
                            )
                        }
                    }
                }
            }

    override fun insertPortForwardConfig(config: PortForwardConfig): Long =
        requireConnection()
            .prepareStatement(
                "INSERT INTO port_forward_configs (context_id, namespace, resource_type, resource_name, remote_port, local_port, custom_local_port, label, timeout_minutes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ).use { stmt ->
                stmt.setLong(1, config.contextId)
                stmt.setString(2, config.namespace)
                stmt.setString(3, config.resourceType)
                stmt.setString(4, config.resourceName)
                stmt.setInt(5, config.remotePort)
                stmt.setInt(6, config.localPort)
                stmt.setInt(7, if (config.customLocalPort) 1 else 0)
                stmt.setString(8, config.label)
                if (config.timeoutMinutes != null) {
                    stmt.setInt(9, config.timeoutMinutes)
                } else {
                    stmt.setNull(9, java.sql.Types.INTEGER)
                }
                stmt.executeUpdate()
                stmt.generatedKeys.use { keys ->
                    if (keys.next()) keys.getLong(1) else -1
                }
            }

    override fun updatePortForwardConfig(config: PortForwardConfig) {
        requireConnection()
            .prepareStatement(
                "UPDATE port_forward_configs SET context_id = ?, namespace = ?, resource_type = ?, resource_name = ?, remote_port = ?, local_port = ?, custom_local_port = ?, label = ?, timeout_minutes = ? WHERE id = ?",
            ).use { stmt ->
                stmt.setLong(1, config.contextId)
                stmt.setString(2, config.namespace)
                stmt.setString(3, config.resourceType)
                stmt.setString(4, config.resourceName)
                stmt.setInt(5, config.remotePort)
                stmt.setInt(6, config.localPort)
                stmt.setInt(7, if (config.customLocalPort) 1 else 0)
                stmt.setString(8, config.label)
                if (config.timeoutMinutes != null) {
                    stmt.setInt(9, config.timeoutMinutes)
                } else {
                    stmt.setNull(9, java.sql.Types.INTEGER)
                }
                stmt.setLong(10, config.id)
                stmt.executeUpdate()
            }
    }

    override fun deletePortForwardConfig(id: Long) {
        requireConnection().prepareStatement("DELETE FROM port_forward_configs WHERE id = ?").use { stmt ->
            stmt.setLong(1, id)
            stmt.executeUpdate()
        }
    }

    override fun insertPortForwardProcess(process: PortForwardProcess): Long =
        requireConnection()
            .prepareStatement(
                "INSERT INTO port_forward_processes (config_id, local_port, remote_port, pod_name, namespace, pid, is_running, started_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            ).use { stmt ->
                stmt.setLong(1, process.configId)
                stmt.setInt(2, process.localPort)
                stmt.setInt(3, process.remotePort)
                stmt.setString(4, process.podName)
                stmt.setString(5, process.namespace)
                stmt.setLong(6, process.pid)
                stmt.setInt(7, if (process.isRunning) 1 else 0)
                stmt.setLong(8, process.startedAt)
                stmt.executeUpdate()
                stmt.generatedKeys.use { keys ->
                    if (keys.next()) keys.getLong(1) else -1
                }
            }

    override fun updatePortForwardProcess(process: PortForwardProcess) {
        requireConnection()
            .prepareStatement(
                "UPDATE port_forward_processes SET config_id = ?, local_port = ?, remote_port = ?, pod_name = ?, namespace = ?, pid = ?, is_running = ?, started_at = ? WHERE id = ?",
            ).use { stmt ->
                stmt.setLong(1, process.configId)
                stmt.setInt(2, process.localPort)
                stmt.setInt(3, process.remotePort)
                stmt.setString(4, process.podName)
                stmt.setString(5, process.namespace)
                stmt.setLong(6, process.pid)
                stmt.setInt(7, if (process.isRunning) 1 else 0)
                stmt.setLong(8, process.startedAt)
                stmt.setLong(9, process.id)
                stmt.executeUpdate()
            }
    }

    override fun getAllActiveProcesses(): List<PortForwardProcess> =
        requireConnection()
            .prepareStatement(
                "SELECT id, config_id, local_port, remote_port, pod_name, namespace, pid, is_running, started_at FROM port_forward_processes WHERE is_running = 1",
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                PortForwardProcess(
                                    id = rs.getLong("id"),
                                    configId = rs.getLong("config_id"),
                                    localPort = rs.getInt("local_port"),
                                    remotePort = rs.getInt("remote_port"),
                                    podName = rs.getString("pod_name"),
                                    namespace = rs.getString("namespace"),
                                    pid = rs.getLong("pid"),
                                    isRunning = rs.getInt("is_running") != 0,
                                    startedAt = rs.getLong("started_at"),
                                ),
                            )
                        }
                    }
                }
            }

    override fun getProcessesForConfig(configId: Long): List<PortForwardProcess> =
        requireConnection()
            .prepareStatement(
                "SELECT id, config_id, local_port, remote_port, pod_name, namespace, pid, is_running, started_at FROM port_forward_processes WHERE config_id = ?",
            ).use { stmt ->
                stmt.setLong(1, configId)
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                PortForwardProcess(
                                    id = rs.getLong("id"),
                                    configId = rs.getLong("config_id"),
                                    localPort = rs.getInt("local_port"),
                                    remotePort = rs.getInt("remote_port"),
                                    podName = rs.getString("pod_name"),
                                    namespace = rs.getString("namespace"),
                                    pid = rs.getLong("pid"),
                                    isRunning = rs.getInt("is_running") != 0,
                                    startedAt = rs.getLong("started_at"),
                                ),
                            )
                        }
                    }
                }
            }

    override fun deletePortForwardProcess(id: Long) {
        requireConnection().prepareStatement("DELETE FROM port_forward_processes WHERE id = ?").use { stmt ->
            stmt.setLong(1, id)
            stmt.executeUpdate()
        }
    }

    override fun getAllSubContexts(): List<SubContext> =
        requireConnection()
            .prepareStatement(
                "SELECT id, context_id, regex_pattern, display_name FROM sub_contexts",
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                SubContext(
                                    id = rs.getLong("id"),
                                    contextId = rs.getLong("context_id"),
                                    regexPattern = rs.getString("regex_pattern"),
                                    displayName = rs.getString("display_name"),
                                ),
                            )
                        }
                    }
                }
            }

    override fun getSubContexts(contextId: Long): List<SubContext> =
        requireConnection()
            .prepareStatement(
                "SELECT id, context_id, regex_pattern, display_name FROM sub_contexts WHERE context_id = ?",
            ).use { stmt ->
                stmt.setLong(1, contextId)
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                SubContext(
                                    id = rs.getLong("id"),
                                    contextId = rs.getLong("context_id"),
                                    regexPattern = rs.getString("regex_pattern"),
                                    displayName = rs.getString("display_name"),
                                ),
                            )
                        }
                    }
                }
            }

    override fun insertSubContext(subContext: SubContext): Long =
        requireConnection()
            .prepareStatement(
                "INSERT INTO sub_contexts (context_id, regex_pattern, display_name) VALUES (?, ?, ?)",
            ).use { stmt ->
                stmt.setLong(1, subContext.contextId)
                stmt.setString(2, subContext.regexPattern)
                stmt.setString(3, subContext.displayName)
                stmt.executeUpdate()
                stmt.generatedKeys.use { keys ->
                    if (keys.next()) keys.getLong(1) else -1
                }
            }

    override fun updateSubContext(subContext: SubContext) {
        requireConnection()
            .prepareStatement(
                "UPDATE sub_contexts SET context_id = ?, regex_pattern = ?, display_name = ? WHERE id = ?",
            ).use { stmt ->
                stmt.setLong(1, subContext.contextId)
                stmt.setString(2, subContext.regexPattern)
                stmt.setString(3, subContext.displayName)
                stmt.setLong(4, subContext.id)
                stmt.executeUpdate()
            }
    }

    override fun deleteSubContext(id: Long) {
        requireConnection().prepareStatement("DELETE FROM sub_contexts WHERE id = ?").use { stmt ->
            stmt.setLong(1, id)
            stmt.executeUpdate()
        }
    }

    override fun deleteSubContextsForContext(contextId: Long) {
        requireConnection().prepareStatement("DELETE FROM sub_contexts WHERE context_id = ?").use { stmt ->
            stmt.setLong(1, contextId)
            stmt.executeUpdate()
        }
    }

    override fun getAllFavoriteNamespaces(): List<FavoriteNamespace> =
        requireConnection()
            .prepareStatement(
                "SELECT id, context_id, namespace FROM favorite_namespaces",
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                FavoriteNamespace(
                                    id = rs.getLong("id"),
                                    contextId = rs.getLong("context_id"),
                                    namespace = rs.getString("namespace"),
                                ),
                            )
                        }
                    }
                }
            }

    override fun getFavoriteNamespaces(contextId: Long): List<FavoriteNamespace> =
        requireConnection()
            .prepareStatement(
                "SELECT id, context_id, namespace FROM favorite_namespaces WHERE context_id = ?",
            ).use { stmt ->
                stmt.setLong(1, contextId)
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                FavoriteNamespace(
                                    id = rs.getLong("id"),
                                    contextId = rs.getLong("context_id"),
                                    namespace = rs.getString("namespace"),
                                ),
                            )
                        }
                    }
                }
            }

    override fun insertFavoriteNamespace(fav: FavoriteNamespace): Long =
        requireConnection()
            .prepareStatement(
                "INSERT OR IGNORE INTO favorite_namespaces (context_id, namespace) VALUES (?, ?)",
            ).use { stmt ->
                stmt.setLong(1, fav.contextId)
                stmt.setString(2, fav.namespace)
                stmt.executeUpdate()
                stmt.generatedKeys.use { keys ->
                    if (keys.next()) keys.getLong(1) else -1
                }
            }

    override fun deleteFavoriteNamespace(
        contextId: Long,
        namespace: String,
    ) {
        requireConnection()
            .prepareStatement(
                "DELETE FROM favorite_namespaces WHERE context_id = ? AND namespace = ?",
            ).use { stmt ->
                stmt.setLong(1, contextId)
                stmt.setString(2, namespace)
                stmt.executeUpdate()
            }
    }

    override fun isFavoriteNamespace(
        contextId: Long,
        namespace: String,
    ): Boolean =
        requireConnection()
            .prepareStatement(
                "SELECT COUNT(*) FROM favorite_namespaces WHERE context_id = ? AND namespace = ?",
            ).use { stmt ->
                stmt.setLong(1, contextId)
                stmt.setString(2, namespace)
                stmt.executeQuery().use { rs ->
                    rs.next() && rs.getInt(1) > 0
                }
            }
}
