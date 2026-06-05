package com.fgsoft.klusterui.model

import kotlinx.serialization.Serializable

@Serializable
data class KubeContext(
    val id: Long = 0,
    val name: String,
    val context: String,
    val color: Long = 0xFF1976D2,
    val portForwardBasePort: Int = 8000,
    val isActive: Boolean = false,
)
