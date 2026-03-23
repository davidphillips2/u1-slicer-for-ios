package com.u1.slicer.shared.platform

/**
 * Platform-specific utilities for KMP
 */
expect object PlatformUtils {
    fun currentTimeMillis(): Long
    fun formatFloat(format: String, vararg args: Any): String
}
