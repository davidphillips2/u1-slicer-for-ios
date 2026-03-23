package com.u1.slicer.shared

import platform.UIKit.UIDevice

/**
 * iOS platform implementation
 */
class IOSPlatform : Platform {
    override val name: String = "iOS ${UIDevice.currentDevice.systemVersion}"
}

/**
 * Actual platform function for iOS
 */
actual fun getPlatform(): Platform = IOSPlatform()
