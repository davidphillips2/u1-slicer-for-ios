package com.u1.slicer.shared

/**
 * Platform-specific interface for identifying the current platform
 */
interface Platform {
    val name: String
}

/**
 * Expected platform implementation
 */
expect fun getPlatform(): Platform
