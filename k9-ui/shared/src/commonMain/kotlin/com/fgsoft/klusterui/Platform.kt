package com.fgsoft.klusterui

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform