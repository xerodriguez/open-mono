package com.fgsoft.klusterui.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortForwardConfigTest {
    @Test
    fun `default values`() {
        val config =
            PortForwardConfig(
                contextId = 1,
                namespace = "default",
                resourceType = "pods",
                resourceName = "my-pod",
                remotePort = 8080,
                localPort = 16080,
            )
        assertEquals(0L, config.id)
        assertEquals(false, config.customLocalPort)
        assertEquals("", config.label)
        assertNull(config.timeoutMinutes)
    }

    @Test
    fun `custom local port is preserved`() {
        val config =
            PortForwardConfig(
                contextId = 1,
                namespace = "default",
                resourceType = "services",
                resourceName = "my-svc",
                remotePort = 80,
                localPort = 9090,
                customLocalPort = true,
            )
        assertTrue(config.customLocalPort)
    }

    @Test
    fun `timeoutMinutes null by default`() {
        val config =
            PortForwardConfig(
                id = 5,
                contextId = 2,
                namespace = "ns",
                resourceType = "deployments",
                resourceName = "app",
                remotePort = 3000,
                localPort = 13000,
            )
        assertNull(config.timeoutMinutes)
    }

    @Test
    fun `timeoutMinutes set explicitly`() {
        val config =
            PortForwardConfig(
                id = 5,
                contextId = 2,
                namespace = "ns",
                resourceType = "deployments",
                resourceName = "app",
                remotePort = 3000,
                localPort = 13000,
                timeoutMinutes = 10,
            )
        assertEquals(10, config.timeoutMinutes)
    }

    @Test
    fun `label is preserved`() {
        val config =
            PortForwardConfig(
                contextId = 1,
                namespace = "default",
                resourceType = "pods",
                resourceName = "my-pod",
                remotePort = 8080,
                localPort = 16080,
                label = "API Gateway",
            )
        assertEquals("API Gateway", config.label)
    }

    @Test
    fun `equality on equal content`() {
        val a =
            PortForwardConfig(
                id = 5,
                contextId = 1,
                namespace = "ns",
                resourceType = "pods",
                resourceName = "pod",
                remotePort = 80,
                localPort = 8080,
            )
        val b = a.copy()
        assertEquals(a, b)
    }
}
