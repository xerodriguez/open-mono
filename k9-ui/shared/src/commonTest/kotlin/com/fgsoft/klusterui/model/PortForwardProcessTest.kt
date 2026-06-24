package com.fgsoft.klusterui.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortForwardProcessTest {
    @Test
    fun `default values`() {
        val process =
            PortForwardProcess(
                configId = 1,
                localPort = 16080,
                remotePort = 8080,
                podName = "my-pod-xyz",
                namespace = "default",
            )
        assertEquals(0L, process.id)
        assertEquals(0L, process.pid)
        assertFalse(process.isRunning)
        assertEquals(0L, process.startedAt)
    }

    @Test
    fun `running process has correct fields`() {
        val now = currentTimeMillis()
        val process =
            PortForwardProcess(
                configId = 2,
                localPort = 9090,
                remotePort = 80,
                podName = "svc-pod",
                namespace = "prod",
                pid = 12345,
                isRunning = true,
                startedAt = now,
            )
        assertTrue(process.isRunning)
        assertEquals(12345L, process.pid)
        assertEquals(now, process.startedAt)
    }

    @Test
    fun `stopped process is marked not running`() {
        val process =
            PortForwardProcess(
                id = 5,
                configId = 1,
                localPort = 8080,
                remotePort = 80,
                podName = "pod",
                namespace = "ns",
                pid = 999,
                isRunning = true,
                startedAt = 1000L,
            ).copy(isRunning = false)

        assertFalse(process.isRunning)
    }

    @Test
    fun `equality ignores id field`() {
        val a =
            PortForwardProcess(
                id = 1,
                configId = 5,
                localPort = 80,
                remotePort = 80,
                podName = "p",
                namespace = "n",
            )
        val b = a.copy()
        assertEquals(a, b)
    }
}
