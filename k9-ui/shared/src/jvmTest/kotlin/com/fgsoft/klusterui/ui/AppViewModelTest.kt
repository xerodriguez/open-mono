package com.fgsoft.klusterui.ui

import com.fgsoft.klusterui.fakes.FakeDatabase
import com.fgsoft.klusterui.fakes.FakeProcessManager
import com.fgsoft.klusterui.fakes.fakeDeps
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.KubeResource
import com.fgsoft.klusterui.model.PortForwardConfig
import com.fgsoft.klusterui.model.PortForwardProcess
import com.fgsoft.klusterui.model.ResourceType
import com.fgsoft.klusterui.model.currentTimeMillis
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppViewModelTest {
    private lateinit var db: FakeDatabase
    private lateinit var vm: AppViewModel

    @BeforeTest
    fun setUp() {
        db = FakeDatabase()
        vm = AppViewModel(fakeDeps(db))
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    // ── findAvailableLocalPort ──

    @Test
    fun `findAvailableLocalPort returns desiredPort when free`() {
        val port = vm.findAvailableLocalPort(1, 8080)

        assertEquals(8080, port)
    }

    @Test
    fun `findAvailableLocalPort returns next port when desired is taken`() {
        vm.portForward.configs.let { /* inject via db */ }
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
        vm.portForward.loadConfigs(1)

        val port = vm.findAvailableLocalPort(1, 8080)

        assertEquals(8081, port)
    }

    @Test
    fun `findAvailableLocalPort skips multiple occupied ports`() {
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
        db.insertPortForwardConfig(
            PortForwardConfig(
                contextId = 1,
                namespace = "ns",
                resourceType = "services",
                resourceName = "s1",
                remotePort = 81,
                localPort = 8081,
            ),
        )
        vm.portForward.loadConfigs(1)

        val port = vm.findAvailableLocalPort(1, 8080)

        assertEquals(8082, port)
    }

    @Test
    fun `findAvailableLocalPort considers active processes too`() {
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
            PortForwardProcess(
                configId = configId,
                localPort = 8080,
                remotePort = 80,
                podName = "p1-xyz",
                namespace = "ns",
                pid = 100,
                isRunning = true,
                startedAt = 1000,
            ),
        )
        vm.portForward.refreshProcesses()

        val port = vm.findAvailableLocalPort(1, 8080)

        assertEquals(8081, port)
    }

    @Test
    fun `findAvailableLocalPort scans all loaded configs and active processes`() {
        db.insertPortForwardConfig(
            PortForwardConfig(
                contextId = 1,
                namespace = "ns1",
                resourceType = "pods",
                resourceName = "p1",
                remotePort = 80,
                localPort = 9090,
            ),
        )
        db.insertPortForwardConfig(
            PortForwardConfig(
                contextId = 2,
                namespace = "ns2",
                resourceType = "pods",
                resourceName = "p2",
                remotePort = 80,
                localPort = 9091,
            ),
        )
        vm.portForward.loadConfigs(1)

        // Only context 1's configs are loaded (port 9090)
        val port = vm.findAvailableLocalPort(1, 9090)
        // 9090 is taken by p1, next free is 9091
        assertEquals(9091, port)
    }

    // ── checkPortForwardTimeouts ──

    @Test
    fun `checkPortForwardTimeouts stops expired process`() {
        val configId =
            db.insertPortForwardConfig(
                PortForwardConfig(
                    contextId = 1,
                    namespace = "ns",
                    resourceType = "pods",
                    resourceName = "pod",
                    remotePort = 80,
                    localPort = 8080,
                    timeoutMinutes = 5,
                ),
            )
        val now = currentTimeMillis()
        db.insertPortForwardProcess(
            PortForwardProcess(
                configId = configId,
                localPort = 8080,
                remotePort = 80,
                podName = "pod-xyz",
                namespace = "ns",
                pid = 100,
                isRunning = true,
                startedAt = now - (6 * 60 * 1000), // 6 minutes ago
            ),
        )
        vm.portForward.refreshProcesses()
        vm.portForward.loadConfigs(1)
        assertEquals(1, vm.portForward.activeProcesses.size)

        vm.checkPortForwardTimeouts()

        assertEquals(0, vm.portForward.activeProcesses.size)
    }

    @Test
    fun `checkPortForwardTimeouts ignores process within timeout window`() {
        val configId =
            db.insertPortForwardConfig(
                PortForwardConfig(
                    contextId = 1,
                    namespace = "ns",
                    resourceType = "pods",
                    resourceName = "pod",
                    remotePort = 80,
                    localPort = 8080,
                    timeoutMinutes = 10,
                ),
            )
        val now = currentTimeMillis()
        db.insertPortForwardProcess(
            PortForwardProcess(
                configId = configId,
                localPort = 8080,
                remotePort = 80,
                podName = "pod-xyz",
                namespace = "ns",
                pid = 100,
                isRunning = true,
                startedAt = now - (3 * 60 * 1000), // 3 minutes ago
            ),
        )
        vm.portForward.refreshProcesses()
        vm.portForward.loadConfigs(1)
        assertEquals(1, vm.portForward.activeProcesses.size)

        vm.checkPortForwardTimeouts()

        assertEquals(1, vm.portForward.activeProcesses.size)
    }

    @Test
    fun `checkPortForwardTimeouts ignores process with null timeoutMinutes`() {
        val configId =
            db.insertPortForwardConfig(
                PortForwardConfig(
                    contextId = 1,
                    namespace = "ns",
                    resourceType = "pods",
                    resourceName = "pod",
                    remotePort = 80,
                    localPort = 8080,
                    timeoutMinutes = null,
                ),
            )
        val now = currentTimeMillis()
        db.insertPortForwardProcess(
            PortForwardProcess(
                configId = configId,
                localPort = 8080,
                remotePort = 80,
                podName = "pod-xyz",
                namespace = "ns",
                pid = 100,
                isRunning = true,
                startedAt = now - (60 * 60 * 1000), // 1 hour ago
            ),
        )
        vm.portForward.refreshProcesses()
        vm.portForward.loadConfigs(1)
        assertEquals(1, vm.portForward.activeProcesses.size)

        vm.checkPortForwardTimeouts()

        assertEquals(1, vm.portForward.activeProcesses.size)
    }

    @Test
    fun `checkPortForwardTimeouts ignores process with no config`() {
        db.insertPortForwardProcess(
            PortForwardProcess(
                configId = 999, // no config exists
                localPort = 8080,
                remotePort = 80,
                podName = "orphan",
                namespace = "ns",
                pid = 100,
                isRunning = true,
                startedAt = 0,
            ),
        )
        vm.portForward.refreshProcesses()
        assertEquals(1, vm.portForward.activeProcesses.size)

        vm.checkPortForwardTimeouts()

        assertEquals(1, vm.portForward.activeProcesses.size)
    }

    // ── Orchestration ──

    @Test
    fun `deletePortForwardConfig removes config and reloads from active context`() {
        db.insertContext(KubeContext(id = 1, name = "ctx", context = "k8s-ctx", isActive = true))
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
        vm.loadContexts()
        assertEquals(1, vm.portForward.configs.size)

        vm.deletePortForwardConfig(configId)

        assertEquals(0, vm.portForward.configs.size)
    }
}
