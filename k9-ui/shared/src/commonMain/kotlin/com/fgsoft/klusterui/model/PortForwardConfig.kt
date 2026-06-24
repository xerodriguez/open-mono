package com.fgsoft.klusterui.model

import kotlinx.serialization.Serializable

@Serializable
data class PortForwardConfig(
    val id: Long = 0,
    val contextId: Long,
    val namespace: String,
    val resourceType: String,
    val resourceName: String,
    val remotePort: Int,
    val localPort: Int,
    val customLocalPort: Boolean = false,
    val label: String = "",
    val timeoutMinutes: Int? = null,
)

@Serializable
data class PortForwardProcess(
    val id: Long = 0,
    val configId: Long,
    val localPort: Int,
    val remotePort: Int,
    val podName: String,
    val namespace: String,
    val pid: Long = 0,
    val isRunning: Boolean = false,
    val startedAt: Long = 0,
)
