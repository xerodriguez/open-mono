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

@Serializable
data class SubContext(
    val id: Long = 0,
    val contextId: Long = 0,
    val regexPattern: String,
    val displayName: String,
)

@Serializable
data class FavoriteNamespace(
    val id: Long = 0,
    val contextId: Long,
    val namespace: String,
)
