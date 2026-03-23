package com.u1.slicer.shared

/**
 * Android platform implementation
 */
class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

/**
 * Actual platform function for Android
 */
actual fun getPlatform(): Platform = AndroidPlatform()
