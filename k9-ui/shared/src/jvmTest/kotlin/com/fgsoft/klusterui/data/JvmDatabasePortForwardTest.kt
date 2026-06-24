package com.fgsoft.klusterui.data
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JvmDatabasePortForwardTest {
    private lateinit var db: JvmDatabase
    private lateinit var tempFile: File
    private var ctxId: Long = 0

    @BeforeTest
    fun setUp() {
        tempFile = File.createTempFile("klusterui_test_", ".db")
        db = JvmDatabase(tempFile.absolutePath)
        db.connect()
        ctxId = db.insertContext(KubeContext(name = "test", context = "k8s-test"))
    }

    @AfterTest
    fun tearDown() {
        db.close()
        tempFile.delete()
    }

    @Test
    fun `insert and select config preserves all fields including timeoutMinutes`() {
        val id =
            db.insertPortForwardConfig(
                PortForwardConfig(
                    contextId = ctxId,
                    namespace = "default",
                    resourceType = "pods",
                    resourceName = "my-pod",
                    remotePort = 8080,
                    localPort = 16080,
                    customLocalPort = true,
                    label = "API Gateway",
                    timeoutMinutes = 10,
                ),
            )

        val configs = db.getAllPortForwardConfigs()

        assertEquals(1, configs.size)
        val config = configs[0]
        assertEquals(id, config.id)
        assertEquals(ctxId, config.contextId)
        assertEquals("default", config.namespace)
        assertEquals("pods", config.resourceType)
        assertEquals("my-pod", config.resourceName)
        assertEquals(8080, config.remotePort)
        assertEquals(16080, config.localPort)
        assertTrue(config.customLocalPort)
        assertEquals("API Gateway", config.label)
        assertEquals(10, config.timeoutMinutes)
    }

    @Test
    fun `timeoutMinutes null is stored and retrieved correctly`() {
        db.insertPortForwardConfig(
            PortForwardConfig(
                contextId = ctxId,
                namespace = "ns",
                resourceType = "services",
                resourceName = "svc",
                remotePort = 80,
                localPort = 8080,
                timeoutMinutes = null,
            ),
        )

        val configs = db.getAllPortForwardConfigs()

        assertEquals(1, configs.size)
        assertNull(configs[0].timeoutMinutes)
    }

    @Test
    fun `update config modifies all fields`() {
        val id =
            db.insertPortForwardConfig(
                PortForwardConfig(
                    contextId = ctxId,
                    namespace = "default",
                    resourceType = "pods",
                    resourceName = "old",
                    remotePort = 80,
                    localPort = 8080,
                    timeoutMinutes = 5,
                ),
            )

        db.updatePortForwardConfig(
            PortForwardConfig(
                id = id,
                contextId = ctxId,
                namespace = "prod",
                resourceType = "services",
                resourceName = "new",
                remotePort = 443,
                localPort = 8443,
                customLocalPort = true,
                label = "Updated",
                timeoutMinutes = 30,
            ),
        )

        val configs = db.getAllPortForwardConfigs()
        assertEquals(1, configs.size)
        val config = configs[0]
        assertEquals("new", config.resourceName)
        assertEquals("prod", config.namespace)
        assertEquals(443, config.remotePort)
        assertEquals(8443, config.localPort)
        assertTrue(config.customLocalPort)
        assertEquals("Updated", config.label)
        assertEquals(30, config.timeoutMinutes)
    }

    @Test
    fun `delete config cascades to processes`() {
        val configId =
            db.insertPortForwardConfig(
                PortForwardConfig(
                    contextId = ctxId,
                    namespace = "ns",
                    resourceType = "pods",
                    resourceName = "pod",
                    remotePort = 80,
                    localPort = 8080,
                ),
            )
        db.insertPortForwardProcess(
            PortForwardProcess(
                configId = configId,
                localPort = 8080,
                remotePort = 80,
                podName = "pod-xyz",
                namespace = "ns",
                pid = 100,
                isRunning = true,
                startedAt = 1000,
            ),
        )

        db.deletePortForwardConfig(configId)

        assertEquals(0, db.getAllPortForwardConfigs().size)
        assertEquals(0, db.getAllActiveProcesses().size)
    }

    @Test
    fun `insert process and select active returns correct data`() {
        val configId =
            db.insertPortForwardConfig(
                PortForwardConfig(
                    contextId = ctxId,
                    namespace = "ns",
                    resourceType = "pods",
                    resourceName = "pod",
                    remotePort = 80,
                    localPort = 8080,
                ),
            )
        val procId =
            db.insertPortForwardProcess(
                PortForwardProcess(
                    configId = configId,
                    localPort = 8080,
                    remotePort = 80,
                    podName = "pod-xyz",
                    namespace = "ns",
                    pid = 12345,
                    isRunning = true,
                    startedAt = 999999,
                ),
            )

        val active = db.getAllActiveProcesses()

        assertEquals(1, active.size)
        assertEquals(procId, active[0].id)
        assertEquals(configId, active[0].configId)
        assertEquals(12345, active[0].pid)
        assertEquals("pod-xyz", active[0].podName)
        assertEquals(999999, active[0].startedAt)
        assertTrue(active[0].isRunning)
    }

    @Test
    fun `update process isRunning false excludes from active`() {
        val configId =
            db.insertPortForwardConfig(
                PortForwardConfig(
                    contextId = ctxId,
                    namespace = "ns",
                    resourceType = "pods",
                    resourceName = "pod",
                    remotePort = 80,
                    localPort = 8080,
                ),
            )
        val procId =
            db.insertPortForwardProcess(
                PortForwardProcess(
                    configId = configId,
                    localPort = 8080,
                    remotePort = 80,
                    podName = "pod-xyz",
                    namespace = "ns",
                    pid = 100,
                    isRunning = true,
                    startedAt = 1000,
                ),
            )

        db.updatePortForwardProcess(
            PortForwardProcess(
                id = procId,
                configId = configId,
                localPort = 8080,
                remotePort = 80,
                podName = "pod-xyz",
                namespace = "ns",
                pid = 100,
                isRunning = false,
                startedAt = 1000,
            ),
        )

        assertEquals(0, db.getAllActiveProcesses().size)
    }

    @Test
    fun `getProcessesForConfig returns only matching config`() {
        val c1 =
            db.insertPortForwardConfig(
                PortForwardConfig(
                    contextId = ctxId,
                    namespace = "ns",
                    resourceType = "pods",
                    resourceName = "p1",
                    remotePort = 80,
                    localPort = 8080,
                ),
            )
        val c2 =
            db.insertPortForwardConfig(
                PortForwardConfig(
                    contextId = ctxId,
                    namespace = "ns",
                    resourceType = "pods",
                    resourceName = "p2",
                    remotePort = 81,
                    localPort = 8081,
                ),
            )
        db.insertPortForwardProcess(
            PortForwardProcess(
                configId = c1,
                localPort = 8080,
                remotePort = 80,
                podName = "p1",
                namespace = "ns",
                pid = 100,
                isRunning = true,
                startedAt = 1000,
            ),
        )
        db.insertPortForwardProcess(
            PortForwardProcess(
                configId = c2,
                localPort = 8081,
                remotePort = 81,
                podName = "p2",
                namespace = "ns",
                pid = 101,
                isRunning = true,
                startedAt = 2000,
            ),
        )

        val processes = db.getProcessesForConfig(c1)

        assertEquals(1, processes.size)
        assertEquals("p1", processes[0].podName)
    }

    @Test
    fun `getPortForwardConfigsForContext filters correctly`() {
        val ctx2 = db.insertContext(KubeContext(name = "test2", context = "k8s-test2"))
        db.insertPortForwardConfig(
            PortForwardConfig(
                contextId = ctxId,
                namespace = "ns1",
                resourceType = "pods",
                resourceName = "pod-a",
                remotePort = 80,
                localPort = 8080,
            ),
        )
        db.insertPortForwardConfig(
            PortForwardConfig(
                contextId = ctx2,
                namespace = "ns2",
                resourceType = "pods",
                resourceName = "pod-b",
                remotePort = 81,
                localPort = 8081,
            ),
        )

        assertEquals(1, db.getPortForwardConfigsForContext(ctxId).size)
        assertEquals(1, db.getPortForwardConfigsForContext(ctx2).size)
        assertEquals("pod-a", db.getPortForwardConfigsForContext(ctxId)[0].resourceName)
    }
}
