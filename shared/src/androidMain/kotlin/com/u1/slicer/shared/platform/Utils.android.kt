package com.u1.slicer.shared.platform

actual object PlatformUtils {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual fun formatFloat(format: String, vararg args: Any): String {
        return format.format(*args)
    }
}
