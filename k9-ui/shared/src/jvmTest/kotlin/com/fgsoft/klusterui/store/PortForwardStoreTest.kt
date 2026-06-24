package com.fgsoft.klusterui.store

import com.fgsoft.klusterui.fakes.FakeDatabase
import com.fgsoft.klusterui.fakes.FakeProcessManager
import com.fgsoft.klusterui.fakes.fakeDeps
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess
import com.fgsoft.klusterui.ui.store.PortForwardStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PortForwardStoreTest {
    private lateinit var db: FakeDatabase
    private lateinit var processManager: FakeProcessManager
    private lateinit var store: PortForwardStore

    @BeforeTest
    fun setUp() {
        db = FakeDatabase()
        processManager = FakeProcessManager()
        val deps = fakeDeps(db).copy(processManager = processManager)
        store = PortForwardStore(deps, CoroutineScope(UnconfinedTestDispatcher()))
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun `loadConfigs populates configs for context`() {
        val ctxId = 1L
        db.insertPortForwardConfig(
            PortForwardConfig(
                contextId = ctxId,
                namespace = "default",
                resourceType = "pods",
                resourceName = "my-pod",
                remotePort = 8080,
                localPort = 16080,
            ),
        )

        store.loadConfigs(ctxId)

        assertEquals(1, store.configs.size)
        assertEquals("my-pod", store.configs[0].resourceName)
    }

    @Test
    fun `loadConfigs filters by contextId`() {
        db.insertPortForwardConfig(
            PortForwardConfig(
                contextId = 1,
                namespace = "ns1",
                resourceType = "pods",
                resourceName = "pod-a",
                remotePort = 80,
                localPort = 8080,
            ),
        )
        db.insertPortForwardConfig(
            PortForwardConfig(
                contextId = 2,
                namespace = "ns2",
                resourceType = "services",
                resourceName = "svc-b",
                remotePort = 443,
                localPort = 8443,
            ),
        )

        store.loadConfigs(1)

        assertEquals(1, store.configs.size)
        assertEquals("pod-a", store.configs[0].resourceName)
    }

    @Test
    fun `addConfig creates record and refreshes list`() =
        runTest {
            store.loadConfigs(1)

            val id =
                store.addConfig(
                    PortForwardConfig(
                        contextId = 1,
                        namespace = "default",
                        resourceType = "pods",
                        resourceName = "new-pod",
                        remotePort = 3000,
                        localPort = 13000,
                    ),
                )

            assertTrue(id > 0)
            assertEquals(1, store.configs.size)
            assertEquals("new-pod", store.configs[0].resourceName)
        }

    @Test
    fun `updateConfig refreshes list`() =
        runTest {
            val id =
                db.insertPortForwardConfig(
                    PortForwardConfig(
                        contextId = 1,
                        namespace = "default",
                        resourceType = "pods",
                        resourceName = "old-name",
                        remotePort = 80,
                        localPort = 8080,
                    ),
                )
            store.loadConfigs(1)

            store.updateConfig(
                PortForwardConfig(
                    id = id,
                    contextId = 1,
                    namespace = "default",
                    resourceType = "pods",
                    resourceName = "new-name",
                    remotePort = 9090,
                    localPort = 19090,
                ),
            )

            assertEquals(1, store.configs.size)
            assertEquals("new-name", store.configs[0].resourceName)
            assertEquals(9090, store.configs[0].remotePort)
        }

    @Test
    fun `deleteConfig stops processes and removes config`() =
        runTest {
            val configId =
                db.insertPortForwardConfig(
                    PortForwardConfig(
                        contextId = 1,
                        namespace = "ns",
                        resourceType = "pods",
                        resourceName = "pod",
                        remotePort = 80,
                        localPort = 8080,
                    ),
                )
            db.insertPortForwardProcess(
                com.fgsoft.klusterui.model.PortForwardProcess(
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
            store.loadConfigs(1)
            store.refreshProcesses()
            assertEquals(1, store.activeProcesses.size)

            store.deleteConfig(configId)

            assertEquals(1, processManager.killedPids.size)
            assertEquals(100, processManager.killedPids[0])
            assertEquals(0, store.activeProcesses.size)
        }

    @Test
    fun `stop kills process by pid and marks stopped`() =
        runTest {
            val configId =
                db.insertPortForwardConfig(
                    PortForwardConfig(
                        contextId = 1,
                        namespace = "ns",
                        resourceType = "pods",
                        resourceName = "pod",
                        remotePort = 80,
                        localPort = 8080,
                    ),
                )
            val procId =
                db.insertPortForwardProcess(
                    com.fgsoft.klusterui.model.PortForwardProcess(
                        configId = configId,
                        localPort = 8080,
                        remotePort = 80,
                        podName = "pod-xyz",
                        namespace = "ns",
                        pid = 200,
                        isRunning = true,
                        startedAt = 1000,
                    ),
                )
            store.refreshProcesses()
            assertEquals(1, store.activeProcesses.size)
            assertTrue(store.activeProcesses[0].isRunning)

            store.stop(procId)

            assertEquals(1, processManager.killedPids.size)
            assertEquals(200, processManager.killedPids[0])
            assertEquals(0, store.activeProcesses.size)
        }

    @Test
    fun `stop ignores unknown processId`() =
        runTest {
            store.refreshProcesses()
            store.stop(999)
            assertEquals(0, processManager.killedPids.size)
        }

    @Test
    fun `killAll kills all active processes`() =
        runTest {
            val configId =
                db.insertPortForwardConfig(
                    PortForwardConfig(
                        contextId = 1,
                        namespace = "ns",
                        resourceType = "pods",
                        resourceName = "p1",
                        remotePort = 80,
                        localPort = 8080,
                    ),
                )
            db.insertPortForwardProcess(
                com.fgsoft.klusterui.model.PortForwardProcess(
                    configId = configId,
                    localPort = 8080,
                    remotePort = 80,
                    podName = "p1",
                    namespace = "ns",
                    pid = 300,
                    isRunning = true,
                    startedAt = 1000,
                ),
            )
            db.insertPortForwardProcess(
                com.fgsoft.klusterui.model.PortForwardProcess(
                    configId = configId,
                    localPort = 8081,
                    remotePort = 81,
                    podName = "p2",
                    namespace = "ns",
                    pid = 301,
                    isRunning = true,
                    startedAt = 2000,
                ),
            )
            store.refreshProcesses()
            assertEquals(2, store.activeProcesses.size)

            store.killAll()

            assertEquals(2, processManager.killedPids.size)
            assertTrue(300 in processManager.killedPids)
            assertTrue(301 in processManager.killedPids)
            assertEquals(0, store.activeProcesses.size)
        }

    @Test
    fun `refreshProcesses reloads from repository`() =
        runTest {
            val configId =
                db.insertPortForwardConfig(
                    PortForwardConfig(
                        contextId = 1,
                        namespace = "ns",
                        resourceType = "pods",
                        resourceName = "pod",
                        remotePort = 80,
                        localPort = 8080,
                    ),
                )
            val procId =
                db.insertPortForwardProcess(
                    com.fgsoft.klusterui.model.PortForwardProcess(
                        configId = configId,
                        localPort = 8080,
                        remotePort = 80,
                        podName = "pod",
                        namespace = "ns",
                        pid = 400,
                        isRunning = true,
                        startedAt = 1000,
                    ),
                )

            store.refreshProcesses()

            assertEquals(1, store.activeProcesses.size)
            assertEquals(procId, store.activeProcesses[0].id)
        }

    @Test
    fun `start calls processManager and creates process record`() =
        runTest {
            val configId =
                db.insertPortForwardConfig(
                    PortForwardConfig(
                        contextId = 1,
                        namespace = "ns",
                        resourceType = "pods",
                        resourceName = "my-pod",
                        remotePort = 8080,
                        localPort = 16080,
                    ),
                )
            store.loadConfigs(1)
            val config = store.configs[0]

            store.start(config, "pods/my-pod", "test-context")

            assertEquals(1, processManager.startedForwards.size)
            val call = processManager.startedForwards[0]
            assertEquals("ns", call.namespace)
            assertEquals("pods", call.resourceType)
            assertEquals("my-pod", call.resourceName)
            assertEquals(16080, call.localPort)
            assertEquals(8080, call.remotePort)

            store.refreshProcesses()
            assertEquals(1, store.activeProcesses.size)
            assertEquals(configId, store.activeProcesses[0].configId)
            assertTrue(store.activeProcesses[0].isRunning)
        }
}
